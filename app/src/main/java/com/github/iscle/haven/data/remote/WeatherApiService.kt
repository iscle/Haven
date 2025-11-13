package com.github.iscle.haven.data.remote

import com.github.iscle.haven.data.remote.model.WeatherResponse
import com.github.iscle.haven.di.WeatherClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherApiService @Inject constructor(
    @WeatherClient private val client: HttpClient
) {
    suspend fun getCurrentWeather(
        apiKey: String,
        lat: Double,
        lon: Double,
        units: String = "metric"
    ): Result<WeatherResponse> {
        return try {
            if (apiKey.isBlank()) {
                Timber.w("Weather API key is not set")
                return Result.failure(IllegalArgumentException("Weather API key is not set. Please configure it in Settings."))
            }
            
            Timber.d("Fetching weather by coordinates: lat=$lat, lon=$lon, units=$units")
            val weather = client.get("weather") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("units", units)
                parameter("appid", apiKey)
            }.body<WeatherResponse>()
            Timber.d("Weather fetched successfully: city=${weather.name}, temp=${weather.main.temp}°C, condition=${weather.weather.firstOrNull()?.description}")
            Result.success(weather)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch weather by coordinates: lat=$lat, lon=$lon")
            Result.failure(e)
        }
    }

    suspend fun getCurrentWeatherByCity(
        apiKey: String,
        city: String,
        units: String = "metric"
    ): Result<WeatherResponse> {
        return try {
            if (apiKey.isBlank()) {
                Timber.w("Weather API key is not set")
                return Result.failure(IllegalArgumentException("Weather API key is not set. Please configure it in Settings."))
            }
            
            Timber.d("Fetching weather by city: city=$city, units=$units")
            val weather = client.get("weather") {
                parameter("q", city)
                parameter("units", units)
                parameter("appid", apiKey)
            }.body<WeatherResponse>()
            Timber.d("Weather fetched successfully: city=${weather.name}, temp=${weather.main.temp}°C, condition=${weather.weather.firstOrNull()?.description}")
            Result.success(weather)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch weather by city: city=$city")
            Result.failure(e)
        }
    }
}

