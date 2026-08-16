package com.stride.core.data.repository

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

    // TODO(Phase 6): reconcileMissedSessions(planId) — implements the compress → shift →
    // regress-and-repeat scheduler from docs/foundation.md "Adaptive Scheduling". Left
    // unimplemented deliberately rather than half-built: the DAO surface it needs
    // (PlanDao.getSessionsBefore) already exists so Phase 6 isn't blocked on schema changes.
}
