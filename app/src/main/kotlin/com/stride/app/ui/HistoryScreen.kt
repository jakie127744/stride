package com.stride.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.core.data.repository.isDueForRetirement
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.ShoeEntity
import com.stride.core.designsystem.StrideThemeExtras
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium) }

        item {
            Text(
                "Shoes",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.shoes.isEmpty() && !uiState.isLoading) {
            item {
                Text(
                    "No shoes yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(uiState.shoes, key = { "shoe_${it.id}" }) { shoe ->
            ShoeRow(shoe = shoe, onRetire = { viewModel.retireShoe(shoe.id) })
        }
        item { AddShoeRow(onAdd = viewModel::addShoe) }

        item {
            Text(
                "Recent runs",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (uiState.runs.isEmpty() && !uiState.isLoading) {
            item {
                Text(
                    "No runs logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(uiState.runs, key = { "run_${it.id}" }) { run -> RunRow(run) }
    }
}

@Composable
private fun ShoeRow(shoe: ShoeEntity, onRetire: () -> Unit) {
    val extendedColors = StrideThemeExtras.extendedColors
    val progress = (shoe.totalDistanceMeters / shoe.retirementThresholdMeters).toFloat().coerceIn(0f, 1f)
    val dueForRetirement = shoe.isDueForRetirement()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(shoe.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${(shoe.totalDistanceMeters / 1000).toInt()} / ${(shoe.retirementThresholdMeters / 1000).toInt()} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(
                        if (dueForRetirement) extendedColors.action else extendedColors.recovery,
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
        if (shoe.retiredAt != null) {
            Text("Retired", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (dueForRetirement) {
            Text(
                "Due for retirement — tap to retire",
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors.action,
                modifier = Modifier.clickable(onClick = onRetire),
            )
        }
    }
}

@Composable
private fun AddShoeRow(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Add a shoe") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Button(onClick = {
            onAdd(name)
            name = ""
        }) { Text("Add") }
    }
}

@Composable
private fun RunRow(run: RunSessionEntity) {
    val date = remember(run.startedAt) {
        DateTimeFormatter.ofPattern("MMM d").format(run.startedAt.atZone(ZoneId.systemDefault()))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(date, style = MaterialTheme.typography.titleMedium)
            Text(
                run.track.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text("%.2f km".format(run.distanceMeters / 1000.0), style = MaterialTheme.typography.titleMedium)
            Text(
                "${run.durationSeconds / 60}:${(run.durationSeconds % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
