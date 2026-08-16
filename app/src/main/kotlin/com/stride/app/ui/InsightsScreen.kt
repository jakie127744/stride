package com.stride.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineMedium)

        if (uiState.isLoading) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
        } else if (uiState.totalRuns == 0) {
            Text(
                "No runs logged yet — complete a session and these numbers become real. " +
                    "The full pace trend chart from the wireframes still needs several weeks " +
                    "of outdoor runs to mean anything.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Weekly volume", "%.1f km".format(uiState.weeklyVolumeKm), Modifier.weight(1f))
                StatTile("Streak", "${uiState.currentStreakDays} days", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total runs", "${uiState.totalRuns}", Modifier.weight(1f))
                StatTile("Longest run", "%.2f km".format(uiState.longestRunKm), Modifier.weight(1f))
            }
            uiState.bestPaceSecondsPerKm?.let {
                StatTile("Best pace", formatPace(it), Modifier.fillMaxWidth())
            }

            Text("Personal bests", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Longest streak", "${uiState.longestStreakDays} days", Modifier.weight(1f))
                StatTile(
                    "Best 5K effort",
                    uiState.best5kEffortPaceSecondsPerKm?.let { formatPace(it) } ?: "—",
                    Modifier.weight(1f),
                )
            }

            uiState.latestWeekTrend?.let { trend ->
                Text("This week's pace", style = MaterialTheme.typography.titleMedium)
                if (trend.rawAvgPaceSecondsPerKm == trend.normalizedAvgPaceSecondsPerKm) {
                    StatTile("Average pace", formatPace(trend.rawAvgPaceSecondsPerKm), Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Raw average", formatPace(trend.rawAvgPaceSecondsPerKm), Modifier.weight(1f))
                        StatTile("Weather-normalized", formatPace(trend.normalizedAvgPaceSecondsPerKm), Modifier.weight(1f))
                    }
                    Text(
                        "A hot run this week reads slower than it really was — the normalized " +
                            "figure adjusts for that so a hot week doesn't look like lost fitness.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatPace(secondsPerKm: Int): String =
    "${secondsPerKm / 60}:${(secondsPerKm % 60).toString().padStart(2, '0')} /km"

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
