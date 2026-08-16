package com.stride.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stride.core.database.entity.RunSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface RunSessionDao {

    @Query("SELECT * FROM run_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE startedAt >= :sinceInclusive ORDER BY startedAt DESC")
    fun observeSince(sinceInclusive: Instant): Flow<List<RunSessionEntity>>

    @Query("SELECT * FROM run_sessions WHERE id = :id")
    suspend fun getById(id: Long): RunSessionEntity?

    @Insert
    suspend fun insert(run: RunSessionEntity): Long

    @Query("UPDATE run_sessions SET rpe = :rpe WHERE id = :runId")
    suspend fun updateRpe(runId: Long, rpe: Int)
}
