package com.stride.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Default retirement point from docs/foundation.md — configurable per shoe from there. */
const val DEFAULT_SHOE_RETIREMENT_METERS = 500_000.0 // 500km

@Entity(tableName = "shoes")
data class ShoeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Kept denormalized and updated on each run insert — see RunSessionDao's transaction. */
    val totalDistanceMeters: Double = 0.0,
    val retirementThresholdMeters: Double = DEFAULT_SHOE_RETIREMENT_METERS,
    val retiredAt: Instant? = null,
    val createdAt: Instant,
)
