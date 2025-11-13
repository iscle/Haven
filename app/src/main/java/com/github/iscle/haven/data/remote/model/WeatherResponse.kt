package com.github.iscle.haven.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val coord: Coordinates? = null,
    val weather: List<Weather>,
    val main: MainWeatherData,
    val wind: Wind? = null,
    val clouds: Clouds? = null,
    val dt: Long,
    val sys: Sys? = null,
    val timezone: Int? = null,
    val id: Int,
    val name: String
)

@Serializable
data class Coordinates(
    val lon: Double,
    val lat: Double
)

@Serializable
data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

@Serializable
data class MainWeatherData(
    val temp: Double,
    @SerialName("feels_like")
    val feelsLike: Double,
    @SerialName("temp_min")
    val tempMin: Double,
    @SerialName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int
)

@Serializable
data class Wind(
    val speed: Double,
    val deg: Int? = null
)

@Serializable
data class Clouds(
    val all: Int
)

@Serializable
data class Sys(
    val country: String? = null,
    val sunrise: Long? = null,
    val sunset: Long? = null
)

