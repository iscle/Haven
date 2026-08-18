package com.github.iscle.haven.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.InstalledApp
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.domain.usecase.GetAppPreferencesUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetInstalledAppsUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Apps per page in the drawer grid: 6 columns x 2 rows. */
const val APPS_PER_PAGE = 12

data class AppDrawerUiState(
    val apps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val query: String = "",
    val weather: Weather? = null,
    val preferences: AppPreferences = AppPreferences()
) {
    val results: List<InstalledApp>
        get() = if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        }

    val pages: List<List<InstalledApp>>
        get() = apps.chunked(APPS_PER_PAGE)
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val getAppPreferencesUseCase: GetAppPreferencesUseCase,
    private val getCityNameUseCase: GetCityNameUseCase,
    private val getWeatherUseCase: GetWeatherUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        Timber.d("AppDrawerViewModel: Initializing")
        loadApps()
        observePreferences()
        loadWeather()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = getInstalledAppsUseCase()
            Timber.i("AppDrawerViewModel: Loaded ${apps.size} apps")
            _uiState.update { it.copy(apps = apps, isLoading = false) }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            getAppPreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
            }
        }
    }

    /** The header carries the clock and the current temperature, as on the home screen. */
    private fun loadWeather() {
        viewModelScope.launch {
            val city = getCityNameUseCase().first()
            if (city.isBlank()) return@launch
            getWeatherUseCase(city).fold(
                onSuccess = { weather -> _uiState.update { it.copy(weather = weather) } },
                onFailure = { error -> Timber.w(error, "AppDrawerViewModel: No weather for the header") }
            )
        }
    }

    fun startSearch() {
        _uiState.update { it.copy(isSearching = true) }
    }

    fun stopSearch() {
        _uiState.update { it.copy(isSearching = false, query = "") }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
