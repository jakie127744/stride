package com.stride.core.data.insights

import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.database.entity.RunSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class PaceTrendCalculatorTest {

    private fun run(
        weekStart: LocalDate,
        dayOffset: Long = 0,
        avgPaceSecondsPerKm: Int?,
        tempCelsius: Double? = null,
    ) = RunSessionEntity(
        track = Track.BEGINNER,
        environment = RunEnvironment.OUTDOOR,
        startedAt = weekStart.plusDays(dayOffset).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        durationSeconds = 1_800,
        distanceMeters = 5_000.0,
        avgPaceSecondsPerKm = avgPaceSecondsPerKm,
        tempCelsius = tempCelsius,
    )

    private val monday = LocalDate.of(2026, 8, 17).also { assertEquals(DayOfWeek.MONDAY, it.dayOfWeek) }

    @Test
    fun `empty run list produces no weeks`() {
        assertTrue(PaceTrendCalculator.weeklyTrend(emptyList()).isEmpty())
    }

    @Test
    fun `runs group into the correct Monday-start week`() {
        val runs = listOf(
            run(monday, dayOffset = 0, avgPaceSecondsPerKm = 300),
            run(monday, dayOffset = 6, avgPaceSecondsPerKm = 320), // same week, Sunday
        )
        val trend = PaceTrendCalculator.weeklyTrend(runs)
        assertEquals(1, trend.size)
        assertEquals(monday, trend.first().weekStart)
        assertEquals(310, trend.first().rawAvgPaceSecondsPerKm) // (300+320)/2
    }

    @Test
    fun `a comfortable-weather run's normalized pace equals its raw pace`() {
        val runs = listOf(run(monday, avgPaceSecondsPerKm = 300, tempCelsius = 18.0))
        val trend = PaceTrendCalculator.weeklyTrend(runs).first()
        assertEquals(trend.rawAvgPaceSecondsPerKm, trend.normalizedAvgPaceSecondsPerKm)
    }

    @Test
    fun `a hot run's normalized pace reads faster than its raw pace`() {
        val runs = listOf(run(monday, avgPaceSecondsPerKm = 300, tempCelsius = 32.0))
        val trend = PaceTrendCalculator.weeklyTrend(runs).first()
        assertEquals(300, trend.rawAvgPaceSecondsPerKm)
        assertTrue(trend.normalizedAvgPaceSecondsPerKm < trend.rawAvgPaceSecondsPerKm)
        assertEquals(270, trend.normalizedAvgPaceSecondsPerKm) // 300 * 0.9
    }

    @Test
    fun `a run with no recorded temperature is treated as not-hot rather than excluded`() {
        val runs = listOf(run(monday, avgPaceSecondsPerKm = 300, tempCelsius = null))
        val trend = PaceTrendCalculator.weeklyTrend(runs).first()
        assertEquals(300, trend.rawAvgPaceSecondsPerKm)
        assertEquals(300, trend.normalizedAvgPaceSecondsPerKm)
    }

    @Test
    fun `runs without a pace value are excluded entirely, not counted as a zero`() {
        val runs = listOf(run(monday, avgPaceSecondsPerKm = null), run(monday, dayOffset = 1, avgPaceSecondsPerKm = 300))
        val trend = PaceTrendCalculator.weeklyTrend(runs).first()
        assertEquals(300, trend.rawAvgPaceSecondsPerKm)
    }

    @Test
    fun `weeks come back sorted chronologically`() {
        val runs = listOf(
            run(monday.plusWeeks(2), avgPaceSecondsPerKm = 300),
            run(monday, avgPaceSecondsPerKm = 310),
            run(monday.plusWeeks(1), avgPaceSecondsPerKm = 305),
        )
        val trend = PaceTrendCalculator.weeklyTrend(runs)
        assertEquals(listOf(monday, monday.plusWeeks(1), monday.plusWeeks(2)), trend.map { it.weekStart })
    }
}
