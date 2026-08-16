package com.stride.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stride.core.common.Track
import java.time.LocalDate

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val track: Track,
    val name: String,
    val startDate: LocalDate,
    /** 1-indexed. The adaptive scheduler (Phase 6) advances this — it does not always equal
     * "weeks since startDate", since compress/shift/regress can move it independently. */
    val currentWeek: Int = 1,
    val isActive: Boolean = true,
)
