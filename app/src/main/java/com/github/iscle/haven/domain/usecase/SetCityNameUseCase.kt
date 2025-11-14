package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.PreferencesRepository
import timber.log.Timber
import javax.inject.Inject

class SetCityNameUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(city: String) {
        Timber.d("UseCase: Setting city name: $city")
        preferencesRepository.setCityName(city)
    }
}

