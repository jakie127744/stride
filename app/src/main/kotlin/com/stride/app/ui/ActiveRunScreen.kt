package com.stride.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.core.database.entity.StepType
import com.stride.core.designsystem.StrideMotion
import com.stride.core.designsystem.StrideThemeExtras
import com.stride.core.designsystem.WeatherAnimation

private enum class RunTab { RUN, MAP, WEATHER }

@Composable
fun ActiveRunScreen(
    onFinished: (runId: Long) -> Unit,
    viewModel: ActiveRunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extendedColors = StrideThemeExtras.extendedColors
    var tab by remember { mutableStateOf(RunTab.RUN) }

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.isFinished, state.finishedRunId) {
        val runId = state.finishedRunId
        if (state.isFinished && runId != null) onFinished(runId)
    }

    val isWalk = state.currentStep?.stepType == StepType.WALK
    val screenColor by animateColorAsState(
        targetValue = if (isWalk) extendedColors.recovery else extendedColors.action,
        animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.StandardEasing),
        label = "runScreenColor",
    )

    Box(modifier = Modifier.fillMaxSize().background(screenColor)) {
        if (state.isLoading) {
            Text("Loading session…", modifier = Modifier.align(Alignment.Center), color = Color.White)
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // Only outdoor sessions have a track/weather to show — treadmill stays on Run.
            if (state.isOutdoor) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    RunTab.entries.forEach { candidate ->
                        val label = when (candidate) {
                            RunTab.RUN -> "Run"
                            RunTab.MAP -> "Track"
                            RunTab.WEATHER -> "Weather"
                        }
                        val selected = tab == candidate
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else Color.White.copy(alpha = .55f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { tab = candidate }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (tab) {
                    RunTab.RUN -> RunTabContent(state, viewModel)
                    RunTab.MAP -> MapTabContent(state)
                    RunTab.WEATHER -> WeatherTabContent(state)
                }
            }
        }
    }
}

@Composable
private fun RunTabContent(state: ActiveRunUiState, viewModel: ActiveRunViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Step ${state.currentIndex + 1} of ${state.steps.size}",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stepLabel(state.currentStep?.stepType), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                formatCountdown(state.remainingSeconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            state.nextStep?.let {
                Text(
                    "Next: ${stepLabel(it.stepType)} ${formatCountdown(it.durationSeconds ?: 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .85f),
                )
            }
            if (state.isOutdoor) {
                Text(
                    "%.2f km tracked".format(state.gpsDistanceMeters / 1000.0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .85f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Media-button controls for whatever's already playing (Spotify, YouTube Music, a
            // local player) — see MusicController. Not song browsing, just play/pause/skip.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = viewModel.musicController::previous, modifier = Modifier.weight(1f)) {
                    Text("⏮")
                }
                OutlinedButton(onClick = viewModel.musicController::playPause, modifier = Modifier.weight(1f)) {
                    Text("⏯")
                }
                OutlinedButton(onClick = viewModel.musicController::next, modifier = Modifier.weight(1f)) {
                    Text("⏭")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::togglePause, modifier = Modifier.weight(1f)) {
                    Text(if (state.isPaused) "Resume" else "Pause")
                }
                OutlinedButton(onClick = viewModel::skipStep, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun MapTabContent(state: ActiveRunUiState) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = .25f), RoundedCornerShape(16.dp)),
        ) {
            if (state.trackPoints.size < 2) {
                Text(
                    "Tracking your path — need a few GPS fixes first.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = Color.White.copy(alpha = .8f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TrackPolyline(state.trackPoints.map { it.latitude to it.longitude })
                Text(
                    "Raw GPS path — a real basemap (MapLibre + Protomaps) is Phase 5",
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = Color.White.copy(alpha = .5f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapStat("Elevation gain", "${state.elevationGainMeters.toInt()} m", Modifier.weight(1f))
            MapStat(
                "Altitude",
                state.trackPoints.lastOrNull()?.let { "${it.altitudeMeters.toInt()} m" } ?: "—",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TrackPolyline(points: List<Pair<Double, Double>>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val lats = points.map { it.first }
        val lngs = points.map { it.second }
        val latRange = (lats.max() - lats.min()).coerceAtLeast(0.00005)
        val lngRange = (lngs.max() - lngs.min()).coerceAtLeast(0.00005)
        val offsets = points.map { (lat, lng) ->
            Offset(
                x = ((lng - lngs.min()) / lngRange * size.width).toFloat(),
                y = (size.height - (lat - lats.min()) / latRange * size.height).toFloat(),
            )
        }
        for (i in 0 until offsets.size - 1) {
            drawLine(Color.White, offsets[i], offsets[i + 1], strokeWidth = 5f)
        }
        drawCircle(Color.White, radius = 6f, center = offsets.first())
        drawCircle(Color.Red, radius = 7f, center = offsets.last())
    }
}

@Composable
private fun MapStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = .12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .7f))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

@Composable
private fun WeatherTabContent(state: ActiveRunUiState) {
    val weather = state.weather
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .2f), RoundedCornerShape(16.dp)),
    ) {
        weather.snapshot?.let {
            WeatherAnimation(condition = it.condition, isDay = it.isDay, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            when {
                weather.isLoading -> Text("Checking conditions…", color = Color.White)
                weather.unavailable -> Text("Conditions unavailable right now.", color = Color.White)
                weather.snapshot != null -> Column {
                    Text("${weather.snapshot.temperatureCelsius.toInt()}°C", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        "${weather.snapshot.condition.name.lowercase().replace('_', ' ')} · ${weather.snapshot.humidityPercent}% humidity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = .85f),
                    )
                }
                else -> {}
            }
        }
    }
}

private fun stepLabel(type: StepType?): String = when (type) {
    StepType.WALK -> "Walk"
    StepType.RUN -> "Run"
    StepType.REST -> "Rest"
    null -> ""
}

private fun formatCountdown(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
