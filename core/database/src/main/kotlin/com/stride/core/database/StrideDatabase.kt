package com.stride.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stride.core.database.dao.PlanDao
import com.stride.core.database.dao.RunSessionDao
import com.stride.core.database.dao.ShoeDao
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.SessionStepEntity
import com.stride.core.database.entity.ShoeEntity

@Database(
    entities = [
        RunSessionEntity::class,
        PlanEntity::class,
        PlanSessionEntity::class,
        SessionStepEntity::class,
        ShoeEntity::class,
    ],
    version = 2, // v2: SessionStepEntity gained StepType.STRETCH + a nullable `label` column
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class StrideDatabase : RoomDatabase() {
    abstract fun runSessionDao(): RunSessionDao
    abstract fun planDao(): PlanDao
    abstract fun shoeDao(): ShoeDao

    companion object {
        const val DATABASE_NAME = "stride.db"
    }
}
