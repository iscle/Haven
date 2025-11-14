package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCityNameUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<String> {
        return preferencesRepository.cityName
    }
}

