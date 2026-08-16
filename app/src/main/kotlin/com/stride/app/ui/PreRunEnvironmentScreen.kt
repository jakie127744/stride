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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stride.core.common.RunEnvironment
import com.stride.core.designsystem.StrideThemeExtras

/**
 * Asked every session, per docs/foundation.md — "Outdoor vs. treadmill" isn't assumed. Real
 * weather-fetch and coaching-adjustment copy land in Phase 5 (:core:weather); this screen
 * establishes the choice and passes it forward.
 */
@Composable
fun PreRunEnvironmentScreen(
    planSessionId: Long?,
    onContinue: (RunEnvironment) -> Unit,
) {
    var selected by remember { mutableStateOf(RunEnvironment.OUTDOOR) }
    val extendedColors = StrideThemeExtras.extendedColors

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Outside, or the belt?", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Changes how we track pace, and whether we check conditions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EnvironmentToggleOption(
                label = "Outdoor",
                selected = selected == RunEnvironment.OUTDOOR,
                onClick = { selected = RunEnvironment.OUTDOOR },
                modifier = Modifier.weight(1f),
            )
            EnvironmentToggleOption(
                label = "Treadmill",
                selected = selected == RunEnvironment.TREADMILL,
                onClick = { selected = RunEnvironment.TREADMILL },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            if (selected == RunEnvironment.OUTDOOR) {
                "GPS drives your pace and distance. Live weather conditions load once you start (Phase 5)."
            } else {
                "GPS is off for this session — pace comes from the treadmill or manual entry (Phase 5)."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { onContinue(selected) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (planSessionId != null) "Start Session" else "Preview run screen") }
    }
}

@Composable
private fun EnvironmentToggleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
