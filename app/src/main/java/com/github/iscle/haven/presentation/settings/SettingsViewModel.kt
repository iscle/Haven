package com.github.iscle.haven.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.usecase.GetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherApiKeyUseCase
import com.github.iscle.haven.domain.usecase.SetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.SetCityNameUseCase
import com.github.iscle.haven.domain.usecase.SetWeatherApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val backgroundIntervalSeconds: Int = 10,
    val cityName: String = "Barcelona",
    val weatherApiKey: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getBackgroundIntervalUseCase: GetBackgroundIntervalUseCase,
    private val setBackgroundIntervalUseCase: SetBackgroundIntervalUseCase,
    private val getCityNameUseCase: GetCityNameUseCase,
    private val setCityNameUseCase: SetCityNameUseCase,
    private val getWeatherApiKeyUseCase: GetWeatherApiKeyUseCase,
    private val setWeatherApiKeyUseCase: SetWeatherApiKeyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("SettingsViewModel: Initializing")
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            Timber.d("SettingsViewModel: Loading background interval")
            getBackgroundIntervalUseCase().collect { interval ->
                Timber.d("SettingsViewModel: Background interval loaded: $interval seconds")
                _uiState.update { it.copy(backgroundIntervalSeconds = interval) }
            }
        }

        viewModelScope.launch {
            Timber.d("SettingsViewModel: Loading city name")
            getCityNameUseCase().collect { city ->
                Timber.d("SettingsViewModel: City name loaded: $city")
                _uiState.update { it.copy(cityName = city) }
            }
        }

        viewModelScope.launch {
            Timber.d("SettingsViewModel: Loading weather API key")
            getWeatherApiKeyUseCase().collect { apiKey ->
                Timber.d("SettingsViewModel: Weather API key loaded: ${if (apiKey.isNotBlank()) "***${apiKey.takeLast(4)}" else "empty"}")
                _uiState.update { it.copy(weatherApiKey = apiKey) }
            }
        }
    }

    fun setBackgroundInterval(seconds: Int) {
        Timber.i("SettingsViewModel: Setting background interval: $seconds seconds")
        viewModelScope.launch {
            setBackgroundIntervalUseCase(seconds)
        }
    }

    fun setCityName(city: String) {
        Timber.i("SettingsViewModel: Setting city name: $city")
        viewModelScope.launch {
            setCityNameUseCase(city)
        }
    }

    fun setWeatherApiKey(apiKey: String) {
        Timber.i("SettingsViewModel: Setting weather API key: ${if (apiKey.isNotBlank()) "***${apiKey.takeLast(4)}" else "empty"}")
        viewModelScope.launch {
            setWeatherApiKeyUseCase(apiKey)
        }
    }
}

