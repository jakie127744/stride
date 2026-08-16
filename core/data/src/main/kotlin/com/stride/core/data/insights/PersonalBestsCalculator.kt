package com.stride.core.data.insights

import com.stride.core.database.entity.RunSessionEntity
import java.time.ZoneId

data class PersonalBests(
    val longestRunMeters: Double?,
    /** Longest-ever run-on-consecutive-calendar-days streak, distinct from a "current streak"
     * stat (which resets to 0 the moment a day is missed) — a personal best is a milestone the
     * runner keeps even after the streak itself ends. */
    val longestStreakDays: Int,
    val bestPaceSecondsPerKm: Int?,
    /** Best average pace among runs of at least ~5K — an honest proxy for "fastest 5K", not a
     * precise split-based measurement: Stride doesn't persist GPS track points once a run ends
     * (only the aggregate stats), so genuine sub-run split analysis ("fastest 1K within a longer
     * run") isn't possible without a schema change this pass doesn't make. A dedicated 5K-length
     * run's average pace is the honest stand-in. */
    val best5kEffortPaceSecondsPerKm: Int?,
)

/** Pure — no I/O, no Android dependency — computed from whatever run rows the caller already
 * has, same "just a function of the data" shape as [com.stride.core.common.GpsSmoother] and
 * [com.stride.core.data.scheduler.AdaptiveScheduler]. */
object PersonalBestsCalculator {

    /** ~4.5km, not a strict 5.0km — a real outdoor 5K effort routinely comes in a little short
     * of exactly 5000m (GPS accuracy, tangent-cutting, a slightly short course), and excluding
     * every run that isn't *precisely* 5K+ would make this stat rarer than it needs to be. */
    private const val FIVE_K_EFFORT_THRESHOLD_METERS = 4_500.0

    fun compute(runs: List<RunSessionEntity>): PersonalBests {
        if (runs.isEmpty()) {
            return PersonalBests(longestRunMeters = null, longestStreakDays = 0, bestPaceSecondsPerKm = null, best5kEffortPaceSecondsPerKm = null)
        }
        return PersonalBests(
            longestRunMeters = runs.maxOf { it.distanceMeters },
            longestStreakDays = longestStreakDays(runs),
            bestPaceSecondsPerKm = runs.mapNotNull { it.avgPaceSecondsPerKm }.minOrNull(),
            best5kEffortPaceSecondsPerKm = runs
                .filter { it.distanceMeters >= FIVE_K_EFFORT_THRESHOLD_METERS }
                .mapNotNull { it.avgPaceSecondsPerKm }
                .minOrNull(),
        )
    }

    private fun longestStreakDays(runs: List<RunSessionEntity>): Int {
        val days = runs
            .map { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        if (days.isEmpty()) return 0

        var longest = 1
        var current = 1
        for (i in 1 until days.size) {
            current = if (days[i] == days[i - 1].plusDays(1)) current + 1 else 1
            longest = maxOf(longest, current)
        }
        return longest
    }
}
