package com.stride.core.data.repository

import androidx.room.withTransaction
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.PlanDao
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.SessionStepEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val database: StrideDatabase,
    private val planDao: PlanDao,
) : PlanRepository {

    override fun observeActivePlan(): Flow<PlanEntity?> = planDao.observeActivePlan()

    override fun observeSessionsForPlan(planId: Long): Flow<List<PlanSessionEntity>> =
        planDao.observeSessionsForPlan(planId)

    override suspend fun createPlan(plan: PlanEntity, sessions: List<PlanSessionEntity>): Long =
        database.withTransaction {
            val planId = planDao.insertPlan(plan)
            if (sessions.isNotEmpty()) {
                planDao.insertSessions(sessions.map { it.copy(planId = planId) })
            }
            planId
        }

    override suspend fun markSessionCompleted(session: PlanSessionEntity) {
        planDao.updateSession(session.copy(status = PlanSessionStatus.COMPLETED))
    }

    override fun observeStepsForSession(planSessionId: Long): Flow<List<SessionStepEntity>> =
        planDao.observeStepsForSession(planSessionId)

    override suspend fun setStepsForSession(planSessionId: Long, steps: List<SessionStepEntity>) {
        planDao.insertSteps(steps.mapIndexed { index, step -> step.copy(planSessionId = planSessionId, orderIndex = index) })
    }
}
