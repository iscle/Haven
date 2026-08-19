package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.remote.WeatherApiService
import com.github.iscle.haven.data.remote.WeatherCodeMapper
import com.github.iscle.haven.data.remote.model.OpenMeteoWeatherResponse
import com.github.iscle.haven.domain.model.HourlyForecast
import com.github.iscle.haven.domain.model.Weather
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.github.iscle.haven.domain.repository.WeatherRepository as WeatherRepositoryInterface

/** Steps of the forecast strip on the home screen: +3h, +6h, +9h, +12h. */
private val FORECAST_STEPS_HOURS = listOf(3, 6, 9, 12)

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
) : WeatherRepositoryInterface {
    override suspend fun getCurrentWeather(lat: Double, lon: Double, cityName: String): Result<Weather> {
        Timber.d("Repository: Getting weather by coordinates: lat=$lat, lon=$lon")
        return weatherApiService.getCurrentWeather(lat, lon).mapCatching { response ->
            val currentWeather = response.currentWeather
            // Index of the hour we are currently in, so humidity and "feels like"
            // describe now instead of the first hour of the day.
            val nowIndex = currentHourIndex(response)
            val hourly = response.hourly

            val humidity = hourly?.relativeHumidity2m?.getOrNull(nowIndex)
                ?: hourly?.relativeHumidity2m?.firstOrNull()
                ?: 0
            val feelsLike = hourly?.apparentTemperature?.getOrNull(nowIndex)
                ?: hourly?.apparentTemperature?.firstOrNull()
                ?: currentWeather.temperature

            val weather = Weather(
                temperature = currentWeather.temperature,
                feelsLike = feelsLike,
                description = WeatherCodeMapper.getDescription(currentWeather.weatherCode),
                icon = WeatherCodeMapper.getIcon(currentWeather.weatherCode),
                humidity = humidity,
                cityName = cityName.ifBlank { "${lat.toInt()}, ${lon.toInt()}" },
                weatherCode = currentWeather.weatherCode,
                isDay = currentWeather.isDay?.let { it == 1 }
                    ?: (hourly?.isDay?.getOrNull(nowIndex)?.let { it == 1 } ?: true),
                forecast = buildForecast(response, nowIndex),
                sunriseMillis = response.daily?.sunrise?.firstOrNull()?.let(::parseLocalTime),
                sunsetMillis = response.daily?.sunset?.firstOrNull()?.let(::parseLocalTime)
            )
            Timber.d("Repository: Successfully retrieved weather: city=${weather.cityName}, temp=${weather.temperature}°C, forecast=${weather.forecast.size} steps")
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

    /**
     * Open-Meteo returns `current_weather.time` in the same timezone as the hourly
     * series, so matching on the "yyyy-MM-ddTHH" prefix locates the current hour
     * without any timezone arithmetic.
     */
    private fun currentHourIndex(response: OpenMeteoWeatherResponse): Int {
        val times = response.hourly?.time ?: return 0
        val currentTime = response.currentWeather.time
        if (currentTime.length < 13) return 0
        val hourPrefix = currentTime.substring(0, 13)
        val index = times.indexOfFirst { it.startsWith(hourPrefix) }
        return if (index >= 0) index else 0
    }

    /** Open-Meteo returns local times like "2026-08-19T20:47" when timezone=auto. */
    private fun parseLocalTime(value: String): Long? =
        runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(value)?.time }
            .getOrNull()

    private fun buildForecast(response: OpenMeteoWeatherResponse, nowIndex: Int): List<HourlyForecast> {
        val hourly = response.hourly ?: return emptyList()
        val times = hourly.time ?: return emptyList()
        val temperatures = hourly.temperature2m ?: return emptyList()
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)

        return FORECAST_STEPS_HOURS.mapNotNull { step ->
            val index = nowIndex + step
            val time = times.getOrNull(index) ?: return@mapNotNull null
            val temperature = temperatures.getOrNull(index) ?: return@mapNotNull null
            val millis = runCatching { parser.parse(time)?.time }.getOrNull() ?: return@mapNotNull null
            HourlyForecast(
                timeMillis = millis,
                temperature = temperature,
                weatherCode = hourly.weatherCode?.getOrNull(index) ?: 0,
                isDay = hourly.isDay?.getOrNull(index)?.let { it == 1 } ?: true
            )
        }
    }
}
