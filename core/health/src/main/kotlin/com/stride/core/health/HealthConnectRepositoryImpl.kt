package com.stride.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import com.stride.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : HealthConnectRepository {

    // Lazy, not eager — HealthConnectClient.getOrCreate() throws if the SDK isn't actually
    // available on-device, and checkAvailability() (always called first, by every method below)
    // is what actually guards against that, not this property's own laziness.
    private val client: HealthConnectClient? by lazy {
        if (checkAvailability() == HealthConnectAvailability.AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }
    }

    override fun checkAvailability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.UNSUPPORTED
        }

    override fun requiredPermissions(): Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    override suspend fun hasAllPermissions(): Boolean {
        val healthClient = client ?: return false
        return withContext(dispatchers.io) {
            runCatching {
                healthClient.permissionController.getGrantedPermissions().containsAll(requiredPermissions())
            }.getOrDefault(false)
        }
    }

    override suspend fun writeRun(run: CompletedRunRecord): Boolean {
        val healthClient = client ?: return false
        if (!hasAllPermissions()) return false
        return withContext(dispatchers.io) {
            runCatching {
                healthClient.insertRecords(
                    // Explicit List<Record>, not inferred — Kotlin otherwise infers the common
                    // supertype of ExerciseSessionRecord/DistanceRecord as the internal (not
                    // publicly visible) IntervalRecord base class rather than the public Record
                    // interface, which insertRecords actually needs.
                    listOf<Record>(
                        ExerciseSessionRecord(
                            startTime = run.startTime,
                            startZoneOffset = null,
                            endTime = run.endTime,
                            endZoneOffset = null,
                            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                            title = "Stride run",
                            // manualEntry(), not autoRecorded() — this is Stride summarizing a
                            // GPS-tracked run after the fact, not a Health Connect sensor agent
                            // recording it live.
                            metadata = Metadata.manualEntry(),
                        ),
                        DistanceRecord(
                            startTime = run.startTime,
                            startZoneOffset = null,
                            endTime = run.endTime,
                            endZoneOffset = null,
                            distance = Length.meters(run.distanceMeters),
                            metadata = Metadata.manualEntry(),
                        ),
                    ),
                )
            }.isSuccess
        }
    }

    override suspend fun averageHeartRate(startTime: Instant, endTime: Instant): Int? {
        val healthClient = client ?: return null
        if (!hasAllPermissions()) return null
        return withContext(dispatchers.io) {
            runCatching {
                val response = healthClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startTime, endTime)),
                )
                val samples = response.records.flatMap { it.samples }
                if (samples.isEmpty()) null else samples.map { it.beatsPerMinute }.average().toInt()
            }.getOrNull()
        }
    }
}
