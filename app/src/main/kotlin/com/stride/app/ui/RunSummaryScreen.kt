package com.stride.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RunSummaryScreen(
    onDone: () -> Unit,
    viewModel: RunSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLoading) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        val run = state.run
        if (run == null) {
            Text("No run found for this preview.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Text("Session complete", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Nice work", style = MaterialTheme.typography.headlineMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Distance", "%.2f km".format(run.distanceMeters / 1000.0), Modifier.weight(1f))
            StatTile("Duration", formatDuration(run.durationSeconds), Modifier.weight(1f))
        }
        run.avgPaceSecondsPerKm?.let {
            StatTile("Avg pace", "${formatDuration(it)} /km", Modifier.fillMaxWidth())
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("How hard did that feel?", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                for (v in 1..10) {
                    val selected = state.selectedRpe == v
                    Text(
                        "$v",
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { viewModel.selectRpe(v) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save session")
        }
    }
}

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

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
