package com.github.iscle.haven.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current_weather")
    val currentWeather: CurrentWeather,
    @SerialName("hourly")
    val hourly: HourlyWeather? = null
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    @SerialName("windspeed")
    val windSpeed: Double,
    @SerialName("winddirection")
    val windDirection: Double,
    @SerialName("weathercode")
    val weatherCode: Int,
    val time: String,
    @SerialName("is_day")
    val isDay: Int? = null
)

@Serializable
data class HourlyWeather(
    val time: List<String>? = null,
    @SerialName("relativehumidity_2m")
    val relativeHumidity2m: List<Int>? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: List<Double>? = null,
    @SerialName("temperature_2m")
    val temperature2m: List<Double>? = null,
    @SerialName("weathercode")
    val weatherCode: List<Int>? = null,
    @SerialName("is_day")
    val isDay: List<Int>? = null
)
