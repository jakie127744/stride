package com.stride.core.data.repository

import com.stride.core.common.DispatcherProvider
import com.stride.core.database.dao.ShoeDao
import com.stride.core.database.entity.DEFAULT_SHOE_RETIREMENT_METERS
import com.stride.core.database.entity.ShoeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class ShoeRepositoryImpl @Inject constructor(
    private val shoeDao: ShoeDao,
    private val dispatchers: DispatcherProvider,
) : ShoeRepository {

    override fun observeActiveShoes(): Flow<List<ShoeEntity>> = shoeDao.observeActiveShoes()

    override fun observeAllShoes(): Flow<List<ShoeEntity>> = shoeDao.observeAllShoes()

    override suspend fun addShoe(name: String, retirementThresholdMeters: Double): Long =
        withContext(dispatchers.io) {
            shoeDao.insert(
                ShoeEntity(
                    name = name,
                    retirementThresholdMeters = retirementThresholdMeters.takeIf { it > 0 }
                        ?: DEFAULT_SHOE_RETIREMENT_METERS,
                    createdAt = Instant.now(),
                ),
            )
        }

    override suspend fun retireShoe(shoeId: Long) = withContext(dispatchers.io) {
        shoeDao.retire(shoeId, Instant.now())
    }
}
