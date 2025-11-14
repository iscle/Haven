package com.github.iscle.haven.data.remote

/**
 * Maps WMO Weather interpretation codes (WW) to human-readable descriptions
 * Based on: https://open-meteo.com/en/docs
 */
object WeatherCodeMapper {
    fun getDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing rime fog"
            51 -> "Light drizzle"
            53 -> "Moderate drizzle"
            55 -> "Dense drizzle"
            56 -> "Light freezing drizzle"
            57 -> "Dense freezing drizzle"
            61 -> "Slight rain"
            63 -> "Moderate rain"
            65 -> "Heavy rain"
            66 -> "Light freezing rain"
            67 -> "Heavy freezing rain"
            71 -> "Slight snow fall"
            73 -> "Moderate snow fall"
            75 -> "Heavy snow fall"
            77 -> "Snow grains"
            80 -> "Slight rain showers"
            81 -> "Moderate rain showers"
            82 -> "Violent rain showers"
            85 -> "Slight snow showers"
            86 -> "Heavy snow showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with slight hail"
            99 -> "Thunderstorm with heavy hail"
            else -> "Unknown"
        }
    }
    
    fun getIcon(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "01d" // Clear sky
            1, 2 -> "02d" // Partly cloudy
            3 -> "04d" // Overcast
            45, 48 -> "50d" // Fog
            51, 53, 55, 56, 57 -> "09d" // Drizzle
            61, 63, 65, 66, 67 -> "10d" // Rain
            71, 73, 75, 77 -> "13d" // Snow
            80, 81, 82 -> "09d" // Rain showers
            85, 86 -> "13d" // Snow showers
            95, 96, 99 -> "11d" // Thunderstorm
            else -> "01d"
        }
    }
}

