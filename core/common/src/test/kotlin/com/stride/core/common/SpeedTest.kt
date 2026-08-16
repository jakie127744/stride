package com.stride.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedTest {

    @Test
    fun `mph to meters per second round trip`() {
        val speed = 6.0.mphToMetersPerSecond()
        assertEquals(6.0, speed.metersPerSecondToMph(), 0.001)
    }

    @Test
    fun `6 mph pace is 10 minutes per mile`() {
        val speed = 6.0.mphToMetersPerSecond()
        assertEquals(600, speed.metersPerSecondToPaceSecondsPerMile())
    }

    @Test
    fun `zero speed has no pace`() {
        assertNull(0.0.metersPerSecondToPaceSecondsPerKm())
        assertNull(0.0.metersPerSecondToPaceSecondsPerMile())
    }
}
