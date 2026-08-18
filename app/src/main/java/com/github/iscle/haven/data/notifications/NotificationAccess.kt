package com.github.iscle.haven.data.notifications

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Reading notifications needs the user to grant notification access in system
 * settings; there is no runtime permission for it.
 */
object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val expected = ComponentName(context, HavenNotificationListenerService::class.java)
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == expected
        }
    }

    fun settingsIntent() = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
