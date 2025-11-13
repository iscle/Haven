package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.remote.WeatherApiService
import com.github.iscle.haven.data.remote.model.WeatherResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApiService: WeatherApiService
) {
    suspend fun getCurrentWeather(apiKey: String, lat: Double, lon: Double): Result<WeatherResponse> {
        Timber.d("Repository: Getting weather by coordinates: lat=$lat, lon=$lon")
        return weatherApiService.getCurrentWeather(apiKey, lat, lon).also { result ->
            result.fold(
                onSuccess = { weather -> Timber.d("Repository: Successfully retrieved weather: city=${weather.name}") },
                onFailure = { error -> Timber.e(error, "Repository: Failed to get weather by coordinates") }
            )
        }
    }

    suspend fun getCurrentWeatherByCity(apiKey: String, city: String): Result<WeatherResponse> {
        Timber.d("Repository: Getting weather by city: city=$city")
        return weatherApiService.getCurrentWeatherByCity(apiKey, city).also { result ->
            result.fold(
                onSuccess = { weather -> Timber.d("Repository: Successfully retrieved weather: city=${weather.name}") },
                onFailure = { error -> Timber.e(error, "Repository: Failed to get weather by city") }
            )
        }
    }
}

