package com.stride.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.stride.app.navigation.Destination
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.RunSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RunSummaryUiState(
    val isLoading: Boolean = true,
    val run: RunSessionEntity? = null,
    val selectedRpe: Int? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class RunSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runRepository: RunRepository,
) : ViewModel() {

    private val runId: Long = savedStateHandle.toRoute<Destination.RunSummary>().runId

    private val _state = MutableStateFlow(RunSummaryUiState())
    val state: StateFlow<RunSummaryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val run = runRepository.getRun(runId)
            _state.value = RunSummaryUiState(isLoading = false, run = run)
        }
    }

    fun selectRpe(value: Int) {
        _state.value = _state.value.copy(selectedRpe = value)
    }

    fun save(onSaved: () -> Unit) {
        val rpe = _state.value.selectedRpe
        viewModelScope.launch {
            if (rpe != null) runRepository.updateRpe(runId, rpe)
            _state.value = _state.value.copy(saved = true)
            onSaved()
        }
    }
}
