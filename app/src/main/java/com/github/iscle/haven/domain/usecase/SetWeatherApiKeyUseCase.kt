package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.data.repository.PreferencesRepository
import timber.log.Timber
import javax.inject.Inject

class SetWeatherApiKeyUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(apiKey: String) {
        Timber.d("UseCase: Setting weather API key: ${if (apiKey.isNotBlank()) "***${apiKey.takeLast(4)}" else "empty"}")
        preferencesRepository.setWeatherApiKey(apiKey)
    }
}

