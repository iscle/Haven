package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.data.repository.PreferencesRepository
import timber.log.Timber
import javax.inject.Inject

class SetBackgroundIntervalUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(seconds: Int) {
        Timber.d("UseCase: Setting background interval: $seconds seconds")
        preferencesRepository.setBackgroundInterval(seconds)
    }
}

