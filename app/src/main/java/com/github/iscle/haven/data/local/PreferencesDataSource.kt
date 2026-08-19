package com.github.iscle.haven.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.github.iscle.haven.data.local.model.PhotoSourceDto
import com.github.iscle.haven.data.local.model.PinnedImageDto
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.DEFAULT_AMBIENT_END_MINUTES
import com.github.iscle.haven.domain.model.DEFAULT_AMBIENT_START_MINUTES
import com.github.iscle.haven.domain.model.DEFAULT_PHOTO_SOURCES
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.TemperatureUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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

        val NIGHT_MODE_KEY = booleanPreferencesKey("night_mode_enabled")
        val AMBIENT_START_KEY = intPreferencesKey("ambient_start_minutes")
        val AMBIENT_END_KEY = intPreferencesKey("ambient_end_minutes")
        val AMBIENT_FOLLOW_SUN_KEY = booleanPreferencesKey("ambient_follow_sun")
        val PHOTO_SOURCES_KEY = stringPreferencesKey("photo_sources")
        val LAST_PHOTO_SOURCE_KEY = stringPreferencesKey("last_photo_source")
        val ADAPTIVE_BRIGHTNESS_KEY = booleanPreferencesKey("adaptive_brightness")
        val SHOW_CREDIT_KEY = booleanPreferencesKey("show_photographer_credit")
        val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        val USE_24_HOUR_KEY = booleanPreferencesKey("use_24_hour_clock")
        val SHOW_SECONDS_KEY = booleanPreferencesKey("show_seconds")
        val HIDE_NOTIFICATION_CONTENT_KEY = booleanPreferencesKey("hide_notification_content")
        val ALLOWED_NOTIFICATION_PACKAGES_KEY = stringSetPreferencesKey("allowed_notification_packages")
        val PINNED_IMAGE_KEY = stringPreferencesKey("pinned_image")

        const val DEFAULT_INTERVAL_SECONDS = 10
        const val DEFAULT_CITY = "Barcelona"

        private const val UNIT_FAHRENHEIT = "fahrenheit"
        private const val UNIT_CELSIUS = "celsius"
    }

    private val json = Json { ignoreUnknownKeys = true }

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

    val appPreferences: Flow<AppPreferences> = dataStore.data.map { preferences ->
        AppPreferences(
            ambientModeEnabled = preferences[NIGHT_MODE_KEY] ?: true,
            ambientStartMinutes = preferences[AMBIENT_START_KEY] ?: DEFAULT_AMBIENT_START_MINUTES,
            ambientEndMinutes = preferences[AMBIENT_END_KEY] ?: DEFAULT_AMBIENT_END_MINUTES,
            ambientFollowSun = preferences[AMBIENT_FOLLOW_SUN_KEY] ?: false,
            adaptiveBrightness = preferences[ADAPTIVE_BRIGHTNESS_KEY] ?: true,
            showPhotographerCredit = preferences[SHOW_CREDIT_KEY] ?: true,
            temperatureUnit = when (preferences[TEMPERATURE_UNIT_KEY]) {
                UNIT_FAHRENHEIT -> TemperatureUnit.FAHRENHEIT
                else -> TemperatureUnit.CELSIUS
            },
            use24HourClock = preferences[USE_24_HOUR_KEY] ?: true,
            showSeconds = preferences[SHOW_SECONDS_KEY] ?: false,
            hideNotificationContent = preferences[HIDE_NOTIFICATION_CONTENT_KEY] ?: false,
            allowedNotificationPackages = preferences[ALLOWED_NOTIFICATION_PACKAGES_KEY] ?: emptySet()
        )
    }

    val photoSources: Flow<List<PhotoSource>> = dataStore.data.map { preferences ->
        val stored = preferences[PHOTO_SOURCES_KEY]
        if (stored.isNullOrBlank()) {
            DEFAULT_PHOTO_SOURCES
        } else {
            runCatching {
                json.decodeFromString<List<PhotoSourceDto>>(stored).map { it.toDomain() }
            }.onFailure { Timber.w(it, "Failed to read photo sources") }
                .getOrDefault(DEFAULT_PHOTO_SOURCES)
        }
    }

    val lastPhotoSourceId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_PHOTO_SOURCE_KEY]
    }

    val pinnedImage: Flow<BackgroundImage?> = dataStore.data.map { preferences ->
        val stored = preferences[PINNED_IMAGE_KEY] ?: return@map null
        runCatching { json.decodeFromString<PinnedImageDto>(stored).toDomain() }
            .onFailure { Timber.w(it, "Failed to read pinned image") }
            .getOrNull()
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

    suspend fun setAmbientModeEnabled(enabled: Boolean) = putBoolean(NIGHT_MODE_KEY, enabled)

    suspend fun setAmbientFollowSun(enabled: Boolean) = putBoolean(AMBIENT_FOLLOW_SUN_KEY, enabled)

    suspend fun setAmbientSchedule(startMinutes: Int, endMinutes: Int) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting ambient window: $startMinutes -> $endMinutes")
            dataStore.edit { preferences ->
                preferences[AMBIENT_START_KEY] = startMinutes
                preferences[AMBIENT_END_KEY] = endMinutes
            }
        }
    }

    suspend fun setAdaptiveBrightness(enabled: Boolean) = putBoolean(ADAPTIVE_BRIGHTNESS_KEY, enabled)

    suspend fun setShowPhotographerCredit(enabled: Boolean) = putBoolean(SHOW_CREDIT_KEY, enabled)

    suspend fun setUse24HourClock(enabled: Boolean) = putBoolean(USE_24_HOUR_KEY, enabled)

    suspend fun setShowSeconds(enabled: Boolean) = putBoolean(SHOW_SECONDS_KEY, enabled)

    suspend fun setHideNotificationContent(enabled: Boolean) =
        putBoolean(HIDE_NOTIFICATION_CONTENT_KEY, enabled)

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting temperature unit: $unit")
            dataStore.edit { preferences ->
                preferences[TEMPERATURE_UNIT_KEY] = when (unit) {
                    TemperatureUnit.CELSIUS -> UNIT_CELSIUS
                    TemperatureUnit.FAHRENHEIT -> UNIT_FAHRENHEIT
                }
            }
        }
    }

    suspend fun setNotificationPackageAllowed(packageName: String, allowed: Boolean) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting notification allow-list: $packageName allowed=$allowed")
            dataStore.edit { preferences ->
                val current = preferences[ALLOWED_NOTIFICATION_PACKAGES_KEY] ?: emptySet()
                preferences[ALLOWED_NOTIFICATION_PACKAGES_KEY] = if (allowed) {
                    current + packageName
                } else {
                    current - packageName
                }
            }
        }
    }

    suspend fun setPhotoSources(sources: List<PhotoSource>) {
        withContext(Dispatchers.IO) {
            Timber.d("Saving ${sources.size} photo sources")
            dataStore.edit { preferences ->
                preferences[PHOTO_SOURCES_KEY] =
                    json.encodeToString(sources.map(PhotoSourceDto::fromDomain))
            }
        }
    }

    suspend fun setLastPhotoSourceId(sourceId: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[LAST_PHOTO_SOURCE_KEY] = sourceId
            }
        }
    }

    suspend fun setPinnedImage(image: BackgroundImage?) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting pinned image: ${image?.id ?: "none"}")
            dataStore.edit { preferences ->
                if (image == null) {
                    preferences.remove(PINNED_IMAGE_KEY)
                } else {
                    preferences[PINNED_IMAGE_KEY] =
                        json.encodeToString(PinnedImageDto.fromDomain(image))
                }
            }
        }
    }

    private suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        withContext(Dispatchers.IO) {
            Timber.d("Setting ${key.name}: $value")
            dataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }
}
