package com.stride.core.database

import androidx.room.TypeConverter
import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.database.entity.PlanSessionStatus
import com.stride.core.database.entity.PlanSessionType
import com.stride.core.database.entity.StepType
import java.time.Instant
import java.time.LocalDate

/**
 * Room only stores primitives — everything else (timestamps, dates, our own enums) needs an
 * explicit conversion here. Keep converters total (no throwing on unexpected input) since a
 * failure here corrupts a whole query, not just one field.
 */
class Converters {

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToIsoString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoStringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun trackToName(value: Track?): String? = value?.name

    @TypeConverter
    fun nameToTrack(value: String?): Track? = value?.let(Track::valueOf)

    @TypeConverter
    fun environmentToName(value: RunEnvironment?): String? = value?.name

    @TypeConverter
    fun nameToEnvironment(value: String?): RunEnvironment? = value?.let(RunEnvironment::valueOf)

    @TypeConverter
    fun sessionTypeToName(value: PlanSessionType?): String? = value?.name

    @TypeConverter
    fun nameToSessionType(value: String?): PlanSessionType? = value?.let(PlanSessionType::valueOf)

    @TypeConverter
    fun sessionStatusToName(value: PlanSessionStatus?): String? = value?.name

    @TypeConverter
    fun nameToSessionStatus(value: String?): PlanSessionStatus? = value?.let(PlanSessionStatus::valueOf)

    @TypeConverter
    fun stepTypeToName(value: StepType?): String? = value?.name

    @TypeConverter
    fun nameToStepType(value: String?): StepType? = value?.let(StepType::valueOf)
}
