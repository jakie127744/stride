package com.stride.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.SessionStepEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<PlanEntity?>

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plan_sessions WHERE planId = :planId ORDER BY scheduledDate ASC")
    fun observeSessionsForPlan(planId: Long): Flow<List<PlanSessionEntity>>

    @Query("SELECT * FROM plan_sessions WHERE planId = :planId AND scheduledDate = :date LIMIT 1")
    suspend fun getSessionOn(planId: Long, date: LocalDate): PlanSessionEntity?

    @Insert
    suspend fun insertSessions(sessions: List<PlanSessionEntity>)

    @Update
    suspend fun updateSession(session: PlanSessionEntity)

    @Query(
        "SELECT * FROM plan_sessions WHERE planId = :planId AND status = :status " +
            "AND scheduledDate < :beforeDate ORDER BY scheduledDate ASC",
    )
    suspend fun getSessionsBefore(planId: Long, beforeDate: LocalDate, status: PlanSessionStatus): List<PlanSessionEntity>

    // --- Pacing breakdown for a session — see docs/foundation.md "Session pacing" ---

    @Query("SELECT * FROM session_steps WHERE planSessionId = :planSessionId ORDER BY orderIndex ASC")
    fun observeStepsForSession(planSessionId: Long): Flow<List<SessionStepEntity>>

    @Insert
    suspend fun insertSteps(steps: List<SessionStepEntity>)
}
