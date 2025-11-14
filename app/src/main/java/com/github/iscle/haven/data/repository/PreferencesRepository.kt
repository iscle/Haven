package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.PreferencesDataSource
import com.github.iscle.haven.domain.repository.PreferencesRepository as PreferencesRepositoryInterface
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : PreferencesRepositoryInterface {
    override val backgroundInterval: Flow<Int> = preferencesDataSource.backgroundInterval
    override val cityName: Flow<String> = preferencesDataSource.cityName
    override val location: Flow<Pair<Double?, Double?>> = preferencesDataSource.location

    override suspend fun setBackgroundInterval(seconds: Int) {
        preferencesDataSource.setBackgroundInterval(seconds)
    }

    override suspend fun setCityName(city: String) {
        preferencesDataSource.setCityName(city)
    }

    override suspend fun setLocation(latitude: Double, longitude: Double) {
        preferencesDataSource.setLocation(latitude, longitude)
    }
}

