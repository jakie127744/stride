package com.stride.app.run

import com.stride.app.location.LocationTracker
import com.stride.app.location.TrackPoint
import com.stride.app.ui.VoiceCueSpeaker
import com.stride.app.ui.WeatherUiState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import javax.inject.Singleton

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
 * Owns the actual running session — ticking, GPS accumulation, voice cues, weather, and writing
 * the finished run — in a process-scoped [CoroutineScope], not a ViewModel's. This is the fix
 * for the gap flagged earlier: a `viewModelScope`-owned job dies with its ViewModel/Activity,
 * which Android is free to tear down under memory pressure the moment the app backgrounds —
 * mid-run, that means voice cues and GPS just stop. This engine keeps running regardless of
 * what's happening to the UI, and [com.stride.app.run.RunSessionService] keeps the *process*
 * alive by running as a foreground service for as long as the engine is active. Still not the
 * full Phase 4 `MediaSessionService` architecture (see docs/foundation.md) — that's the real,
 * more capable version of the same idea, with proper audio-session integration this doesn't have.
 */
@Singleton
class RunSessionEngine @Inject constructor(
    private val planRepository: PlanRepository,
    private val runRepository: RunRepository,
    private val voiceCue: VoiceCueSpeaker,
    private val locationTracker: LocationTracker,
    private val weatherRepository: WeatherRepository,
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ActiveRunUiState())
    val state: StateFlow<ActiveRunUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var locationJob: Job? = null
    private var startedAt: Instant = Instant.now()

    /** Identifies "which run is this" so a re-attaching ViewModel (the UI came back after the
     * process survived backgrounding) doesn't restart an already-in-progress session from
     * scratch — only a genuinely new session (different args, or the previous one finished)
     * resets the engine. */
    private var activeSessionKey: String? = null

    fun start(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        val key = "$planSessionId:$outdoor:$shoeId"
        if (activeSessionKey == key && !_state.value.isFinished) return // already running this exact session
        activeSessionKey = key

        tickJob?.cancel()
        locationJob?.cancel()
        startedAt = Instant.now()
        _state.value = ActiveRunUiState(isOutdoor = outdoor)

        engineScope.launch {
            val steps = planSessionId?.let { planRepository.observeStepsForSession(it).first() }
                ?.takeIf { it.isNotEmpty() }
                ?: demoSteps()
            _state.value = _state.value.copy(
                isLoading = false,
                steps = steps,
                currentIndex = 0,
                remainingSeconds = steps.first().durationSeconds ?: 0,
            )
            voiceCue.speak(cueFor(steps.first()))
            resumeTicking(planSessionId, outdoor, shoeId)
        }
        if (outdoor) {
            startLocationTracking()
            fetchWeather()
        }
    }

    private fun startLocationTracking() {
        locationJob = locationTracker.observeLocationUpdates()
            .onEach { point -> accumulateTrackPoint(point) }
            .launchIn(engineScope)
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
        engineScope.launch {
            val location = locationTracker.lastKnownLocation()
            val snapshot: WeatherSnapshot? = location?.let {
                weatherRepository.fetchCurrentConditions(it.latitude, it.longitude)
            }
            _state.value = _state.value.copy(
                weather = if (snapshot != null) WeatherUiState(snapshot = snapshot) else WeatherUiState(unavailable = true),
            )
        }
    }

    private fun cueFor(step: SessionStepEntity): String {
        if (step.stepType == StepType.STRETCH) {
            val name = step.label ?: "Stretch"
            val seconds = step.durationSeconds ?: return name
            return "$name — hold for $seconds seconds"
        }
        val action = when (step.stepType) {
            StepType.WALK -> "Walk"
            StepType.RUN -> "Run"
            StepType.REST -> "Rest"
            StepType.STRETCH -> "Stretch" // unreachable, handled above
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
        if (paused) {
            tickJob?.cancel()
        } else {
            val key = activeSessionKey ?: return
            val (planSessionId, outdoor, shoeId) = decodeKey(key)
            resumeTicking(planSessionId, outdoor, shoeId)
        }
    }

    fun skipStep() = engineScope.launch {
        val key = activeSessionKey ?: return@launch
        val (planSessionId, outdoor, shoeId) = decodeKey(key)
        advanceStep(planSessionId, outdoor, shoeId)
    }

    private fun resumeTicking(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        tickJob?.cancel()
        tickJob = engineScope.launch {
            while (true) {
                delay(1_000)
                val current = _state.value
                if (current.isPaused || current.isFinished) continue
                if (current.remainingSeconds > 1) {
                    _state.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
                } else {
                    advanceStep(planSessionId, outdoor, shoeId)
                }
            }
        }
    }

    private suspend fun advanceStep(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        val current = _state.value
        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.steps.size) {
            val nextStep = current.steps[nextIndex]
            _state.value = current.copy(currentIndex = nextIndex, remainingSeconds = nextStep.durationSeconds ?: 0)
            voiceCue.speak(cueFor(nextStep))
        } else {
            finish(planSessionId, outdoor, shoeId)
        }
    }

    private suspend fun finish(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        tickJob?.cancel()
        locationJob?.cancel()
        val current = _state.value
        val steps = current.steps
        val totalDurationSeconds = steps.sumOf { it.durationSeconds ?: 0 }
        val estimatedDistanceMeters = steps.sumOf { step ->
            (step.durationSeconds ?: 0) * (step.targetSpeedMetersPerSecond ?: 0.0)
        }
        val distanceMeters = if (outdoor && current.trackPoints.size >= 2) {
            current.gpsDistanceMeters
        } else {
            estimatedDistanceMeters
        }
        val track = planRepository.observeActivePlan().first()?.track ?: Track.BEGINNER
        val runId = runRepository.recordRun(
            RunSessionEntity(
                planSessionId = planSessionId,
                track = track,
                environment = if (outdoor) RunEnvironment.OUTDOOR else RunEnvironment.TREADMILL,
                startedAt = startedAt,
                durationSeconds = totalDurationSeconds,
                distanceMeters = distanceMeters,
                avgPaceSecondsPerKm = distanceMeters.takeIf { it > 0 }
                    ?.let { (totalDurationSeconds / (it / 1000.0)).toInt() },
                tempCelsius = current.weather.snapshot?.temperatureCelsius,
                humidityPercent = current.weather.snapshot?.humidityPercent,
                weatherCondition = current.weather.snapshot?.condition?.name,
                shoeId = shoeId,
            ),
        )
        planSessionId?.let { planRepository.markSessionCompleted(it) }
        voiceCue.speak("Session complete. Nice work.")
        _state.value = current.copy(isFinished = true, finishedRunId = runId)
    }

    private fun decodeKey(key: String): Triple<Long?, Boolean, Long?> {
        val parts = key.split(":")
        return Triple(parts[0].toLongOrNull(), parts[1].toBoolean(), parts[2].toLongOrNull())
    }
}
