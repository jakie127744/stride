package com.stride.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.app.run.ActiveRunUiState
import com.stride.core.database.entity.StepType
import com.stride.core.designsystem.RunnerFigure
import com.stride.core.designsystem.RunnerMode
import com.stride.core.designsystem.StrideMotion
import com.stride.core.designsystem.StrideThemeExtras
import com.stride.core.designsystem.WeatherAnimation

private enum class RunTab { RUN, MAP, WEATHER }

@Composable
fun ActiveRunScreen(
    onFinished: (runId: Long) -> Unit,
    onCancelled: () -> Unit,
    viewModel: ActiveRunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extendedColors = StrideThemeExtras.extendedColors
    var tab by remember { mutableStateOf(RunTab.RUN) }
    var showEndConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state.isFinished, state.finishedRunId) {
        val runId = state.finishedRunId
        if (state.isFinished && runId != null) onFinished(runId)
    }
    LaunchedEffect(state.isCancelled) {
        if (state.isCancelled) onCancelled()
    }

    // Back — whether the system gesture/button or Android's predictive-back — abandons the run
    // rather than silently leaving the engine (and its foreground service) running behind the
    // next screen. Confirm first: this is a real "lose your progress" action, not a passive nav.
    BackHandler { showEndConfirm = true }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End this run?") },
            text = { Text("Your progress won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm = false
                    viewModel.cancelRun()
                }) { Text("End run") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text("Keep going") }
            },
        )
    }

    // Stretch phases (warm-up/cool-down) read as calm, same as recovery — walk is also calm;
    // only an active run rep gets the "effort" orange.
    val isEffort = state.currentStep?.stepType == StepType.RUN
    val screenColor by animateColorAsState(
        targetValue = if (isEffort) extendedColors.action else extendedColors.recovery,
        animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.StandardEasing),
        label = "runScreenColor",
    )

    Box(modifier = Modifier.fillMaxSize().background(screenColor)) {
        if (state.isLoading) {
            Text("Loading session…", modifier = Modifier.align(Alignment.Center), color = Color.White)
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            ) {
                // Only outdoor sessions have a track/weather to show — treadmill stays on Run.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isOutdoor) {
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
                // Discoverable even on gesture-nav devices where Back isn't an obvious affordance.
                Text(
                    "End run",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = .7f),
                    modifier = Modifier
                        .clickable { showEndConfirm = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                when (tab) {
                    RunTab.RUN -> RunTabContent(state, viewModel)
                    RunTab.MAP -> MapTabContent(state)
                    RunTab.WEATHER -> WeatherTabContent(state)
                }
            }
        }
    }
}

/** Buttons on this screen sit on a full-bleed colored background (orange or teal), not the
 * app's surface — the default OutlinedButton content color is `colorScheme.primary`, which
 * *is* that orange, so it vanishes against it. Every button here is explicitly white. */
@Composable
private fun WhiteOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .7f)),
        content = { content() },
    )
}

@Composable
private fun RunTabContent(state: ActiveRunUiState, viewModel: ActiveRunViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Step ${state.currentIndex + 1} of ${state.steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            IntervalStrip(steps = state.steps, currentIndex = state.currentIndex)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RunnerFigure(
                mode = runnerModeFor(state.currentStep?.stepType),
                modifier = Modifier.size(width = 90.dp, height = 110.dp),
            )
            Text(stepLabel(state.currentStep), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                formatCountdown(state.remainingSeconds),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            state.nextStep?.let {
                Text(
                    "Next: ${stepLabel(it)} ${formatCountdown(it.durationSeconds ?: 0)}",
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
                WhiteOutlinedButton(onClick = viewModel.musicController::previous, modifier = Modifier.weight(1f)) {
                    Text("⏮")
                }
                WhiteOutlinedButton(onClick = viewModel.musicController::playPause, modifier = Modifier.weight(1f)) {
                    Text("⏯")
                }
                WhiteOutlinedButton(onClick = viewModel.musicController::next, modifier = Modifier.weight(1f)) {
                    Text("⏭")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WhiteOutlinedButton(onClick = viewModel::togglePause, modifier = Modifier.weight(1f)) {
                    Text(if (state.isPaused) "Resume" else "Pause")
                }
                WhiteOutlinedButton(onClick = viewModel::skipStep, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
            }
        }
    }
}

/** One segment per step in the session — filled for completed/current, dim for upcoming.
 * The piece from the original wireframes that the real screen was missing. */
@Composable
private fun IntervalStrip(steps: List<com.stride.core.database.entity.SessionStepEntity>, currentIndex: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { index, _ ->
            val isCurrent = index == currentIndex
            val isDone = index < currentIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isCurrent) 8.dp else 6.dp)
                    .background(
                        color = if (isDone || isCurrent) Color.White else Color.White.copy(alpha = .3f),
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
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
                TrackMapView(
                    points = state.trackPoints,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    "MapLibre demo tiles — self-hosted Protomaps is the production plan (docs/foundation.md)",
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = Color.White,
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

private fun runnerModeFor(type: StepType?): RunnerMode = when (type) {
    StepType.RUN -> RunnerMode.RUN
    StepType.STRETCH -> RunnerMode.STRETCH
    StepType.WALK, StepType.REST, null -> RunnerMode.WALK
}

private fun stepLabel(step: com.stride.core.database.entity.SessionStepEntity?): String = when (step?.stepType) {
    StepType.WALK -> "Walk"
    StepType.RUN -> "Run"
    StepType.REST -> "Rest"
    StepType.STRETCH -> step.label ?: "Stretch"
    null -> ""
}

private fun formatCountdown(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
