package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.remote.WeatherCodeMapper
import com.github.iscle.haven.data.remote.WeatherApiService
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.domain.repository.WeatherRepository as WeatherRepositoryInterface
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
) : WeatherRepositoryInterface {
    override suspend fun getCurrentWeather(lat: Double, lon: Double, cityName: String): Result<Weather> {
        Timber.d("Repository: Getting weather by coordinates: lat=$lat, lon=$lon")
        return weatherApiService.getCurrentWeather(lat, lon).mapCatching { response ->
            val currentWeather = response.currentWeather
            val humidity = response.hourly?.relativeHumidity2m?.firstOrNull() ?: 0
            val feelsLike = response.hourly?.apparentTemperature?.firstOrNull() ?: currentWeather.temperature
            
            val weather = Weather(
                temperature = currentWeather.temperature,
                feelsLike = feelsLike,
                description = WeatherCodeMapper.getDescription(currentWeather.weatherCode),
                icon = WeatherCodeMapper.getIcon(currentWeather.weatherCode),
                humidity = humidity,
                cityName = cityName.ifBlank { "${lat.toInt()}, ${lon.toInt()}" }
            )
            Timber.d("Repository: Successfully retrieved weather: city=${weather.cityName}, temp=${weather.temperature}°C")
            weather
        }.onFailure { error ->
            Timber.e(error, "Repository: Failed to get weather by coordinates")
        }
    }

    override suspend fun getCurrentWeatherByCity(city: String): Result<Weather> {
        Timber.d("Repository: Getting weather by city: city=$city")
        return weatherApiService.getCoordinatesByCity(city).fold(
            onSuccess = { (lat, lon) ->
                getCurrentWeather(lat, lon, city)
            },
            onFailure = { error ->
                Timber.e(error, "Repository: Failed to get coordinates for city: $city")
                Result.failure(error)
            }
        ).also { result ->
            result.fold(
                onSuccess = { weather -> 
                    Timber.d("Repository: Successfully retrieved weather for city: ${weather.cityName}, temp=${weather.temperature}°C") 
                },
                onFailure = { error -> Timber.e(error, "Repository: Failed to get weather by city") }
            )
        }
    }
}

