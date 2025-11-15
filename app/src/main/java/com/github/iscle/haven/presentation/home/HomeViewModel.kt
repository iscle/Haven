package com.github.iscle.haven.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.domain.usecase.GetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetRandomBackgroundImageUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val backgroundImage: BackgroundImage? = null,
    val weather: Weather? = null,
    val isLoadingBackground: Boolean = false,
    val isLoadingWeather: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRandomBackgroundImageUseCase: GetRandomBackgroundImageUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val getBackgroundIntervalUseCase: GetBackgroundIntervalUseCase,
    private val getCityNameUseCase: GetCityNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var backgroundJob: Job? = null
    private var weatherJob: Job? = null
    private var currentIntervalSeconds = 10
    private val weatherRefreshIntervalSeconds = 600L // 10 minutes
    private var currentCity: String = ""

    init {
        Timber.d("HomeViewModel: Initializing")
        observeCityName()
        loadBackgroundImage()
        observeBackgroundInterval()
        startWeatherRefresh()
    }

    private fun observeCityName() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing city name")
            getCityNameUseCase().collect { city ->
                if (city != currentCity) {
                    Timber.i("HomeViewModel: City name changed: $currentCity -> $city")
                    currentCity = city
                    if (city.isNotBlank()) {
                        loadWeatherForCity(city)
                    }
                }
            }
        }
    }

    private fun observeBackgroundInterval() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing background interval")
            getBackgroundIntervalUseCase().collect { intervalSeconds ->
                if (currentIntervalSeconds != intervalSeconds) {
                    Timber.i("HomeViewModel: Background interval changed: ${currentIntervalSeconds}s -> ${intervalSeconds}s")
                    currentIntervalSeconds = intervalSeconds
                    startBackgroundRotation()
                }
            }
        }
    }

    private fun startBackgroundRotation() {
        backgroundJob?.cancel()
        Timber.d("HomeViewModel: Starting background rotation with interval: ${currentIntervalSeconds}s")
        backgroundJob = viewModelScope.launch {
            while (true) {
                delay(currentIntervalSeconds * 1000L)
                Timber.d("HomeViewModel: Background rotation timer triggered, loading new image")
                loadBackgroundImage()
            }
        }
    }

    private fun startWeatherRefresh() {
        weatherJob?.cancel()
        Timber.d("HomeViewModel: Starting weather refresh with interval: ${weatherRefreshIntervalSeconds}s")
        weatherJob = viewModelScope.launch {
            while (true) {
                delay(weatherRefreshIntervalSeconds * 1000L)
                Timber.d("HomeViewModel: Weather refresh timer triggered, loading new weather")
                if (currentCity.isNotBlank()) {
                    loadWeatherForCity(currentCity)
                }
            }
        }
    }

    fun loadBackgroundImage() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Loading background image")
            _uiState.update { it.copy(isLoadingBackground = true) }
            
            getRandomBackgroundImageUseCase().fold(
                onSuccess = { image ->
                    Timber.i("HomeViewModel: Background image loaded successfully: id=${image.id}, photographer=${image.photographer}")
                    _uiState.update { 
                        it.copy(
                            backgroundImage = image,
                            isLoadingBackground = false,
                            errorMessage = null
                        )
                    }
                    if (backgroundJob == null) {
                        Timber.d("HomeViewModel: Starting background rotation job")
                        startBackgroundRotation()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "HomeViewModel: Failed to load background image")
                    _uiState.update {
                        it.copy(
                            isLoadingBackground = false,
                            errorMessage = "Failed to load background: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private fun loadWeatherForCity(city: String) {
        viewModelScope.launch {
            if (city.isBlank()) {
                Timber.w("HomeViewModel: Cannot load weather - city name is empty")
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        errorMessage = "City name is not set"
                    )
                }
                return@launch
            }
            
            Timber.d("HomeViewModel: Loading weather for city: $city")
            _uiState.update { it.copy(isLoadingWeather = true) }
            
            getWeatherUseCase(city).fold(
                onSuccess = { weather ->
                    Timber.i("HomeViewModel: Weather loaded successfully: city=${weather.cityName}, temp=${weather.temperature}°C")
                    _uiState.update {
                        it.copy(
                            weather = weather,
                            isLoadingWeather = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "HomeViewModel: Failed to load weather for city: $city")
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            errorMessage = "Failed to load weather: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadWeather() {
        if (currentCity.isNotBlank()) {
            loadWeatherForCity(currentCity)
        }
    }

    override fun onCleared() {
        Timber.d("HomeViewModel: Clearing ViewModel, cancelling background and weather jobs")
        super.onCleared()
        backgroundJob?.cancel()
        weatherJob?.cancel()
    }
}

