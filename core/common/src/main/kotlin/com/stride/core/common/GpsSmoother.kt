package com.stride.core.common

/** A single raw GPS fix, before any smoothing decision has been made about it. */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)

/**
 * Filters raw GPS fixes for the two dominant sources of noise phone GPS produces — see
 * docs/roadmap.md Phase 5 "GPS smoothing (raw fixes are noisier than the haversine-between-fixes
 * approach)":
 *
 * 1. A fix reported with poor accuracy (the OS-reported accuracy radius, not a guess).
 * 2. A fix that's physically implausible given how fast a runner could actually have moved since
 *    the last *accepted* fix — urban-canyon/multipath reflection commonly reports a fix hundreds
 *    of meters away with an otherwise-normal accuracy value, which accuracy filtering alone won't
 *    catch.
 *
 * Rejected fixes are simply dropped, not corrected or interpolated — guessing a "corrected"
 * position risks being more wrong than just skipping one noisy sample, and the next real fix a
 * few seconds later self-corrects the track either way.
 *
 * Stateful (remembers the last *accepted* fix, not the last fix seen) and NOT thread-safe by
 * design — callers own their own instance and call [accept] from one place, matching how
 * `RunSessionEngine` already serializes GPS handling through its own state updates.
 */
class GpsSmoother(
    private val maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
    private val maxPlausibleSpeedMetersPerSecond: Double = DEFAULT_MAX_PLAUSIBLE_SPEED_MPS,
) {
    private var lastAccepted: GpsFix? = null

    /** True if [fix] should be trusted and folded into the run's track/distance. */
    fun accept(fix: GpsFix): Boolean {
        if (fix.accuracyMeters > maxAccuracyMeters) return false

        val previous = lastAccepted
        if (previous != null) {
            val elapsedSeconds = (fix.timestampMillis - previous.timestampMillis) / 1000.0
            // A non-positive interval (duplicate/out-of-order timestamp) can't produce a
            // meaningful speed estimate — accept on accuracy alone rather than divide by zero.
            if (elapsedSeconds > 0) {
                val distance = haversineDistanceMeters(previous.latitude, previous.longitude, fix.latitude, fix.longitude)
                val impliedSpeed = distance / elapsedSeconds
                if (impliedSpeed > maxPlausibleSpeedMetersPerSecond) return false
            }
        }

        lastAccepted = fix
        return true
    }

    companion object {
        /** Phone GPS regularly reports 5-15m accuracy outdoors in the open; 30m is generous
         * enough to not reject normal urban/tree-cover fixes while still catching genuinely bad
         * ones (100m+, common right after acquiring a fix or under heavy cover). */
        const val DEFAULT_MAX_ACCURACY_METERS = 30f

        /** ~36 km/h — well above elite 5K race pace (~19-20 km/h) and even mile-pace sprinting
         * (~24-28 km/h), but well below what a GPS jump artifact implies (a few hundred meters
         * "moved" in one 3-second fix interval is 30+ m/s). Generous on purpose: this only needs
         * to catch jump artifacts, not enforce a pace ceiling. */
        const val DEFAULT_MAX_PLAUSIBLE_SPEED_MPS = 10.0
    }
}
