package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.app.location.LocationTracker
import com.stride.app.location.TrackPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class LiveTrackUiState(
    val point: TrackPoint? = null,
    val hasPermission: Boolean = true,
)

/**
 * Honest scope: this shares a snapshot of your current location via the system share sheet —
 * a real, working thing a runner can send a contact right now. The wireframed "Live Track"
 * (a persistent, auto-expiring link a contact can watch update in real time) needs a backend
 * to host and push those updates; that's real Phase 5 infrastructure work, not something a
 * client-only app can do on its own. Not pretending otherwise here.
 */
@HiltViewModel
class LiveTrackViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveTrackUiState())
    val state: StateFlow<LiveTrackUiState> = _state.asStateFlow()

    fun startWatching() {
        locationTracker.observeLocationUpdates()
            .onEach { point -> _state.value = _state.value.copy(point = point) }
            .launchIn(viewModelScope)
    }

    fun shareText(): String {
        val point = _state.value.point ?: return "Sharing my run location — waiting for GPS…"
        return "I'm running here right now: https://maps.google.com/?q=${point.latitude},${point.longitude}"
    }
}
