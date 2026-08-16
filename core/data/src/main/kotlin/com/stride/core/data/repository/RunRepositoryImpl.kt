package com.stride.core.data.repository

import androidx.room.withTransaction
import com.stride.core.database.StrideDatabase
import com.stride.core.database.dao.RunSessionDao
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.health.CompletedRunRecord
import com.stride.core.health.HealthConnectRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class RunRepositoryImpl @Inject constructor(
    private val database: StrideDatabase,
    private val runSessionDao: RunSessionDao,
    private val healthConnectRepository: HealthConnectRepository,
) : RunRepository {

    override fun observeRuns(): Flow<List<RunSessionEntity>> = runSessionDao.observeAll()

    override fun observeRunsSince(sinceInclusive: Instant): Flow<List<RunSessionEntity>> =
        runSessionDao.observeSince(sinceInclusive)

    override suspend fun getRun(id: Long): RunSessionEntity? = runSessionDao.getById(id)

    override suspend fun recordRun(run: RunSessionEntity): Long {
        val endTime = run.startedAt.plusSeconds(run.durationSeconds.toLong())

        // Best-effort Health Connect enrichment before persisting — a synced wearable may have
        // logged real HR data during the run's time window that Stride itself has no sensor for.
        // avgHeartRate already has a value only if some future caller sets it directly; today
        // nothing does, so this is effectively always the enrichment path, but the null-check
        // keeps a real future source (e.g. a paired HR strap) from being silently overwritten.
        val enrichedRun = if (run.avgHeartRate == null) {
            val avgHeartRate = healthConnectRepository.averageHeartRate(run.startedAt, endTime)
            if (avgHeartRate != null) run.copy(avgHeartRate = avgHeartRate) else run
        } else {
            run
        }

        val id = database.withTransaction {
            val insertedId = runSessionDao.insert(enrichedRun)
            // Local val, not `run.shoeId` inline: Kotlin won't smart-cast a nullable property
            // declared in another module even after a null check, since it can't guarantee the
            // getter is side-effect-free across a module boundary.
            val shoeId = enrichedRun.shoeId
            if (shoeId != null) {
                database.shoeDao().addDistance(shoeId, enrichedRun.distanceMeters)
            }
            insertedId
        }

        // Write-out after the transaction commits, not inside it — Health Connect is a separate
        // system with its own failure modes (unavailable, no permission, IPC error), and none of
        // those should be able to roll back a run Stride has already recorded in its own
        // database. HealthConnectRepository.writeRun() never throws by contract (see its doc).
        healthConnectRepository.writeRun(
            CompletedRunRecord(startTime = enrichedRun.startedAt, endTime = endTime, distanceMeters = enrichedRun.distanceMeters),
        )

        return id
    }

    override suspend fun updateRpe(runId: Long, rpe: Int) = runSessionDao.updateRpe(runId, rpe)
}
