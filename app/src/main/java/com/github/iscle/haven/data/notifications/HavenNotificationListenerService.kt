package com.github.iscle.haven.data.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.github.iscle.haven.domain.model.HavenNotification
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Feeds the notification stack on the home screen. Filtering against the
 * user's allow-list happens downstream, so changing the list does not require
 * the listener to reconnect.
 */
@AndroidEntryPoint
class HavenNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationsDataSource: ActiveNotificationsDataSource

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.i("NotificationListener: connected")
        publishCurrent()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.i("NotificationListener: disconnected")
        notificationsDataSource.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishCurrent()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishCurrent()
    }

    private fun publishCurrent() {
        // getActiveNotifications() throws if the listener is not connected yet.
        val current = runCatching { activeNotifications }
            .onFailure { Timber.w(it, "NotificationListener: could not read active notifications") }
            .getOrNull()
            ?: return
        notificationsDataSource.update(
            current
                .filter { it.isWorthShowing() }
                .mapNotNull { it.toDomain() }
                .sortedByDescending { it.postTime }
        )
    }

    /** Skip our own notifications, ongoing service chrome and group summaries. */
    private fun StatusBarNotification.isWorthShowing(): Boolean {
        if (packageName == this@HavenNotificationListenerService.packageName) return false
        val flags = notification?.flags ?: return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    private fun StatusBarNotification.toDomain(): HavenNotification? {
        val extras = notification?.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return null
        val label = appLabel(packageName)
        return HavenNotification(
            key = key,
            packageName = packageName,
            appLabel = label,
            title = title.ifBlank { label },
            text = text,
            postTime = postTime
        )
    }

    private fun appLabel(packageName: String): String = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}
