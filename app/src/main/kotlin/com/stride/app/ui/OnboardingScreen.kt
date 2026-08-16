package com.stride.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stride.core.common.Track
import com.stride.core.designsystem.StrideThemeExtras

@Composable
fun OnboardingScreen(
    onTrackSelected: (planSessionId: Long) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val extendedColors = StrideThemeExtras.extendedColors

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Which runner are you today?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "You can switch tracks any time — nothing here is permanent.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TrackCard(
            title = "Beginner Track",
            description = "New to running, or coming back after time off. Walk/run intervals, gentle progress.",
            accent = extendedColors.recovery,
            container = extendedColors.recoveryContainer,
            onClick = { viewModel.selectTrack(Track.BEGINNER, onTrackSelected) },
        )
        TrackCard(
            title = "Pro Track",
            description = "Already running. Custom intervals, HR zones, pace targets.",
            accent = extendedColors.action,
            container = extendedColors.actionContainer,
            onClick = { viewModel.selectTrack(Track.PRO, onTrackSelected) },
        )
    }
}

@Composable
private fun TrackCard(
    title: String,
    description: String,
    accent: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = accent)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}
