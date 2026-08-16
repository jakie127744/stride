package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val activePlan: PlanEntity? = null,
    val todaySession: PlanSessionEntity? = null,
    val completedRunCount: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    planRepository: PlanRepository,
    runRepository: RunRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = planRepository.observeActivePlan()
        .flatMapLatest { plan ->
            val sessions = if (plan != null) {
                planRepository.observeSessionsForPlan(plan.id)
            } else {
                flowOf(emptyList())
            }
            combine(sessions, runRepository.observeRuns()) { planSessions, runs ->
                HomeUiState(
                    isLoading = false,
                    activePlan = plan,
                    todaySession = planSessions.firstOrNull { it.scheduledDate == LocalDate.now() },
                    completedRunCount = runs.size,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}
