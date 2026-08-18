package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.HavenNotification
import com.github.iscle.haven.domain.repository.NotificationRepository
import com.github.iscle.haven.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Only apps on the allow-list reach the clock, and when the user asked to hide
 * content the body is dropped here so it never reaches the UI.
 */
class GetAllowedNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<List<HavenNotification>> = combine(
        notificationRepository.notifications,
        preferencesRepository.appPreferences
    ) { notifications, preferences ->
        notifications
            .filter { it.packageName in preferences.allowedNotificationPackages }
            .map { notification ->
                if (preferences.hideNotificationContent) {
                    notification.copy(text = "")
                } else {
                    notification
                }
            }
    }
}
