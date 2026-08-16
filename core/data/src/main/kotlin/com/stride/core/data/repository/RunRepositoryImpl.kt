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
        // Local val, not `run.shoeId` inline: Kotlin won't smart-cast a nullable property
        // declared in another module even after a null check, since it can't guarantee the
        // getter is side-effect-free across a module boundary.
        val shoeId = run.shoeId
        if (shoeId != null) {
            database.shoeDao().addDistance(shoeId, run.distanceMeters)
        }
        id
    }

    override suspend fun updateRpe(runId: Long, rpe: Int) = runSessionDao.updateRpe(runId, rpe)
}
