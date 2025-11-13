package com.github.iscle.haven.domain.model

data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val cityName: String
)

