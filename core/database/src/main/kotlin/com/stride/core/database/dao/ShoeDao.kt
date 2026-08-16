package com.stride.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.stride.core.database.entity.ShoeEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ShoeDao {

    @Query("SELECT * FROM shoes WHERE retiredAt IS NULL ORDER BY createdAt DESC")
    fun observeActiveShoes(): Flow<List<ShoeEntity>>

    @Query("SELECT * FROM shoes ORDER BY createdAt DESC")
    fun observeAllShoes(): Flow<List<ShoeEntity>>

    @Insert
    suspend fun insert(shoe: ShoeEntity): Long

    @Update
    suspend fun update(shoe: ShoeEntity)

    @Query("UPDATE shoes SET totalDistanceMeters = totalDistanceMeters + :addedMeters WHERE id = :shoeId")
    suspend fun addDistance(shoeId: Long, addedMeters: Double)

    @Query("UPDATE shoes SET retiredAt = :retiredAt WHERE id = :shoeId")
    suspend fun retire(shoeId: Long, retiredAt: Instant)
}
