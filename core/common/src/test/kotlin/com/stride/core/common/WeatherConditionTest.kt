package com.stride.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `code 0 is clear`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
    }

    @Test
    fun `code 3 is cloudy`() {
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(3))
    }

    @Test
    fun `code 61 (rain) maps to rain`() {
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(61))
    }

    @Test
    fun `code 75 (heavy snow) maps to snow`() {
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(75))
    }

    @Test
    fun `code 95 (thunderstorm) maps to thunderstorm`() {
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
    }

    @Test
    fun `unknown code falls back to cloudy rather than crashing`() {
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(999))
    }

    @Test
    fun `hot and cold thresholds`() {
        val hot = WeatherSnapshot(28.0, 40, 5.0, WeatherCondition.CLEAR, true)
        val cold = WeatherSnapshot(3.0, 40, 5.0, WeatherCondition.CLEAR, true)
        val mild = WeatherSnapshot(18.0, 40, 5.0, WeatherCondition.CLEAR, true)
        assertEquals(true, hot.isHot)
        assertEquals(true, cold.isCold)
        assertEquals(false, mild.isHot)
        assertEquals(false, mild.isCold)
    }
}
