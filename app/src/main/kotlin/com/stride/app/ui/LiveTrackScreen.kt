package com.stride.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LiveTrackScreen(viewModel: LiveTrackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.startWatching() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Live Track", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Share your current location with someone before you head out. " +
                "A persistent, auto-updating link a contact can watch live is real Phase 5 " +
                "work (it needs a backend to host it) — this shares one honest snapshot now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val point = state.point
        if (point == null) {
            Text("Waiting for a GPS fix…", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "Last fix: %.5f, %.5f".format(point.latitude, point.longitude),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                }
                context.startActivity(Intent.createChooser(intent, "Share location"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Share my location") }
    }
}
