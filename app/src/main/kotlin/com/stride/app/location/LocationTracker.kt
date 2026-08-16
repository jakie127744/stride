package com.stride.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    /** Meters above sea level, GPS-derived — the honest caveat: phone GPS altitude is
     * typically accurate to only ±10-20m, well short of a real barometric/DEM-backed source.
     * Good enough for a live "you're climbing" cue, not for precise elevation-gain stats. */
    val altitudeMeters: Double,
    val timestampMillis: Long,
    /** OS-reported horizontal accuracy radius in meters — feeds `RunSessionEngine`'s
     * `GpsSmoother` (see docs/roadmap.md Phase 5 "GPS smoothing"). `Location.hasAccuracy()` is
     * false only in genuinely rare cases (very old/degraded fixes); `Float.MAX_VALUE` here is a
     * deliberately-fails-the-accuracy-check sentinel rather than silently trusting an unrated fix.
     */
    val accuracyMeters: Float,
)

/**
 * Interface boundary over GPS access — kept free of the Play Services/Android types so
 * `RunSessionEngine` can be unit-tested against a fake instead of a real
 * `FusedLocationProviderClient`. See [FusedLocationTracker] for the real implementation and
 * [com.stride.app.di.AppModule] for the Hilt binding. Mirrors the same "don't block on missing
 * architecture" call made for VoiceCueSpeaker.
 */
interface LocationTracker {
    /** One-shot fix for weather lookups — null if permission isn't granted or no fix is available. */
    suspend fun lastKnownLocation(): TrackPoint?

    /** Continuous updates for the in-run map/elevation panel. Empty flow if permission is missing
     * — the caller decides how to surface that (see ActiveRunViewModel). */
    fun observeLocationUpdates(): Flow<TrackPoint>
}

/**
 * :app-local for now rather than a dedicated :core:location module — Phase 2's scaffold didn't
 * anticipate needing this ahead of Phase 5, and a full GPS/Health Connect module split can
 * happen when Phase 5 properly lands.
 */
@Singleton
class FusedLocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationTracker {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun lastKnownLocation(): TrackPoint? {
        if (!hasPermission()) return null
        val location = runCatching { client.lastLocation.await() }.getOrNull() ?: return null
        return location.toTrackPoint()
    }

    @SuppressLint("MissingPermission")
    override fun observeLocationUpdates(): Flow<TrackPoint> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toTrackPoint()) }
            }
        }
        client.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun android.location.Location.toTrackPoint() = TrackPoint(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = altitude,
        timestampMillis = time,
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    )
}
