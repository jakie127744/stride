package com.stride.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.core.database.entity.StepType
import com.stride.core.designsystem.StrideMotion
import com.stride.core.designsystem.StrideThemeExtras

@Composable
fun ActiveRunScreen(
    onFinished: (runId: Long) -> Unit,
    viewModel: ActiveRunViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val extendedColors = StrideThemeExtras.extendedColors

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

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
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
