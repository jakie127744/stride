package com.stride.app.run

import com.stride.app.location.LocationTracker
import com.stride.app.location.TrackPoint
import com.stride.app.ui.VoiceCueSpeaker
import com.stride.app.ui.WeatherUiState
import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.common.DispatcherProvider
import com.stride.core.common.WeatherSnapshot
import com.stride.core.common.findStretchExercise
import com.stride.core.common.haversineDistanceMeters
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.StepType
import com.stride.core.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    /** Set by [RunSessionEngine.cancel] when the runner abandons a session early — the UI
     * navigates back without a completed-run record. Distinct from [isFinished], which means
     * the session ran to completion and was written to Room. */
    val isCancelled: Boolean = false,
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
    dispatcherProvider: DispatcherProvider,
) {
    // Injected rather than a bare Dispatchers.Default reference so a unit test can swap in a
    // TestDispatcher and deterministically control the tick loop instead of racing a real
    // 1-second delay() — see RunSessionEngineTest.
    private val engineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

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
            _state.update {
                it.copy(
                    isLoading = false,
                    steps = steps,
                    currentIndex = 0,
                    remainingSeconds = steps.first().durationSeconds ?: 0,
                )
            }
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
        // Paused means paused — a runner standing at a crosswalk (still carrying the phone,
        // still getting drifting GPS fixes) shouldn't have that drift counted into the final
        // distance. Dropping fixes while paused, rather than just freezing the tick loop, is
        // what actually keeps "Pause" honest.
        _state.update { current ->
            if (current.isPaused) return@update current
            val previous = current.trackPoints.lastOrNull()
            var addedDistance = 0.0
            var addedGain = 0.0
            if (previous != null) {
                addedDistance = haversineDistanceMeters(previous.latitude, previous.longitude, point.latitude, point.longitude)
                val altDelta = point.altitudeMeters - previous.altitudeMeters
                if (altDelta > 0) addedGain = altDelta
            }
            current.copy(
                trackPoints = current.trackPoints + point,
                gpsDistanceMeters = current.gpsDistanceMeters + addedDistance,
                elevationGainMeters = current.elevationGainMeters + addedGain,
            )
        }
    }

    private fun fetchWeather() {
        _state.update { it.copy(weather = WeatherUiState(isLoading = true)) }
        engineScope.launch {
            val location = locationTracker.lastKnownLocation()
            val snapshot: WeatherSnapshot? = location?.let {
                weatherRepository.fetchCurrentConditions(it.latitude, it.longitude)
            }
            _state.update {
                it.copy(weather = if (snapshot != null) WeatherUiState(snapshot = snapshot) else WeatherUiState(unavailable = true))
            }
            if (snapshot != null) applyWeatherAdjustment(snapshot)
        }
    }

    /**
     * Makes good on what the pre-run screen tells the runner ("we'll ease today's pace target",
     * "extending today's warm-up") instead of leaving it a UI promise with no logic behind it.
     * Only touches steps strictly after the current one — a step already in progress or done
     * doesn't retroactively change under the runner's feet. Honest limitation: weather resolves
     * asynchronously (a network round trip), so if the runner blows through the warm-up before it
     * arrives, the warm-up extension has nothing left to apply to — the hydration/cold
     * acknowledgment cue still fires either way, which is the one part the spec needs to be
     * guaranteed rather than best-effort.
     */
    private fun applyWeatherAdjustment(snapshot: WeatherSnapshot) {
        if (!snapshot.isHot && !snapshot.isCold) return
        _state.update { current ->
            val adjustedSteps = current.steps.mapIndexed { index, step ->
                if (index <= current.currentIndex) return@mapIndexed step
                when {
                    snapshot.isHot && step.stepType in listOf(StepType.WALK, StepType.RUN) ->
                        step.copy(
                            targetSpeedMetersPerSecond = step.targetSpeedMetersPerSecond?.let { it * HOT_WEATHER_PACE_EASE },
                        )
                    snapshot.isCold && step.stepType == StepType.STRETCH && isWarmUpStep(current.steps, index) ->
                        step.copy(durationSeconds = step.durationSeconds?.let { it + COLD_WARM_UP_EXTENSION_SECONDS })
                    else -> step
                }
            }
            current.copy(steps = adjustedSteps)
        }
        val cue = when {
            snapshot.isHot -> "Warmer than usual — easing your pace target. Remember to hydrate."
            snapshot.isCold -> "Cold out there — your warm-up just got a bit longer."
            else -> null
        }
        cue?.let { voiceCue.speak(it) }
    }

    /** Warm-up stretches are always the leading STRETCH run of the session (see
     * OnboardingViewModel.buildSteps: warm-up + main + cool-down, in that order) — a STRETCH
     * step preceded only by other STRETCH steps is warm-up, not cool-down. */
    private fun isWarmUpStep(steps: List<SessionStepEntity>, index: Int): Boolean =
        steps.take(index).all { it.stepType == StepType.STRETCH }

    private fun cueFor(step: SessionStepEntity): String {
        if (step.stepType == StepType.STRETCH) {
            val name = step.label ?: "Stretch"
            // OnboardingViewModel.toStep() doubles durationSeconds for perSide stretches to cover
            // both sides — the cue should say the per-side hold time (from the catalog), not the
            // doubled total, or "hold for 40 seconds" reads like one long hold instead of two.
            val exercise = step.label?.let { findStretchExercise(it) }
            if (exercise?.perSide == true) {
                return "$name — hold for ${exercise.holdSeconds} seconds, then switch sides"
            }
            val seconds = exercise?.holdSeconds ?: step.durationSeconds ?: return name
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
        _state.update { it.copy(isPaused = paused) }
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
                    val newRemaining = current.remainingSeconds - 1
                    _state.update { it.copy(remainingSeconds = newRemaining) }
                    maybeSpeakSwitchSidesCue(current, newRemaining)
                } else {
                    advanceStep(planSessionId, outdoor, shoeId)
                    // finish() runs on this same coroutine (this loop calls advanceStep calls
                    // finish, all inline) — breaking out cleanly here, rather than finish()
                    // self-cancelling tickJob, is what makes finish() actually reach its Room
                    // write. Self-cancellation mid-finish() would throw CancellationException at
                    // finish()'s own next suspension point (recordRun/markSessionCompleted),
                    // silently aborting the save before isFinished ever gets set — a real bug a
                    // unit test caught: 4/4 finish() calls failed to persist under that pattern.
                    if (_state.value.isFinished) break
                }
            }
        }
    }

    /** The upfront cue already says "hold for X seconds, then switch sides" — this is the actual
     * switch prompt at the halfway point of a perSide stretch's (doubled) hold time, so the
     * runner doesn't have to watch the countdown to know when to swap legs/arms. */
    private fun maybeSpeakSwitchSidesCue(state: ActiveRunUiState, remainingSeconds: Int) {
        val step = state.currentStep ?: return
        if (step.stepType != StepType.STRETCH) return
        val exercise = step.label?.let { findStretchExercise(it) } ?: return
        if (!exercise.perSide) return
        val total = step.durationSeconds ?: return
        if (remainingSeconds == total / 2) voiceCue.speak("Switch sides")
    }

    private suspend fun advanceStep(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        val current = _state.value
        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.steps.size) {
            val nextStep = current.steps[nextIndex]
            _state.update { it.copy(currentIndex = nextIndex, remainingSeconds = nextStep.durationSeconds ?: 0) }
            voiceCue.speak(cueFor(nextStep))
        } else {
            finish(planSessionId, outdoor, shoeId)
        }
    }

    private suspend fun finish(planSessionId: Long?, outdoor: Boolean, shoeId: Long?) {
        // Not cancelling tickJob here: finish() can run *on* that same coroutine (called via the
        // tick loop -> advanceStep -> finish chain), and self-cancelling mid-function would throw
        // CancellationException at the next suspend call below, aborting the Room write before
        // isFinished is ever set. The tick loop's own isFinished guard (resumeTicking) is what
        // actually stops it — see the comment there.
        locationJob?.cancel()
        val current = _state.value
        val steps = current.steps
        // Full session length (stretches included) is what "Duration" honestly means to the
        // runner. Pace, though, is a moving-pace figure — stretch hold-time would otherwise
        // silently dilute it, making pace look slower than the runner actually moved.
        val totalDurationSeconds = steps.sumOf { it.durationSeconds ?: 0 }
        val movingDurationSeconds = steps
            .filter { it.stepType == StepType.WALK || it.stepType == StepType.RUN }
            .sumOf { it.durationSeconds ?: 0 }
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
                    ?.let { (movingDurationSeconds / (it / 1000.0)).toInt() },
                tempCelsius = current.weather.snapshot?.temperatureCelsius,
                humidityPercent = current.weather.snapshot?.humidityPercent,
                weatherCondition = current.weather.snapshot?.condition?.name,
                shoeId = shoeId,
            ),
        )
        planSessionId?.let { planRepository.markSessionCompleted(it) }
        voiceCue.speak("Session complete. Nice work.")
        _state.update { it.copy(isFinished = true, finishedRunId = runId) }
    }

    private fun decodeKey(key: String): Triple<Long?, Boolean, Long?> {
        val parts = key.split(":")
        return Triple(parts[0].toLongOrNull(), parts[1].toBoolean(), parts[2].toLongOrNull())
    }

    /**
     * Abandons the in-progress session without writing a [RunSessionEntity] — the runner backed
     * out early rather than finishing. Stops all engine work immediately; [ActiveRunViewModel]
     * pairs this with [RunSessionService.stop] to also tear down the foreground service, closing
     * the "zombie GPS/notification keeps running after Back" gap. Safe to call when nothing is
     * running (e.g. a stray double-tap) — it's just a no-op past the job cancellations.
     */
    fun cancel() {
        tickJob?.cancel()
        locationJob?.cancel()
        activeSessionKey = null
        _state.update { it.copy(isCancelled = true) }
    }

    private companion object {
        /** A hot-weather run trims the pace target by 10% rather than declaring a hard cap —
         * still a meaningful ease without moving the goalposts on what "run pace" means. */
        const val HOT_WEATHER_PACE_EASE = 0.9
        const val COLD_WARM_UP_EXTENSION_SECONDS = 15
    }
}
