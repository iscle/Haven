package com.github.iscle.haven.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val BACKGROUND_INTERVAL_KEY = intPreferencesKey("background_interval")
        val CITY_NAME_KEY = stringPreferencesKey("city_name")
        val LATITUDE_KEY = stringPreferencesKey("latitude")
        val LONGITUDE_KEY = stringPreferencesKey("longitude")
        val UNSPLASH_API_KEY = stringPreferencesKey("unsplash_api_key")
        val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
        
        const val DEFAULT_INTERVAL_SECONDS = 10
        const val DEFAULT_CITY = "Barcelona"
    }

    val backgroundInterval: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BACKGROUND_INTERVAL_KEY] ?: DEFAULT_INTERVAL_SECONDS
    }

    val cityName: Flow<String> = dataStore.data.map { preferences ->
        preferences[CITY_NAME_KEY] ?: DEFAULT_CITY
    }

    val location: Flow<Pair<Double?, Double?>> = dataStore.data.map { preferences ->
        val lat = preferences[LATITUDE_KEY]?.toDoubleOrNull()
        val lon = preferences[LONGITUDE_KEY]?.toDoubleOrNull()
        Pair(lat, lon)
    }

    suspend fun setBackgroundInterval(seconds: Int) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting background interval: $seconds seconds")
            dataStore.edit { preferences ->
                preferences[BACKGROUND_INTERVAL_KEY] = seconds
            }
            Timber.d("Background interval saved: $seconds seconds")
        }
    }

    suspend fun setCityName(city: String) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting city name: $city")
            dataStore.edit { preferences ->
                preferences[CITY_NAME_KEY] = city
            }
            Timber.d("City name saved: $city")
        }
    }

    suspend fun setLocation(latitude: Double, longitude: Double) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting location: lat=$latitude, lon=$longitude")
            dataStore.edit { preferences ->
                preferences[LATITUDE_KEY] = latitude.toString()
                preferences[LONGITUDE_KEY] = longitude.toString()
            }
            Timber.d("Location saved: lat=$latitude, lon=$longitude")
        }
    }
}

