package com.stride.core.data.repository

import com.stride.core.data.scheduler.AdaptiveScheduler
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PlanRepository {
    fun observeActivePlan(): Flow<PlanEntity?>
    fun observeSessionsForPlan(planId: Long): Flow<List<PlanSessionEntity>>
    suspend fun getSessionOn(planId: Long, date: LocalDate): PlanSessionEntity?
    suspend fun createPlan(plan: PlanEntity, sessions: List<PlanSessionEntity>): Long
    suspend fun markSessionCompleted(planSessionId: Long)

    fun observeStepsForSession(planSessionId: Long): Flow<List<SessionStepEntity>>
    suspend fun setStepsForSession(planSessionId: Long, steps: List<SessionStepEntity>)

    /**
     * Compress → shift → regress-and-repeat, per docs/foundation.md "Adaptive Scheduling" — see
     * [AdaptiveScheduler] for the actual decision logic (pure, unit-tested independently of this
     * repository). Returns null if there was nothing to reconcile (no missed sessions), otherwise
     * which strategy was applied, so the caller can tell the runner what happened instead of
     * silently moving their plan around — "the runner is never shown a failed plan, only a
     * recalculated one" only holds if they're actually told it was recalculated.
     */
    suspend fun reconcileMissedSessions(planId: Long): AdaptiveScheduler.Strategy?
}
