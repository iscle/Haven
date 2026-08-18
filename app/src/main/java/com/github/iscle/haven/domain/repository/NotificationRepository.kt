package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.HavenNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notifications: Flow<List<HavenNotification>>
}
