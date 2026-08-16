package com.stride.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.stride.app.audio.MusicController
import com.stride.app.location.LocationTracker
import com.stride.app.location.TrackPoint
import com.stride.app.navigation.Destination
import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.common.WeatherSnapshot
import com.stride.core.common.haversineDistanceMeters
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.StepType
import com.stride.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ActiveRunUiState(
    val isLoading: Boolean = true,
    val steps: List<SessionStepEntity> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val finishedRunId: Long? = null,
    val isOutdoor: Boolean = true,
    val trackPoints: List<TrackPoint> = emptyList(),
    val gpsDistanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val weather: WeatherUiState = WeatherUiState(),
) {
    val currentStep: SessionStepEntity? get() = steps.getOrNull(currentIndex)
    val nextStep: SessionStepEntity? get() = steps.getOrNull(currentIndex + 1)
}

/**
 * Drives a session off its planned steps (time-based, per docs/foundation.md "Session pacing").
 * For OUTDOOR sessions this now also tracks real GPS points — live distance, a plottable path,
 * and elevation gain (see [LocationTracker]'s accuracy caveat) — and fetches weather once, all
 * genuinely working, not mocked. Still not the full Phase 4/5 picture: no Media3
 * MediaSessionService (voice cues use the interim [VoiceCueSpeaker]), no MapLibre basemap under
 * the track (Map tab draws the raw path only), no background execution once the app is closed.
 */
