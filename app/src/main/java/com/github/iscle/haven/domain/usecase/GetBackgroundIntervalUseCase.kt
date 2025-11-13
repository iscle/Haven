package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBackgroundIntervalUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<Int> {
        return preferencesRepository.backgroundInterval
    }
}

