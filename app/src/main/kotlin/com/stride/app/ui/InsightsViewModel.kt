package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.core.data.insights.PaceTrendCalculator
import com.stride.core.data.insights.PersonalBestsCalculator
import com.stride.core.data.insights.WeeklyPaceTrend
import com.stride.core.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val totalRuns: Int = 0,
    val weeklyVolumeKm: Double = 0.0,
    val bestPaceSecondsPerKm: Int? = null,
    val longestRunKm: Double = 0.0,
    val currentStreakDays: Int = 0,
    /** Longest-ever streak, distinct from [currentStreakDays] — a personal best the runner keeps
     * even after the current streak ends. */
    val longestStreakDays: Int = 0,
    /** Best average pace among ~5K-or-longer runs — see PersonalBestsCalculator's doc for why
     * this is an honest proxy, not a precise split-based "fastest 5K". */
    val best5kEffortPaceSecondsPerKm: Int? = null,
    /** Most recent completed week's raw vs. heat-normalized average pace — the two are equal
     * unless that week had a hot outdoor run in it, in which case the normalized figure reads
     * faster (see PaceTrendCalculator). Null until at least one week of paced runs exists. */
    val latestWeekTrend: WeeklyPaceTrend? = null,
)

/** All computed directly from real run rows — no placeholder numbers, no chart yet (needs
 * a few real runs logged to be meaningful; see the empty-state note in the screen itself). */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    runRepository: RunRepository,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = runRepository.observeRuns()
        .map { runs ->
            if (runs.isEmpty()) return@map InsightsUiState(isLoading = false)

            val now = Instant.now()
            val weeklyVolumeMeters = runs
                .filter { ChronoUnit.DAYS.between(it.startedAt, now) < 7 }
                .sumOf { it.distanceMeters }

            val distinctDays = runs.map { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
                .distinct()
                .sortedDescending()
            var streak = 0
            var expected = java.time.LocalDate.now()
            for (day in distinctDays) {
                if (day == expected) {
                    streak++
                    expected = expected.minusDays(1)
                } else {
                    break
                }
            }

            val personalBests = PersonalBestsCalculator.compute(runs)

            InsightsUiState(
                isLoading = false,
                totalRuns = runs.size,
                weeklyVolumeKm = weeklyVolumeMeters / 1000.0,
                bestPaceSecondsPerKm = personalBests.bestPaceSecondsPerKm,
                longestRunKm = (personalBests.longestRunMeters ?: 0.0) / 1000.0,
                currentStreakDays = streak,
                longestStreakDays = personalBests.longestStreakDays,
                best5kEffortPaceSecondsPerKm = personalBests.best5kEffortPaceSecondsPerKm,
                latestWeekTrend = PaceTrendCalculator.weeklyTrend(runs).lastOrNull(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}
