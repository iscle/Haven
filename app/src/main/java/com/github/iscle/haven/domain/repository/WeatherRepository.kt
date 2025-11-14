package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.Weather

interface WeatherRepository {
    suspend fun getCurrentWeather(lat: Double, lon: Double, cityName: String = ""): Result<Weather>
    suspend fun getCurrentWeatherByCity(city: String): Result<Weather>
}

