package com.stride.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {

    @Test
    fun `same point is zero distance`() {
        assertEquals(0.0, haversineDistanceMeters(47.6062, -122.3321, 47.6062, -122.3321), 0.001)
    }

    @Test
    fun `roughly one degree of latitude is about 111km`() {
        val distance = haversineDistanceMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue(distance in 110_000.0..112_000.0)
    }

    @Test
    fun `short GPS-fix-to-fix distance is a few meters, not zero`() {
        // ~0.00005 degrees latitude is roughly 5.5m -- the scale a run's consecutive fixes land at.
        val distance = haversineDistanceMeters(47.60000, -122.30000, 47.60005, -122.30000)
        assertTrue(distance in 4.0..7.0)
    }
}
