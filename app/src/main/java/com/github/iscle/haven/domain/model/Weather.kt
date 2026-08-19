package com.github.iscle.haven.domain.model

data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val cityName: String,
    /** WMO weather interpretation code for the current conditions. */
    val weatherCode: Int = 0,
    /** True while the sun is up at the observed location. */
    val isDay: Boolean = true,
    /** Upcoming hours used by the forecast strip on the home screen. */
    val forecast: List<HourlyForecast> = emptyList(),
    /** Today's sun times, used by the ambient schedule when it follows the sun. */
    val sunriseMillis: Long? = null,
    val sunsetMillis: Long? = null
)

data class HourlyForecast(
    val timeMillis: Long,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Boolean
)
