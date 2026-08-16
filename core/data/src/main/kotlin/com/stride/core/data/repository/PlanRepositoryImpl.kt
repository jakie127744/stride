package com.stride.core.data.repository

import androidx.room.withTransaction
import com.stride.core.data.scheduler.AdaptiveScheduler
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.PlanDao
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.SessionStepEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** A session logged with RPE at or above this counts as "felt hard" for the adaptive
 * scheduler's regress trigger — same 1-10 scale docs/foundation.md's RPE capture uses. */
private const val HARD_RPE_THRESHOLD = 8

/** How many of the runner's most recent completed sessions on this plan to look at when
 * deciding whether the recent trend has been hard — one bad day isn't a trend. */
private const val RECENT_SESSIONS_FOR_RPE_TREND = 2

class PlanRepositoryImpl @Inject constructor(
    private val database: StrideDatabase,
    private val planDao: PlanDao,
    private val runRepository: RunRepository,
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

    override suspend fun reconcileMissedSessions(planId: Long): AdaptiveScheduler.Strategy? {
        val sessions = planDao.observeSessionsForPlan(planId).first()
        if (sessions.isEmpty()) return null

        val reconciliation = AdaptiveScheduler.reconcile(
            today = LocalDate.now(),
            sessions = sessions,
            recentSessionsFeltHard = recentSessionsFeltHard(sessions),
        ) ?: return null

        database.withTransaction {
            planDao.updateSessions(reconciliation.updatedSessions)
        }
        return reconciliation.strategy
    }

    /** Looks at the plan's most recent *completed* runs (via RunSessionEntity.planSessionId, not
     * PlanSessionEntity itself — RPE is captured on the run record, on the post-run summary
     * screen) to decide whether the runner's recent trend has been hard enough to regress rather
     * than just reschedule. */
    private suspend fun recentSessionsFeltHard(sessions: List<PlanSessionEntity>): Boolean {
        val sessionIds = sessions.map { it.id }.toSet()
        val recentRuns = runRepository.observeRuns().first()
            .filter { it.planSessionId in sessionIds && it.rpe != null }
            .sortedByDescending { it.startedAt }
            .take(RECENT_SESSIONS_FOR_RPE_TREND)
        return recentRuns.isNotEmpty() && recentRuns.all { (it.rpe ?: 0) >= HARD_RPE_THRESHOLD }
    }
}
