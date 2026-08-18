package com.github.iscle.haven.data.notifications

import com.github.iscle.haven.domain.model.HavenNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory mirror of the notifications the listener service currently sees.
 * Notifications are process state, not storage — they are dropped when the
 * listener disconnects.
 */
@Singleton
class ActiveNotificationsDataSource @Inject constructor() {

    private val _notifications = MutableStateFlow<List<HavenNotification>>(emptyList())
    val notifications: StateFlow<List<HavenNotification>> = _notifications.asStateFlow()

    fun update(notifications: List<HavenNotification>) {
        Timber.d("ActiveNotifications: ${notifications.size} posted")
        _notifications.value = notifications
    }

    fun clear() {
        Timber.d("ActiveNotifications: cleared")
        _notifications.value = emptyList()
    }
}
