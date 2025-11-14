package com.github.iscle.haven.data.remote

import com.github.iscle.haven.data.remote.model.OpenMeteoGeocodingResponse
import com.github.iscle.haven.data.remote.model.OpenMeteoWeatherResponse
import com.github.iscle.haven.di.GeocodingClient
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
    @WeatherClient private val weatherClient: HttpClient,
    @GeocodingClient private val geocodingClient: HttpClient
) {
    suspend fun getCurrentWeather(
        lat: Double,
        lon: Double
    ): Result<OpenMeteoWeatherResponse> {
        return try {
            Timber.d("Fetching weather by coordinates: lat=$lat, lon=$lon")
            val weather = weatherClient.get("forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current_weather", true)
                parameter("temperature_unit", "celsius")
                parameter("hourly", "relativehumidity_2m,apparent_temperature")
            }.body<OpenMeteoWeatherResponse>()
            Timber.d("Weather fetched successfully: temp=${weather.currentWeather.temperature}°C, condition=${weather.currentWeather.weatherCode}")
            Result.success(weather)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch weather by coordinates: lat=$lat, lon=$lon")
            Result.failure(e)
        }
    }

    suspend fun getCoordinatesByCity(city: String): Result<Pair<Double, Double>> {
        return try {
            Timber.d("Fetching coordinates for city: city=$city")
            val response = geocodingClient.get("search") {
                parameter("name", city)
                parameter("count", 1)
            }.body<OpenMeteoGeocodingResponse>()
            
            val result = response.results?.firstOrNull()
            if (result != null) {
                Timber.d("Coordinates fetched successfully: city=${result.name}, lat=${result.latitude}, lon=${result.longitude}")
                Result.success(Pair(result.latitude, result.longitude))
            } else {
                Timber.w("No coordinates found for city: $city")
                Result.failure(IllegalArgumentException("City not found: $city"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch coordinates for city: city=$city")
            Result.failure(e)
        }
    }
}

