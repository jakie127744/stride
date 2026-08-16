package com.stride.core.data.repository

import androidx.room.withTransaction
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.PlanDao
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.SessionStepEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val database: StrideDatabase,
    private val planDao: PlanDao,
) : PlanRepository {

    override fun observeActivePlan(): Flow<PlanEntity?> = planDao.observeActivePlan()

    override fun observeSessionsForPlan(planId: Long): Flow<List<PlanSessionEntity>> =
        planDao.observeSessionsForPlan(planId)

    override suspend fun getSessionOn(planId: Long, date: LocalDate): PlanSessionEntity? =
        planDao.getSessionOn(planId, date)

    override suspend fun createPlan(plan: PlanEntity, sessions: List<PlanSessionEntity>): Long =
        database.withTransaction {
            // A new plan is always the runner's one active plan — re-running onboarding (or any
            // future re-onboarding flow) must not leave two rows with isActive=1, or Home can get
            // permanently stuck showing whichever one SQLite happens to return first.
            planDao.deactivateAllPlans()
            val planId = planDao.insertPlan(plan.copy(isActive = true))
            if (sessions.isNotEmpty()) {
                planDao.insertSessions(sessions.map { it.copy(planId = planId) })
            }
            planId
        }

    override suspend fun markSessionCompleted(planSessionId: Long) {
        planDao.updateSessionStatus(planSessionId, PlanSessionStatus.COMPLETED)
    }

    override fun observeStepsForSession(planSessionId: Long): Flow<List<SessionStepEntity>> =
        planDao.observeStepsForSession(planSessionId)

    override suspend fun setStepsForSession(planSessionId: Long, steps: List<SessionStepEntity>) {
        planDao.insertSteps(steps.mapIndexed { index, step -> step.copy(planSessionId = planSessionId, orderIndex = index) })
    }
}
