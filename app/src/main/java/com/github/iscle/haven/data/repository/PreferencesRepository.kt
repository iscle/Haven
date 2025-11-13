package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) {
    val backgroundInterval: Flow<Int> = preferencesDataSource.backgroundInterval
    val cityName: Flow<String> = preferencesDataSource.cityName
    val location: Flow<Pair<Double?, Double?>> = preferencesDataSource.location
    val unsplashApiKey: Flow<String> = preferencesDataSource.unsplashApiKey
    val weatherApiKey: Flow<String> = preferencesDataSource.weatherApiKey

    suspend fun setBackgroundInterval(seconds: Int) {
        preferencesDataSource.setBackgroundInterval(seconds)
    }

    suspend fun setCityName(city: String) {
        preferencesDataSource.setCityName(city)
    }

    suspend fun setLocation(latitude: Double, longitude: Double) {
        preferencesDataSource.setLocation(latitude, longitude)
    }

    suspend fun setUnsplashApiKey(apiKey: String) {
        preferencesDataSource.setUnsplashApiKey(apiKey)
    }

    suspend fun setWeatherApiKey(apiKey: String) {
        preferencesDataSource.setWeatherApiKey(apiKey)
    }
}

