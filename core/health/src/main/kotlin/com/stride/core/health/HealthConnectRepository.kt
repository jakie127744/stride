package com.stride.core.health

import java.time.Instant

enum class HealthConnectAvailability { AVAILABLE, NOT_INSTALLED, UNSUPPORTED }

data class CompletedRunRecord(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
)

/**
 * The read/write half of docs/roadmap.md Phase 5's Health Connect item. Every method here is
 * best-effort by design — Health Connect is an enrichment on top of Stride's own Room-backed run
 * record (the actual source of truth), never a dependency the run flow can fail on. A runner
 * without Health Connect installed, without granted permissions, or without a synced wearable
 * for HR data gets exactly the same working app either way.
 */
interface HealthConnectRepository {
    fun checkAvailability(): HealthConnectAvailability

    /** The exact permission strings Stride needs — write the run (exercise + distance), read
     * heart rate (to enrich [com.stride.core.database.entity.RunSessionEntity.avgHeartRate],
     * which existed in the schema but nothing populated until this). */
    fun requiredPermissions(): Set<String>

    suspend fun hasAllPermissions(): Boolean

    /** Returns false on any failure (unavailable, no permission, write error) rather than
     * throwing — never blocks or fails the run Stride already recorded in its own database. */
    suspend fun writeRun(run: CompletedRunRecord): Boolean

    /** Null if unavailable, no permission, or no HR samples exist for the window (e.g. no synced
     * wearable) — same "never block, just return nothing" principle as
     * [com.stride.core.weather.WeatherRepository]. */
    suspend fun averageHeartRate(startTime: Instant, endTime: Instant): Int?
}
