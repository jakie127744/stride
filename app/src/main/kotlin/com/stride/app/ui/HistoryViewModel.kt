package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.core.data.repository.RunRepository
import com.stride.core.data.repository.ShoeRepository
import com.stride.core.database.entity.DEFAULT_SHOE_RETIREMENT_METERS
import com.stride.core.database.entity.RunSessionEntity
import com.stride.core.database.entity.ShoeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val runs: List<RunSessionEntity> = emptyList(),
    val shoes: List<ShoeEntity> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val shoeRepository: ShoeRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        runRepository.observeRuns(),
        shoeRepository.observeAllShoes(),
    ) { runs, shoes ->
        HistoryUiState(isLoading = false, runs = runs, shoes = shoes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun addShoe(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { shoeRepository.addShoe(name.trim(), DEFAULT_SHOE_RETIREMENT_METERS) }
    }

    fun retireShoe(shoeId: Long) {
        viewModelScope.launch { shoeRepository.retireShoe(shoeId) }
    }
}
