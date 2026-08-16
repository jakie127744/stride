package com.stride.core.data.insights

import com.stride.core.database.entity.RunSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class WeeklyPaceTrend(
    val weekStart: LocalDate,
    val rawAvgPaceSecondsPerKm: Int,
    /** The same average, adjusted for outdoor heat — a hot week's raw pace numbers read as lost
     * fitness unless something accounts for the conditions, which is exactly what
     * docs/foundation.md's "Performance Insights" spec calls "weather-normalized". Uses the same
     * heat threshold/ease factor `RunSessionEngine.applyWeatherAdjustment` applies live during a
     * hot run, so the two stay consistent with each other rather than drifting apart. */
    val normalizedAvgPaceSecondsPerKm: Int,
)

/** Pure — no I/O — groups runs into Monday-start calendar weeks and averages pace within each,
 * both raw and heat-normalized. Only runs with both a pace and (for normalization) a stored
 * outdoor temperature contribute; a treadmill run or one from before weather was recorded just
 * contributes its raw pace unchanged to the normalized figure too. */
object PaceTrendCalculator {

    /** Matches RunSessionEngine.HOT_WEATHER_PACE_EASE — the live in-run adjustment and this
     * after-the-fact trend normalization apply the same correction for the same reason. */
    private const val HOT_WEATHER_PACE_EASE = 0.9
    private const val HOT_THRESHOLD_CELSIUS = 27.0

    fun weeklyTrend(runs: List<RunSessionEntity>): List<WeeklyPaceTrend> =
        runs
            .filter { it.avgPaceSecondsPerKm != null }
            .groupBy { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate().with(DayOfWeek.MONDAY) }
            .toSortedMap()
            .map { (weekStart, weekRuns) ->
                val rawPaces = weekRuns.map { requireNotNull(it.avgPaceSecondsPerKm) }
                val normalizedPaces = weekRuns.map { run ->
                    val pace = requireNotNull(run.avgPaceSecondsPerKm).toDouble()
                    val isHot = (run.tempCelsius ?: Double.MIN_VALUE) >= HOT_THRESHOLD_CELSIUS
                    // A hot run's raw pace (seconds/km) reads slower than a comfortable-weather
                    // effort would have — multiplying by 0.9 pulls that number back down, so a
                    // hot week's normalized figure reads closer to what the same underlying
                    // fitness would have produced without the heat handicap.
                    if (isHot) pace * HOT_WEATHER_PACE_EASE else pace
                }
                WeeklyPaceTrend(
                    weekStart = weekStart,
                    rawAvgPaceSecondsPerKm = rawPaces.average().toInt(),
                    normalizedAvgPaceSecondsPerKm = normalizedPaces.average().toInt(),
                )
            }
}
