package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.core.data.repository.PlanRepository
import com.stride.core.data.repository.RunRepository
import com.stride.core.data.scheduler.AdaptiveScheduler
import com.stride.core.database.entity.PlanEntity
import com.stride.core.database.entity.PlanSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val activePlan: PlanEntity? = null,
    val todaySession: PlanSessionEntity? = null,
    val completedRunCount: Int = 0,
    /** Backs the dev-only "Run summary (demo)" preview link — null when no run has been
     * recorded yet, since Room ids are 1-indexed and a hardcoded 0 would never match a real
     * row (that link used to be permanently dead for exactly that reason). */
    val latestRunId: Long? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    runRepository: RunRepository,
) : ViewModel() {

    private val _rescheduleMessage = MutableStateFlow<String?>(null)

    /** Non-null right after reconcileMissedSessions() actually moved something — "the runner is
     * never shown a failed plan, only a recalculated one" only holds if they're told it was
     * recalculated, not just silently moved. See HomeScreen for how this gets dismissed. */
    val rescheduleMessage: StateFlow<String?> = _rescheduleMessage.asStateFlow()

    init {
        // Once per ViewModel (process) lifetime, not tied to uiState's reactive recomputation —
        // reconciling on every plan/session emission would re-run it far more than intended.
        viewModelScope.launch {
            val plan = planRepository.observeActivePlan().first() ?: return@launch
            when (planRepository.reconcileMissedSessions(plan.id)) {
                AdaptiveScheduler.Strategy.COMPRESS ->
                    _rescheduleMessage.value = "We moved a missed session to fit later this week."
                AdaptiveScheduler.Strategy.SHIFT ->
                    _rescheduleMessage.value = "Your plan shifted forward a bit to make room for a missed session."
                AdaptiveScheduler.Strategy.REGRESS ->
                    _rescheduleMessage.value = "It's been a while — we eased your plan back so you can pick up comfortably."
                null -> Unit // nothing was missed, nothing to tell the runner
            }
        }
    }

    fun dismissRescheduleMessage() {
        _rescheduleMessage.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest — stable in practice, still marked experimental upstream
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
                    latestRunId = runs.maxByOrNull { it.startedAt }?.id,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}
