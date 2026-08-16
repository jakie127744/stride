package com.stride.core.data.repository

import androidx.room.withTransaction
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.RunSessionDao
import com.stride.core.database.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class RunRepositoryImpl @Inject constructor(
    private val database: StrideDatabase,
    private val runSessionDao: RunSessionDao,
) : RunRepository {

    override fun observeRuns(): Flow<List<RunSessionEntity>> = runSessionDao.observeAll()

    override fun observeRunsSince(sinceInclusive: Instant): Flow<List<RunSessionEntity>> =
        runSessionDao.observeSince(sinceInclusive)

    override suspend fun getRun(id: Long): RunSessionEntity? = runSessionDao.getById(id)

    override suspend fun recordRun(run: RunSessionEntity): Long = database.withTransaction {
        val id = runSessionDao.insert(run)
        if (run.shoeId != null) {
            database.shoeDao().addDistance(run.shoeId, run.distanceMeters)
        }
        id
    }
}
