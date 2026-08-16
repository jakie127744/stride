package com.stride.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.core.common.RunEnvironment
import com.stride.core.designsystem.WeatherAnimation
import com.stride.core.designsystem.StrideThemeExtras

/**
 * Asked every session, per docs/foundation.md — "Outdoor vs. treadmill" isn't assumed. Outdoor
 * now does a real weather fetch (device location + Open-Meteo) with a live animated condition
 * display, not a mockup card.
 */
@Composable
fun PreRunEnvironmentScreen(
    planSessionId: Long?,
    onContinue: (RunEnvironment) -> Unit,
    viewModel: PreRunEnvironmentViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf(RunEnvironment.OUTDOOR) }
    val weatherState by viewModel.weather.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.fetchWeather() }

    fun requestWeatherIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.fetchWeather() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(selected) {
        if (selected == RunEnvironment.OUTDOOR) requestWeatherIfNeeded()
    }

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

        if (selected == RunEnvironment.OUTDOOR) {
            WeatherCard(weatherState)
        } else {
            Text(
                "GPS is off for this session — pace comes from the treadmill or manual entry (Phase 5).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { onContinue(selected) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (planSessionId != null) "Start Session" else "Preview run screen") }
    }
}

@Composable
private fun WeatherCard(state: WeatherUiState) {
    val extendedColors = StrideThemeExtras.extendedColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        state.snapshot?.let { snapshot ->
            WeatherAnimation(
                condition = snapshot.condition,
                isDay = snapshot.isDay,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            when {
                state.isLoading -> Text("Checking conditions…", style = MaterialTheme.typography.bodyMedium)
                state.unavailable -> Text(
                    "Couldn't check conditions — location permission may be off, or you're offline. Won't block your run.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.snapshot != null -> Column {
                    Text(
                        "${state.snapshot.temperatureCelsius.toInt()}°C",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        "${state.snapshot.condition.name.lowercase().replace('_', ' ')} · ${state.snapshot.humidityPercent}% humidity",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {}
            }
            if (state.snapshot?.isHot == true) {
                Text(
                    "Warmer than usual — we'll ease today's pace target and remind you to hydrate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors.action,
                )
            } else if (state.snapshot?.isCold == true) {
                Text(
                    "Cold out — extending today's warm-up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors.recovery,
                )
            }
        }
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
