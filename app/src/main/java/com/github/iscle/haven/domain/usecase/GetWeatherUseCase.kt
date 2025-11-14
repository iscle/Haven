package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.WeatherRepository
import com.github.iscle.haven.domain.model.Weather
import timber.log.Timber
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(city: String): Result<Weather> {
        Timber.d("UseCase: Getting weather for city: $city")
        return weatherRepository.getCurrentWeatherByCity(city).onFailure { error ->
            Timber.e(error, "UseCase: Failed to get weather for city: $city")
        }
    }

    suspend operator fun invoke(lat: Double, lon: Double, cityName: String = ""): Result<Weather> {
        Timber.d("UseCase: Getting weather for coordinates: lat=$lat, lon=$lon")
        return weatherRepository.getCurrentWeather(lat, lon, cityName).onFailure { error ->
            Timber.e(error, "UseCase: Failed to get weather for coordinates: lat=$lat, lon=$lon")
        }
    }
}

