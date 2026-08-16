package com.stride.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StepType { WALK, RUN, REST, STRETCH }

/**
 * One ordered step inside a session's pacing plan — e.g. "walk 4:00 at 3.0 mph, then run 1:00
 * at 6.0 mph, repeated 6 times" is six WALK/RUN row pairs, not a single "repeat" row. Expanding
 * repeats at generation time keeps every read (the run screen's "what's next", the summary,
 * the interval-strip UI) a plain ordered query instead of runtime repeat-unrolling logic.
 *
 * Exactly one of [durationSeconds] / [distanceMeters] is set — time-based steps for walk/run
 * intervals (the common case), distance-based for track-style reps. [targetSpeedMetersPerSecond]
 * is null for "just go by feel" steps (early beginner weeks intentionally avoid a pace target).
 */
@Entity(
    tableName = "session_steps",
    foreignKeys = [
        ForeignKey(
            entity = PlanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["planSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planSessionId")],
)
data class SessionStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planSessionId: Long,
    val orderIndex: Int,
    val stepType: StepType,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val targetSpeedMetersPerSecond: Double? = null,
    /** Named stretch/exercise for STRETCH steps only (e.g. "Calf stretch") — see
     * [com.stride.core.common.StretchExercise]. Null for every other step type. */
    val label: String? = null,
)
