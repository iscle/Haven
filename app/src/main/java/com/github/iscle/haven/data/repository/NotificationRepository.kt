package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.notifications.ActiveNotificationsDataSource
import com.github.iscle.haven.domain.model.HavenNotification
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.github.iscle.haven.domain.repository.NotificationRepository as NotificationRepositoryInterface

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val activeNotificationsDataSource: ActiveNotificationsDataSource
) : NotificationRepositoryInterface {
    override val notifications: Flow<List<HavenNotification>> =
        activeNotificationsDataSource.notifications
}
