package com.stride.core.data.repository

import com.stride.core.database.entity.ShoeEntity
import kotlinx.coroutines.flow.Flow

interface ShoeRepository {
    fun observeActiveShoes(): Flow<List<ShoeEntity>>
    fun observeAllShoes(): Flow<List<ShoeEntity>>
    suspend fun addShoe(name: String, retirementThresholdMeters: Double): Long
    suspend fun retireShoe(shoeId: Long)
}

/** True once a shoe has covered its configured retirement distance — drives the proactive
 * "these shoes are due for retirement" nudge from docs/foundation.md, not just a passive bar. */
fun ShoeEntity.isDueForRetirement(): Boolean =
    retiredAt == null && totalDistanceMeters >= retirementThresholdMeters
