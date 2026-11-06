package com.github.iscle.haven.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.HavenNotification
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.domain.usecase.GetAllowedNotificationsUseCase
import com.github.iscle.haven.domain.usecase.GetAppPreferencesUseCase
import com.github.iscle.haven.domain.usecase.GetBackgroundIntervalUseCase
import com.github.iscle.haven.domain.usecase.GetCityNameUseCase
import com.github.iscle.haven.domain.usecase.GetPinnedImageUseCase
import com.github.iscle.haven.domain.usecase.GetRandomBackgroundImageUseCase
import com.github.iscle.haven.domain.usecase.GetWeatherUseCase
import com.github.iscle.haven.domain.usecase.SetPinnedImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val backgroundRequestId: Long = 0,
    val weather: Weather? = null,
    val isLoadingBackground: Boolean = false,
    val isLoadingWeather: Boolean = false,
    val errorMessage: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val notifications: List<HavenNotification> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRandomBackgroundImageUseCase: GetRandomBackgroundImageUseCase,
    private val getWeatherUseCase: GetWeatherUseCase,
    private val getBackgroundIntervalUseCase: GetBackgroundIntervalUseCase,
    private val getCityNameUseCase: GetCityNameUseCase,
    private val getAppPreferencesUseCase: GetAppPreferencesUseCase,
    private val getAllowedNotificationsUseCase: GetAllowedNotificationsUseCase,
    private val getPinnedImageUseCase: GetPinnedImageUseCase,
    private val setPinnedImageUseCase: SetPinnedImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var backgroundJob: Job? = null
    private var backgroundLoadJob: Job? = null
    private var backgroundLoadFailed = false
    private var awaitingBackgroundDecode = false
    private var weatherJob: Job? = null
    private var currentIntervalSeconds = 10
    private val weatherRefreshIntervalSeconds = 600L // 10 minutes
    private var currentCity: String = ""

    init {
        Timber.d("HomeViewModel: Initializing")
        observeCityName()
        observePreferences()
        observeNotifications()
        observePinnedImage()
        startBackgroundRotation()
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

    private fun observePreferences() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing display preferences")
            getAppPreferencesUseCase().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing allowed notifications")
            getAllowedNotificationsUseCase().collect { notifications ->
                Timber.d("HomeViewModel: ${notifications.size} allowed notifications on the clock")
                _uiState.update { it.copy(notifications = notifications) }
            }
        }
    }

    /**
     * "Show on clock now" from the history details pane writes a pinned image;
     * show it straight away, restart the rotation so it stays for a full
     * interval, then clear the pin so it is not restored on the next launch.
     */
    private fun observePinnedImage() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing pinned image")
            getPinnedImageUseCase().collect { pinned ->
                if (pinned == null) return@collect
                Timber.i("HomeViewModel: Showing pinned image: id=${pinned.id}")
                backgroundLoadJob?.cancel()
                backgroundLoadFailed = false
                awaitingBackgroundDecode = true
                _uiState.update {
                    it.copy(
                        backgroundImage = pinned,
                        backgroundRequestId = it.backgroundRequestId + 1,
                        isLoadingBackground = true,
                        errorMessage = null
                    )
                }
                startBackgroundRotation()
                setPinnedImageUseCase(null)
            }
        }
    }

    private fun observeBackgroundInterval() {
        viewModelScope.launch {
            Timber.d("HomeViewModel: Observing background interval")
            getBackgroundIntervalUseCase().collect { storedInterval ->
                val intervalSeconds = storedInterval.coerceIn(10, 86_400)
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
                val interval = if (backgroundLoadFailed) {
                    minOf(currentIntervalSeconds, 30)
                } else {
                    currentIntervalSeconds
                }
                delay(interval * 1000L)
                Timber.d("HomeViewModel: Background rotation timer triggered, loading new image")
                loadBackgroundImage()
                backgroundLoadJob?.join()
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
        if (backgroundLoadJob?.isActive == true || awaitingBackgroundDecode) return
        backgroundLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackground = true) }
            try {
                val image = getRandomBackgroundImageUseCase().getOrThrow()
                backgroundLoadFailed = false
                awaitingBackgroundDecode = true
                _uiState.update {
                    it.copy(
                        backgroundImage = image,
                        backgroundRequestId = it.backgroundRequestId + 1,
                        errorMessage = null
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                backgroundLoadFailed = true
                Timber.e(error, "Failed to load background image")
                _uiState.update {
                    it.copy(errorMessage = "Failed to load background: ${error.message}")
                }
                startBackgroundRotation()
            } finally {
                if (!awaitingBackgroundDecode) {
                    _uiState.update { it.copy(isLoadingBackground = false) }
                }
            }
        }
    }

    /** Rotation starts after decoding, so a slow download cannot be replaced mid-flight. */
    fun onBackgroundImageLoaded(image: BackgroundImage, requestId: Long) {
        if (!isCurrentBackground(image, requestId)) return
        awaitingBackgroundDecode = false
        backgroundLoadFailed = false
        _uiState.update { it.copy(isLoadingBackground = false) }
        startBackgroundRotation()
    }

    /** Keep the current photo visible and retry when its replacement cannot be decoded. */
    fun onBackgroundImageError(image: BackgroundImage, requestId: Long) {
        if (!isCurrentBackground(image, requestId)) return
        awaitingBackgroundDecode = false
        backgroundLoadFailed = true
        _uiState.update { it.copy(isLoadingBackground = false) }
        startBackgroundRotation()
    }

    private fun isCurrentBackground(image: BackgroundImage, requestId: Long): Boolean =
        _uiState.value.backgroundImage == image && _uiState.value.backgroundRequestId == requestId

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
