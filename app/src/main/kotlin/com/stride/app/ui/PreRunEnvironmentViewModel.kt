package com.stride.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stride.app.location.LocationTracker
import com.stride.core.common.WeatherSnapshot
import com.stride.core.data.repository.ShoeRepository
import com.stride.core.database.entity.ShoeEntity
import com.stride.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val isLoading: Boolean = false,
    val snapshot: WeatherSnapshot? = null,
    val unavailable: Boolean = false,
)

@HiltViewModel
class PreRunEnvironmentViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val weatherRepository: WeatherRepository,
    shoeRepository: ShoeRepository,
) : ViewModel() {

    private val _weather = MutableStateFlow(WeatherUiState())
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    val shoes: StateFlow<List<ShoeEntity>> = shoeRepository.observeActiveShoes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Called once location permission is confirmed granted — see PreRunEnvironmentScreen. */
    fun fetchWeather() {
        _weather.value = WeatherUiState(isLoading = true)
        viewModelScope.launch {
            val location = locationTracker.lastKnownLocation()
            val snapshot = location?.let { weatherRepository.fetchCurrentConditions(it.latitude, it.longitude) }
            _weather.value = if (snapshot != null) {
                WeatherUiState(snapshot = snapshot)
            } else {
                WeatherUiState(unavailable = true)
            }
        }
    }
}
