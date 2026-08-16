package com.stride.core.data.repository

import com.stride.core.database.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface RunRepository {
    fun observeRuns(): Flow<List<RunSessionEntity>>
    fun observeRunsSince(sinceInclusive: Instant): Flow<List<RunSessionEntity>>
    suspend fun getRun(id: Long): RunSessionEntity?

    /**
     * Records a completed run and, if it was logged against a shoe, accumulates that distance
     * onto the shoe's total in the same transaction — see docs/foundation.md "Hardware tracking".
     * Returns the new run's id.
     */
    suspend fun recordRun(run: RunSessionEntity): Long

    /** Captured on the post-run summary screen, after the run row already exists. */
    suspend fun updateRpe(runId: Long, rpe: Int)
}
