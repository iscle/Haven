package com.github.iscle.haven.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.domain.usecase.GetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetRandomBackgroundImageUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherApiKeyUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val getCityNameUseCase: GetCityNameUseCase,
    private val getWeatherApiKeyUseCase: GetWeatherApiKeyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var backgroundJob: Job? = null
    private var currentIntervalSeconds = 10
    private var weatherApiKey = ""

    init {
        Timber.d("HomeViewModel: Initializing")
        observeWeatherApiKey()
        loadWeather()
        loadBackgroundImage()
        observeBackgroundInterval()
    }

    private fun observeWeatherApiKey() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing weather API key")
            getWeatherApiKeyUseCase().collect { apiKey ->
                val wasEmpty = weatherApiKey.isBlank()
                weatherApiKey = apiKey
                if (wasEmpty && apiKey.isNotBlank()) {
                    Timber.i("HomeViewModel: Weather API key set, reloading weather")
                    loadWeather()
                } else if (apiKey.isBlank()) {
                    Timber.w("HomeViewModel: Weather API key is empty")
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

    fun loadWeather() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Loading weather")
            combine(
                getCityNameUseCase(),
                getWeatherApiKeyUseCase()
            ) { city, apiKey ->
                Pair(city, apiKey)
            }.collect { (city, apiKey) ->
                if (apiKey.isBlank()) {
                    Timber.w("HomeViewModel: Cannot load weather - API key is empty")
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            errorMessage = "Weather API key is not set"
                        )
                    }
                    return@collect
                }
                
                Timber.d("HomeViewModel: Loading weather for city: $city")
                _uiState.update { it.copy(isLoadingWeather = true) }
                
                getWeatherUseCase(apiKey, city).fold(
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
    }

    override fun onCleared() {
        Timber.d("HomeViewModel: Clearing ViewModel, cancelling background job")
        super.onCleared()
        backgroundJob?.cancel()
    }
}

