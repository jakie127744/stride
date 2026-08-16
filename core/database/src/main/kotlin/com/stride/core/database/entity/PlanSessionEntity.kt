package com.stride.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class PlanSessionType {
    WALK_RUN_INTERVAL,
    TEMPO,
    LONG_RUN,
    EASY_RUN,
    REST,
}

enum class PlanSessionStatus {
    PLANNED,
    COMPLETED,
    SKIPPED,
    /** Set by the adaptive scheduler when compress/shift/regress moves this session's date —
     * see docs/foundation.md "Adaptive Scheduling". Distinct from SKIPPED: the runner didn't
     * lose the session, it just isn't on the original date anymore. */
    RESCHEDULED,
}

@Entity(
    tableName = "plan_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId"), Index("scheduledDate")],
)
data class PlanSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val weekNumber: Int,
    val scheduledDate: LocalDate,
    val sessionType: PlanSessionType,
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Double? = null,
    val status: PlanSessionStatus = PlanSessionStatus.PLANNED,
)
