package com.stride.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.stride.app.navigation.Destination
import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.StepType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
) {
    val currentStep: SessionStepEntity? get() = steps.getOrNull(currentIndex)
    val nextStep: SessionStepEntity? get() = steps.getOrNull(currentIndex + 1)
}

/**
 * Drives a session purely off its planned steps (time-based, per docs/foundation.md "Session
 * pacing") — no GPS or Media3 cues yet, those are Phase 4/5. The countdown itself, the
 * step-to-step progression, and writing a real run record at the end are all real.
 */
@HiltViewModel
class ActiveRunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val planRepository: PlanRepository,
    private val runRepository: RunRepository,
    private val voiceCue: VoiceCueSpeaker,
) : ViewModel() {

    private val route: Destination.ActiveRun = savedStateHandle.toRoute()
    private val planSessionId: Long? = route.planSessionId
    private val environment: RunEnvironment = if (route.outdoor) RunEnvironment.OUTDOOR else RunEnvironment.TREADMILL

    private val _state = MutableStateFlow(ActiveRunUiState())
    val state: StateFlow<ActiveRunUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private val startedAt: Instant = Instant.now()

    fun start() {
        viewModelScope.launch {
            val steps = planSessionId?.let { planRepository.observeStepsForSession(it).first() }
                ?.takeIf { it.isNotEmpty() }
                ?: demoSteps() // dev-preview entry (no real session) — still worth being able to try
            _state.value = ActiveRunUiState(
                isLoading = false,
                steps = steps,
                currentIndex = 0,
                remainingSeconds = steps.first().durationSeconds ?: 0,
            )
            voiceCue.speak(cueFor(steps.first()))
            resumeTicking()
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
        val steps = _state.value.steps
        val totalDurationSeconds = steps.sumOf { it.durationSeconds ?: 0 }
        val totalDistanceMeters = steps.sumOf { step ->
            (step.durationSeconds ?: 0) * (step.targetSpeedMetersPerSecond ?: 0.0)
        }
        val track = planRepository.observeActivePlan().first()?.track ?: Track.BEGINNER
        val runId = runRepository.recordRun(
            RunSessionEntity(
                planSessionId = planSessionId,
                track = track,
                environment = environment,
                startedAt = startedAt,
                durationSeconds = totalDurationSeconds,
                distanceMeters = totalDistanceMeters,
                avgPaceSecondsPerKm = totalDistanceMeters.takeIf { it > 0 }
                    ?.let { (totalDurationSeconds / (it / 1000.0)).toInt() },
            ),
        )
        planSessionId?.let { planRepository.markSessionCompleted(it) }
        voiceCue.speak("Session complete. Nice work.")
        _state.value = _state.value.copy(isFinished = true, finishedRunId = runId)
    }

    override fun onCleared() {
        tickJob?.cancel()
    }
}
