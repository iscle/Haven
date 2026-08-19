package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.PreferencesDataSource
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.github.iscle.haven.domain.repository.PreferencesRepository as PreferencesRepositoryInterface

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : PreferencesRepositoryInterface {
    override val backgroundInterval: Flow<Int> = preferencesDataSource.backgroundInterval
    override val cityName: Flow<String> = preferencesDataSource.cityName
    override val location: Flow<Pair<Double?, Double?>> = preferencesDataSource.location
    override val appPreferences: Flow<AppPreferences> = preferencesDataSource.appPreferences
    override val pinnedImage: Flow<BackgroundImage?> = preferencesDataSource.pinnedImage
    override val photoSources: Flow<List<PhotoSource>> = preferencesDataSource.photoSources
    override val lastPhotoSourceId: Flow<String?> = preferencesDataSource.lastPhotoSourceId

    override suspend fun setBackgroundInterval(seconds: Int) {
        preferencesDataSource.setBackgroundInterval(seconds)
    }

    override suspend fun setCityName(city: String) {
        preferencesDataSource.setCityName(city)
    }

    override suspend fun setLocation(latitude: Double, longitude: Double) {
        preferencesDataSource.setLocation(latitude, longitude)
    }

    override suspend fun setAmbientModeEnabled(enabled: Boolean) {
        preferencesDataSource.setAmbientModeEnabled(enabled)
    }

    override suspend fun setAmbientSchedule(startMinutes: Int, endMinutes: Int) {
        preferencesDataSource.setAmbientSchedule(startMinutes, endMinutes)
    }

    override suspend fun setAmbientFollowSun(enabled: Boolean) {
        preferencesDataSource.setAmbientFollowSun(enabled)
    }

    override suspend fun setPhotoSources(sources: List<PhotoSource>) {
        preferencesDataSource.setPhotoSources(sources)
    }

    override suspend fun setLastPhotoSourceId(sourceId: String) {
        preferencesDataSource.setLastPhotoSourceId(sourceId)
    }

    override suspend fun setAdaptiveBrightness(enabled: Boolean) {
        preferencesDataSource.setAdaptiveBrightness(enabled)
    }

    override suspend fun setShowPhotographerCredit(enabled: Boolean) {
        preferencesDataSource.setShowPhotographerCredit(enabled)
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        preferencesDataSource.setTemperatureUnit(unit)
    }

    override suspend fun setUse24HourClock(enabled: Boolean) {
        preferencesDataSource.setUse24HourClock(enabled)
    }

    override suspend fun setShowSeconds(enabled: Boolean) {
        preferencesDataSource.setShowSeconds(enabled)
    }

    override suspend fun setHideNotificationContent(enabled: Boolean) {
        preferencesDataSource.setHideNotificationContent(enabled)
    }

    override suspend fun setNotificationPackageAllowed(packageName: String, allowed: Boolean) {
        preferencesDataSource.setNotificationPackageAllowed(packageName, allowed)
    }

    override suspend fun setPinnedImage(image: BackgroundImage?) {
        preferencesDataSource.setPinnedImage(image)
    }
}