@HiltViewModel
class ActiveRunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val runRepository: RunRepository,
    private val voiceCue: VoiceCueSpeaker,
    private val locationTracker: LocationTracker,
    private val weatherRepository: WeatherRepository,
    val musicController: MusicController,
) : ViewModel() {

    private val route: Destination.ActiveRun = savedStateHandle.toRoute()
    private val planSessionId: Long? = route.planSessionId
    private val environment: RunEnvironment = if (route.outdoor) RunEnvironment.OUTDOOR else RunEnvironment.TREADMILL

    private val _state = MutableStateFlow(ActiveRunUiState(isOutdoor = route.outdoor))
    val state: StateFlow<ActiveRunUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var locationJob: Job? = null
    private val startedAt: Instant = Instant.now()

    fun start() {
        viewModelScope.launch {
            val steps = planSessionId?.let { planRepository.observeStepsForSession(it).first() }
                ?.takeIf { it.isNotEmpty() }
                ?: demoSteps() // dev-preview entry (no real session) — still worth being able to try
            _state.value = _state.value.copy(
                isLoading = false,
                steps = steps,
                currentIndex = 0,
                remainingSeconds = steps.first().durationSeconds ?: 0,
            )
            voiceCue.speak(cueFor(steps.first()))
            resumeTicking()
        }
        if (environment == RunEnvironment.OUTDOOR) {
            startLocationTracking()
            fetchWeather()
        }
    }

    private fun startLocationTracking() {
        locationJob = locationTracker.observeLocationUpdates()
            .onEach { point -> accumulateTrackPoint(point) }
            .launchIn(viewModelScope)
    }

    private fun accumulateTrackPoint(point: TrackPoint) {
        val current = _state.value
        val previous = current.trackPoints.lastOrNull()
        var addedDistance = 0.0
        var addedGain = 0.0
        if (previous != null) {
            addedDistance = haversineDistanceMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
            val altDelta = point.altitudeMeters - previous.altitudeMeters
            if (altDelta > 0) addedGain = altDelta
        }
        _state.value = current.copy(
            trackPoints = current.trackPoints + point,
            gpsDistanceMeters = current.gpsDistanceMeters + addedDistance,
            elevationGainMeters = current.elevationGainMeters + addedGain,
        )
    }

    private fun fetchWeather() {
        _state.value = _state.value.copy(weather = WeatherUiState(isLoading = true))
        viewModelScope.launch {
            val location = locationTracker.lastKnownLocation()
            val snapshot: WeatherSnapshot? = location?.let {
                weatherRepository.fetchCurrentConditions(it.latitude, it.longitude)
            }
            _state.value = _state.value.copy(
                weather = if (snapshot != null) WeatherUiState(snapshot = snapshot) else WeatherUiState(unavailable = true),
            )
        }
    }

    /** "Walk for 4 minutes" / "Run for 1 minute 30 seconds" — spoken at the start of every step. */
    private fun cueFor(step: SessionStepEntity): String {
        val action = when (step.stepType) {
            StepType.WALK -> "Walk"
            StepType.RUN -> "Run"
            StepType.REST -> "Rest"
        }
        val seconds = step.durationSeconds ?: return action
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        val duration = buildString {
            if (minutes > 0) append("$minutes minute${if (minutes != 1) "s" else ""}")
            if (minutes > 0 && remSeconds > 0) append(" ")
            if (remSeconds > 0 || minutes == 0) append("$remSeconds second${if (remSeconds != 1) "s" else ""}")
        }
        return "$action for $duration"
    }

    private fun demoSteps(): List<SessionStepEntity> = listOf(
        SessionStepEntity(planSessionId = 0, orderIndex = 0, stepType = StepType.WALK, durationSeconds = 10),
        SessionStepEntity(planSessionId = 0, orderIndex = 1, stepType = StepType.RUN, durationSeconds = 10),
    )

    fun togglePause() {
        val paused = !_state.value.isPaused
        _state.value = _state.value.copy(isPaused = paused)
        if (paused) tickJob?.cancel() else resumeTicking()
    }

    fun skipStep() = viewModelScope.launch { advanceStep() }

    private fun resumeTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val current = _state.value
                if (current.isPaused || current.isFinished) continue
                if (current.remainingSeconds > 1) {
                    _state.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
                } else {
                    advanceStep()
                }
            }
        }
    }

    private suspend fun advanceStep() {
        val current = _state.value
        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.steps.size) {
            val nextStep = current.steps[nextIndex]
            _state.value = current.copy(currentIndex = nextIndex, remainingSeconds = nextStep.durationSeconds ?: 0)
            voiceCue.speak(cueFor(nextStep))
        } else {
            finish()
        }
    }

    private suspend fun finish() {
        tickJob?.cancel()
        locationJob?.cancel()
        val current = _state.value
        val steps = current.steps
        val totalDurationSeconds = steps.sumOf { it.durationSeconds ?: 0 }
        val estimatedDistanceMeters = steps.sumOf { step ->
            (step.durationSeconds ?: 0) * (step.targetSpeedMetersPerSecond ?: 0.0)
        }
        // Real GPS distance wins when we have enough fixes to trust it; the planned-step
        // estimate is the fallback for treadmill sessions or a weak/absent GPS fix.
        val distanceMeters = if (current.isOutdoor && current.trackPoints.size >= 2) {
            current.gpsDistanceMeters
        } else {
            estimatedDistanceMeters
        }
        val track = planRepository.observeActivePlan().first()?.track ?: Track.BEGINNER
        val runId = runRepository.recordRun(
            RunSessionEntity(
                planSessionId = planSessionId,
                track = track,
                environment = environment,
                startedAt = startedAt,
                durationSeconds = totalDurationSeconds,
                distanceMeters = distanceMeters,
                avgPaceSecondsPerKm = distanceMeters.takeIf { it > 0 }
                    ?.let { (totalDurationSeconds / (it / 1000.0)).toInt() },
                tempCelsius = current.weather.snapshot?.temperatureCelsius,
                humidityPercent = current.weather.snapshot?.humidityPercent,
                weatherCondition = current.weather.snapshot?.condition?.name,
            ),
        )
        planSessionId?.let { planRepository.markSessionCompleted(it) }
        voiceCue.speak("Session complete. Nice work.")
        _state.value = current.copy(isFinished = true, finishedRunId = runId)
    }

    override fun onCleared() {
        tickJob?.cancel()
        locationJob?.cancel()
    }
}
