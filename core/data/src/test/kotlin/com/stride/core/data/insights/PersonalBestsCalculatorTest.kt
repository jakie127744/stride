package com.stride.core.data.insights

import com.stride.core.common.RunEnvironment
import com.stride.core.common.Track
import com.stride.core.database.entity.RunSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PersonalBestsCalculatorTest {

    private fun run(
        id: Long,
        startedAt: Instant,
        distanceMeters: Double,
        avgPaceSecondsPerKm: Int? = null,
    ) = RunSessionEntity(
        id = id,
        track = Track.BEGINNER,
        environment = RunEnvironment.OUTDOOR,
        startedAt = startedAt,
        durationSeconds = 1_800,
        distanceMeters = distanceMeters,
        avgPaceSecondsPerKm = avgPaceSecondsPerKm,
    )

    @Test
    fun `empty run list returns all-null bests`() {
        val bests = PersonalBestsCalculator.compute(emptyList())
        assertNull(bests.longestRunMeters)
        assertEquals(0, bests.longestStreakDays)
        assertNull(bests.bestPaceSecondsPerKm)
        assertNull(bests.best5kEffortPaceSecondsPerKm)
    }

    @Test
    fun `longest run and best pace pick the correct extremes`() {
        val runs = listOf(
            run(1, Instant.now(), distanceMeters = 3_000.0, avgPaceSecondsPerKm = 360),
            run(2, Instant.now(), distanceMeters = 8_000.0, avgPaceSecondsPerKm = 300),
            run(3, Instant.now(), distanceMeters = 5_000.0, avgPaceSecondsPerKm = 330),
        )
        val bests = PersonalBestsCalculator.compute(runs)
        assertEquals(8_000.0, bests.longestRunMeters)
        assertEquals(300, bests.bestPaceSecondsPerKm) // the fastest (lowest) seconds/km
    }

    @Test
    fun `5K-effort best pace only considers runs at or above the threshold`() {
        val runs = listOf(
            run(1, Instant.now(), distanceMeters = 3_000.0, avgPaceSecondsPerKm = 250), // fast but too short
            run(2, Instant.now(), distanceMeters = 5_000.0, avgPaceSecondsPerKm = 300),
        )
        val bests = PersonalBestsCalculator.compute(runs)
        assertEquals(250, bests.bestPaceSecondsPerKm) // overall best still includes the short run
        assertEquals(300, bests.best5kEffortPaceSecondsPerKm) // 5K-effort best excludes it
    }

    @Test
    fun `no run reaches 5K-effort length yields a null 5K best`() {
        val runs = listOf(run(1, Instant.now(), distanceMeters = 2_000.0, avgPaceSecondsPerKm = 300))
        assertNull(PersonalBestsCalculator.compute(runs).best5kEffortPaceSecondsPerKm)
    }

    @Test
    fun `runs without a pace value are simply excluded from pace stats`() {
        val runs = listOf(run(1, Instant.now(), distanceMeters = 5_000.0, avgPaceSecondsPerKm = null))
        val bests = PersonalBestsCalculator.compute(runs)
        assertNull(bests.bestPaceSecondsPerKm)
        assertNull(bests.best5kEffortPaceSecondsPerKm)
        assertEquals(5_000.0, bests.longestRunMeters) // distance stat is unaffected
    }

    @Test
    fun `a run of consecutive days computes the correct longest streak`() {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val runs = (0..4).map { dayOffset -> run(dayOffset.toLong(), start.plus(dayOffset.toLong(), ChronoUnit.DAYS), 3_000.0) }
        assertEquals(5, PersonalBestsCalculator.compute(runs).longestStreakDays)
    }

    @Test
    fun `a gap breaks the streak and the longest of multiple streaks wins`() {
        val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
        // Two days, gap, three days -> longest streak is 3, not 2 and not 5.
        val runs = listOf(0, 1, 5, 6, 7).mapIndexed { index, dayOffset ->
            run(index.toLong(), start.plus(dayOffset.toLong(), ChronoUnit.DAYS), 3_000.0)
        }
        assertEquals(3, PersonalBestsCalculator.compute(runs).longestStreakDays)
    }

    @Test
    fun `multiple runs on the same day count once toward the streak`() {
        val day = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val runs = listOf(run(1, day, 3_000.0), run(2, day.plusSeconds(3_600), 3_000.0))
        assertEquals(1, PersonalBestsCalculator.compute(runs).longestStreakDays)
    }
}
