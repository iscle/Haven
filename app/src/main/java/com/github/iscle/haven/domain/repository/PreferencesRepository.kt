package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.TemperatureUnit
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val backgroundInterval: Flow<Int>
    val cityName: Flow<String>
    val location: Flow<Pair<Double?, Double?>>
    val appPreferences: Flow<AppPreferences>
    val pinnedImage: Flow<BackgroundImage?>
    val photoSources: Flow<List<PhotoSource>>
    val lastPhotoSourceId: Flow<String?>

    suspend fun setBackgroundInterval(seconds: Int)
    suspend fun setCityName(city: String)
    suspend fun setLocation(latitude: Double, longitude: Double)

    suspend fun setAmbientModeEnabled(enabled: Boolean)
    suspend fun setAmbientSchedule(startMinutes: Int, endMinutes: Int)
    suspend fun setAmbientFollowSun(enabled: Boolean)
    suspend fun setPhotoSources(sources: List<PhotoSource>)
    suspend fun setLastPhotoSourceId(sourceId: String)
    suspend fun setAdaptiveBrightness(enabled: Boolean)
    suspend fun setShowPhotographerCredit(enabled: Boolean)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setUse24HourClock(enabled: Boolean)
    suspend fun setShowSeconds(enabled: Boolean)
    suspend fun setHideNotificationContent(enabled: Boolean)
    suspend fun setNotificationPackageAllowed(packageName: String, allowed: Boolean)
    suspend fun setPinnedImage(image: BackgroundImage?)
}
