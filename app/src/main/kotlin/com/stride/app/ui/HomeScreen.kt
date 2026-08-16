package com.stride.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stride.core.designsystem.StrideMotion
import com.stride.core.designsystem.StrideThemeExtras

/**
 * Real screen (not a placeholder) — proves the Hilt → repository → Room chain works end to end.
 * The "today card" cross-fades between the action and recovery tones via [animateColorAsState]
 * depending on whether there's a session scheduled today, exactly the pattern specced in
 * docs/foundation.md for effort/recovery state changes — [StrideMotion.DURATION_MEDIUM] keeps it
 * consistent with every other transition in the app rather than a one-off duration.
 */
@Composable
fun HomeScreen(
    onStartSession: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenInsights: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extendedColors = StrideThemeExtras.extendedColors

    val hasSessionToday = uiState.todaySession != null
    val cardColor by animateColorAsState(
        targetValue = if (hasSessionToday) extendedColors.action else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.StandardEasing),
        label = "todayCardColor",
    )
    val cardContentColor by animateColorAsState(
        targetValue = if (hasSessionToday) extendedColors.onAction else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.StandardEasing),
        label = "todayCardContentColor",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Stride", style = MaterialTheme.typography.headlineMedium)

        when {
            uiState.isLoading -> Text("Loading…", style = MaterialTheme.typography.bodyMedium)

            uiState.activePlan == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .padding(20.dp),
            ) {
                Text(
                    "No active plan yet — plan creation lands in Phase 6. " +
                        "This screen is already reading live (empty) data from Room via Hilt.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (hasSessionToday) "Today's session" else "No session scheduled today",
                    style = MaterialTheme.typography.labelLarge,
                    color = cardContentColor,
                )
                Text(
                    uiState.todaySession?.sessionType?.name?.replace('_', ' ') ?: "Rest day",
                    style = MaterialTheme.typography.titleLarge,
                    color = cardContentColor,
                )
                if (hasSessionToday) {
                    Button(onClick = onStartSession) { Text("Start Session") }
                }
            }
        }

        Text(
            "${uiState.completedRunCount} runs logged",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("History") }
            Button(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) { Text("Insights") }
        }
    }
}
