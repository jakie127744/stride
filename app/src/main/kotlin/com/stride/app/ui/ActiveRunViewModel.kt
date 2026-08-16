package com.stride.app.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.stride.app.audio.MusicController
import com.stride.app.navigation.Destination
import com.stride.app.run.ActiveRunUiState
import com.stride.app.run.RunSessionEngine
import com.stride.app.run.RunSessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * A thin adapter over [RunSessionEngine] now — the engine owns the actual state/logic in its
 * own process-scoped coroutine, so it (and [RunSessionService], which keeps the process alive)
 * survive this ViewModel being torn down when the app backgrounds. See RunSessionEngine's doc
 * for why that split exists.
 */
@HiltViewModel
class ActiveRunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val engine: RunSessionEngine,
    val musicController: MusicController,
) : ViewModel() {

    private val route: Destination.ActiveRun = savedStateHandle.toRoute()

    val state: StateFlow<ActiveRunUiState> = engine.state

    fun start() {
        RunSessionService.start(context)
        engine.start(route.planSessionId, route.outdoor, route.shoeId)
    }

    fun togglePause() = engine.togglePause()
    fun skipStep() = engine.skipStep()

    /** Abandon the session early — stops the foreground service and the engine's jobs together,
     * so backing out mid-run doesn't leave GPS/notification running in the background forever. */
    fun cancelRun() {
        engine.cancel()
        RunSessionService.stop(context)
    }
}
