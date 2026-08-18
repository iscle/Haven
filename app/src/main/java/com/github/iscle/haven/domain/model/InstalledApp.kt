package com.github.iscle.haven.domain.model

/** A launchable app, as shown in the app drawer and the notification allow-list. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val firstInstallTime: Long
)
