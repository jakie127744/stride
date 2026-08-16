package com.stride.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.stride.core.designsystem.LocalReducedMotion
import com.stride.core.designsystem.StrideMotion

/**
 * Every real screen replaces one of these as its :feature:* module ships (Phase 4–6) —
 * this exists to prove the nav graph + entrance-motion pattern works end-to-end now, and to
 * give every route a fade+scale-in "arrival" so the app doesn't feel static while screens
 * fill in one by one. Reduced-motion users get a plain instant appearance instead.
 */
@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reducedMotion) {
                fadeIn(tween(StrideMotion.DURATION_SHORT))
            } else {
                fadeIn(tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.EmphasizedEasing)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(StrideMotion.DURATION_MEDIUM, easing = StrideMotion.EmphasizedEasing),
                    )
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable fun HistoryPlaceholderScreen() = PlaceholderScreen("History", "Run history + shoe log — Phase 6")

@Composable fun InsightsPlaceholderScreen() = PlaceholderScreen("Insights", "Performance statistics — Phase 6")

@Composable fun LiveTrackPlaceholderScreen() = PlaceholderScreen("Live Track", "Safety sharing — Phase 5")
