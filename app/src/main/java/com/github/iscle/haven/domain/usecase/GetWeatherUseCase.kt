package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.data.repository.WeatherRepository
import com.github.iscle.haven.domain.model.Weather
import timber.log.Timber
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(apiKey: String, city: String): Result<Weather> {
        Timber.d("UseCase: Getting weather for city: $city")
        return weatherRepository.getCurrentWeatherByCity(apiKey, city).mapCatching { response ->
            val weather = Weather(
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                description = response.weather.firstOrNull()?.description ?: "",
                icon = response.weather.firstOrNull()?.icon ?: "",
                humidity = response.main.humidity,
                cityName = response.name
            )
            Timber.d("UseCase: Successfully created weather: city=${weather.cityName}, temp=${weather.temperature}°C")
            weather
        }.onFailure { error ->
            Timber.e(error, "UseCase: Failed to get weather for city: $city")
        }
    }

    suspend operator fun invoke(apiKey: String, lat: Double, lon: Double): Result<Weather> {
        Timber.d("UseCase: Getting weather for coordinates: lat=$lat, lon=$lon")
        return weatherRepository.getCurrentWeather(apiKey, lat, lon).mapCatching { response ->
            val weather = Weather(
                temperature = response.main.temp,
                feelsLike = response.main.feelsLike,
                description = response.weather.firstOrNull()?.description ?: "",
                icon = response.weather.firstOrNull()?.icon ?: "",
                humidity = response.main.humidity,
                cityName = response.name
            )
            Timber.d("UseCase: Successfully created weather: city=${weather.cityName}, temp=${weather.temperature}°C")
            weather
        }.onFailure { error ->
            Timber.e(error, "UseCase: Failed to get weather for coordinates: lat=$lat, lon=$lon")
        }
    }
}

