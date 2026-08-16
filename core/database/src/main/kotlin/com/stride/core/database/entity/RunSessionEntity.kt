package com.stride.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import java.time.Instant

/**
 * A completed run. [planSessionId] is nullable — a freeform/unplanned run is still valid and
 * still counts toward stats and shoe mileage. Weather fields are only populated when
 * [environment] is OUTDOOR, and are what let :feature:insights show a weather-normalized pace
 * trend instead of penalizing a slow run on a hot day (see docs/foundation.md).
 */
@Entity(
    tableName = "run_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PlanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["planSessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ShoeEntity::class,
            parentColumns = ["id"],
            childColumns = ["shoeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planSessionId"), Index("shoeId"), Index("startedAt")],
)
data class RunSessionEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planSessionId: Long? = null,
    val track: Track,
    val environment: RunEnvironment,
    val startedAt: Instant,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val avgPaceSecondsPerKm: Int? = null,
    val avgHeartRate: Int? = null,
    /** Rate of perceived exertion, 1–10. Captured on the post-run summary screen. */
    val rpe: Int? = null,
    val shoeId: Long? = null,
    // --- Weather snapshot (OUTDOOR only) — see :core:weather ---
    val tempCelsius: Double? = null,
    val humidityPercent: Int? = null,
    val weatherCondition: String? = null,
)
