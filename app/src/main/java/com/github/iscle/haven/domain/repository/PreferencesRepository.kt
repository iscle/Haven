package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.TemperatureUnit
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val backgroundInterval: Flow<Int>
    val cityName: Flow<String>
    val location: Flow<Pair<Double?, Double?>>
    val appPreferences: Flow<AppPreferences>
    val pinnedImage: Flow<BackgroundImage?>

    suspend fun setBackgroundInterval(seconds: Int)
    suspend fun setCityName(city: String)
    suspend fun setLocation(latitude: Double, longitude: Double)

    suspend fun setNightModeEnabled(enabled: Boolean)
    suspend fun setAdaptiveBrightness(enabled: Boolean)
    suspend fun setShowPhotographerCredit(enabled: Boolean)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setUse24HourClock(enabled: Boolean)
    suspend fun setShowSeconds(enabled: Boolean)
    suspend fun setHideNotificationContent(enabled: Boolean)
    suspend fun setNotificationPackageAllowed(packageName: String, allowed: Boolean)
    suspend fun setPinnedImage(image: BackgroundImage?)
}
