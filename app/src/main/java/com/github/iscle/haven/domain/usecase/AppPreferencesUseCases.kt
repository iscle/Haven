package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.TemperatureUnit
import com.github.iscle.haven.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<AppPreferences> = preferencesRepository.appPreferences
}

class SetNightModeEnabledUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setNightModeEnabled(enabled)
}

class SetAdaptiveBrightnessUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setAdaptiveBrightness(enabled)
}

class SetShowPhotographerCreditUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setShowPhotographerCredit(enabled)
}

class SetTemperatureUnitUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(unit: TemperatureUnit) =
        preferencesRepository.setTemperatureUnit(unit)
}

class SetUse24HourClockUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setUse24HourClock(enabled)
}

class SetShowSecondsUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setShowSeconds(enabled)
}

class SetHideNotificationContentUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        preferencesRepository.setHideNotificationContent(enabled)
}

class SetNotificationPackageAllowedUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(packageName: String, allowed: Boolean) =
        preferencesRepository.setNotificationPackageAllowed(packageName, allowed)
}
