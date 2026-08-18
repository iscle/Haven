package com.github.iscle.haven.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.InstalledApp
import com.github.iscle.haven.domain.model.TemperatureUnit
import com.github.iscle.haven.domain.usecase.GetAppPreferencesUseCase
import com.github.iscle.haven.domain.usecase.GetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetInstalledAppsUseCase
import com.github.iscle.haven.domain.usecase.SetAdaptiveBrightnessUseCase
import com.github.iscle.haven.domain.usecase.SetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.SetCityNameUseCase
import com.github.iscle.haven.domain.usecase.SetHideNotificationContentUseCase
import com.github.iscle.haven.domain.usecase.SetNightModeEnabledUseCase
import com.github.iscle.haven.domain.usecase.SetNotificationPackageAllowedUseCase
import com.github.iscle.haven.domain.usecase.SetShowPhotographerCreditUseCase
import com.github.iscle.haven.domain.usecase.SetShowSecondsUseCase
import com.github.iscle.haven.domain.usecase.SetTemperatureUnitUseCase
import com.github.iscle.haven.domain.usecase.SetUse24HourClockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val backgroundIntervalSeconds: Int = 10,
    val cityName: String = "Barcelona",
    val preferences: AppPreferences = AppPreferences(),
    val apps: List<InstalledApp> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getBackgroundIntervalUseCase: GetBackgroundIntervalUseCase,
    private val setBackgroundIntervalUseCase: SetBackgroundIntervalUseCase,
    private val getCityNameUseCase: GetCityNameUseCase,
    private val setCityNameUseCase: SetCityNameUseCase,
    private val getAppPreferencesUseCase: GetAppPreferencesUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val setNightModeEnabledUseCase: SetNightModeEnabledUseCase,
    private val setAdaptiveBrightnessUseCase: SetAdaptiveBrightnessUseCase,
    private val setShowPhotographerCreditUseCase: SetShowPhotographerCreditUseCase,
    private val setTemperatureUnitUseCase: SetTemperatureUnitUseCase,
    private val setUse24HourClockUseCase: SetUse24HourClockUseCase,
    private val setShowSecondsUseCase: SetShowSecondsUseCase,
    private val setHideNotificationContentUseCase: SetHideNotificationContentUseCase,
    private val setNotificationPackageAllowedUseCase: SetNotificationPackageAllowedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Timber.d("SettingsViewModel: Initializing")
        loadSettings()
        loadApps()
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
            getAppPreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
            }
        }
    }

    /** The notification pane needs the installed apps to build its allow-list. */
    private fun loadApps() {
        viewModelScope.launch {
            val apps = getInstalledAppsUseCase()
            Timber.d("SettingsViewModel: Loaded ${apps.size} apps for the allow-list")
            _uiState.update { it.copy(apps = apps) }
        }
    }

    fun setBackgroundInterval(seconds: Int) {
        Timber.i("SettingsViewModel: Setting background interval: $seconds seconds")
        viewModelScope.launch { setBackgroundIntervalUseCase(seconds) }
    }

    fun setCityName(city: String) {
        Timber.i("SettingsViewModel: Setting city name: $city")
        viewModelScope.launch { setCityNameUseCase(city) }
    }

    fun setNightModeEnabled(enabled: Boolean) {
        viewModelScope.launch { setNightModeEnabledUseCase(enabled) }
    }

    fun setAdaptiveBrightness(enabled: Boolean) {
        viewModelScope.launch { setAdaptiveBrightnessUseCase(enabled) }
    }

    fun setShowPhotographerCredit(enabled: Boolean) {
        viewModelScope.launch { setShowPhotographerCreditUseCase(enabled) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { setTemperatureUnitUseCase(unit) }
    }

    fun setUse24HourClock(enabled: Boolean) {
        viewModelScope.launch { setUse24HourClockUseCase(enabled) }
    }

    fun setShowSeconds(enabled: Boolean) {
        viewModelScope.launch { setShowSecondsUseCase(enabled) }
    }

    fun setHideNotificationContent(enabled: Boolean) {
        viewModelScope.launch { setHideNotificationContentUseCase(enabled) }
    }

    fun setNotificationPackageAllowed(packageName: String, allowed: Boolean) {
        viewModelScope.launch { setNotificationPackageAllowedUseCase(packageName, allowed) }
    }
}
