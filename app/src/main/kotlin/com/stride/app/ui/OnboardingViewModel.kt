package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.core.common.Track
import com.stride.core.common.mphToMetersPerSecond
import com.stride.core.common.recommendedPresetForWeek
import com.stride.core.data.repository.PlanRepository
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionType
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.StepType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val planRepository: PlanRepository,
) : ViewModel() {

    /**
     * Creates a single starter session for today so Home has something real to show — full
     * multi-week plan authoring is Phase 6. Beginner gets a walk/run interval breakdown from
     * the week-1 preset (docs/foundation.md "Session pacing"); Pro gets a placeholder tempo
     * shape until the real interval builder exists.
     */
    fun selectTrack(track: Track, onCreated: (planSessionId: Long) -> Unit) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val planId = planRepository.createPlan(
                plan = PlanEntity(
                    track = track,
                    name = if (track == Track.BEGINNER) "Couch to 5K" else "Pro Training",
                    startDate = today,
                    currentWeek = 1,
                ),
                sessions = listOf(
                    PlanSessionEntity(
                        planId = 0, // overwritten by createPlan once the real planId exists
                        weekNumber = 1,
                        scheduledDate = today,
                        sessionType = if (track == Track.BEGINNER) {
                            PlanSessionType.WALK_RUN_INTERVAL
                        } else {
                            PlanSessionType.TEMPO
                        },
                    ),
                ),
            )
            val session = planRepository.getSessionOn(planId, today) ?: return@launch
            planRepository.setStepsForSession(session.id, buildSteps(track))
            onCreated(session.id)
        }
    }

    private fun buildSteps(track: Track): List<SessionStepEntity> = if (track == Track.BEGINNER) {
        val preset = recommendedPresetForWeek(1)
        // 4 walk/run pairs — a real session length, without yet asking the runner how long
        // they want to go (that prompt is part of the Phase 5 pre-run environment screen).
        (1..4).flatMap {
            listOf(
                SessionStepEntity(
                    planSessionId = 0,
                    orderIndex = 0,
                    stepType = StepType.WALK,
                    durationSeconds = preset.walkDurationSeconds,
                    targetSpeedMetersPerSecond = preset.walkSpeedMph.mphToMetersPerSecond(),
                ),
                SessionStepEntity(
                    planSessionId = 0,
                    orderIndex = 0,
                    stepType = StepType.RUN,
                    durationSeconds = preset.runDurationSeconds,
                    targetSpeedMetersPerSecond = preset.runSpeedMph.mphToMetersPerSecond(),
                ),
            )
        }
    } else {
        // Placeholder tempo shape — the real custom interval builder is Phase 6.
        (1..4).flatMap {
            listOf(
                SessionStepEntity(
                    planSessionId = 0,
                    orderIndex = 0,
                    stepType = StepType.RUN,
                    durationSeconds = 4 * 60,
                    targetSpeedMetersPerSecond = 8.0.mphToMetersPerSecond(),
                ),
                SessionStepEntity(
                    planSessionId = 0,
                    orderIndex = 0,
                    stepType = StepType.WALK,
                    durationSeconds = 60,
                    targetSpeedMetersPerSecond = 3.0.mphToMetersPerSecond(),
                ),
            )
        }
    }
}
