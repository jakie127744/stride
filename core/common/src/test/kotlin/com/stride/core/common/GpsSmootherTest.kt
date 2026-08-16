package com.stride.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsSmootherTest {

    // Roughly a normal running pace: ~3m every 3 seconds ≈ 1 m/s (a brisk walk, but well within
    // "plausible" — the point is testing it's accepted, not that it's realistic running pace).
    private fun fixAt(lat: Double, lng: Double, accuracy: Float = 10f, timestampMillis: Long = 0L) =
        GpsFix(latitude = lat, longitude = lng, accuracyMeters = accuracy, timestampMillis = timestampMillis)

    @Test
    fun `first fix is accepted purely on accuracy`() {
        val smoother = GpsSmoother()
        assertTrue(smoother.accept(fixAt(40.0, -74.0, accuracy = 10f)))
    }

    @Test
    fun `a fix with poor accuracy is rejected`() {
        val smoother = GpsSmoother(maxAccuracyMeters = 30f)
        assertFalse(smoother.accept(fixAt(40.0, -74.0, accuracy = 50f)))
    }

    @Test
    fun `a fix with accuracy exactly at the threshold is accepted`() {
        val smoother = GpsSmoother(maxAccuracyMeters = 30f)
        assertTrue(smoother.accept(fixAt(40.0, -74.0, accuracy = 30f)))
    }

    @Test
    fun `a plausible consecutive movement is accepted`() {
        val smoother = GpsSmoother(maxPlausibleSpeedMetersPerSecond = 10.0)
        assertTrue(smoother.accept(fixAt(40.00000, -74.00000, timestampMillis = 0)))
        // ~11m north over 3s ≈ 3.7 m/s — a real running pace, well under the 10 m/s cap.
        assertTrue(smoother.accept(fixAt(40.00010, -74.00000, timestampMillis = 3_000)))
    }

    @Test
    fun `a GPS jump implying an impossible speed is rejected`() {
        val smoother = GpsSmoother(maxPlausibleSpeedMetersPerSecond = 10.0)
        assertTrue(smoother.accept(fixAt(40.00000, -74.00000, timestampMillis = 0)))
        // ~1.1km "moved" in 3 seconds ≈ 370 m/s — a classic multipath/urban-canyon jump artifact.
        assertFalse(smoother.accept(fixAt(40.01000, -74.00000, timestampMillis = 3_000)))
    }

    @Test
    fun `a rejected fix does not become the new baseline for the next comparison`() {
        val smoother = GpsSmoother(maxPlausibleSpeedMetersPerSecond = 10.0)
        assertTrue(smoother.accept(fixAt(40.00000, -74.00000, timestampMillis = 0)))
        assertFalse(smoother.accept(fixAt(40.01000, -74.00000, timestampMillis = 3_000))) // jump, rejected
        // Continuing plausibly from the *last accepted* fix (still the first one), not the jump.
        assertTrue(smoother.accept(fixAt(40.00010, -74.00000, timestampMillis = 6_000)))
    }

    @Test
    fun `a non-positive time interval is accepted on accuracy alone`() {
        val smoother = GpsSmoother()
        assertTrue(smoother.accept(fixAt(40.0, -74.0, timestampMillis = 5_000)))
        // Duplicate/out-of-order timestamp — can't compute a meaningful speed, shouldn't crash
        // (divide-by-zero) or reject solely because of it.
        assertTrue(smoother.accept(fixAt(40.0, -74.0, timestampMillis = 5_000)))
    }
}
