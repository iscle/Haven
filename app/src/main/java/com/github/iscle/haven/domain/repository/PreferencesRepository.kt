package com.github.iscle.haven.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val backgroundInterval: Flow<Int>
    val cityName: Flow<String>
    val location: Flow<Pair<Double?, Double?>>

    suspend fun setBackgroundInterval(seconds: Int)
    suspend fun setCityName(city: String)
    suspend fun setLocation(latitude: Double, longitude: Double)
}

