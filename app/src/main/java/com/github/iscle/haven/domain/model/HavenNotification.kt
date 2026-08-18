package com.github.iscle.haven.domain.model

/** A notification currently posted by an app the user allowed on the clock. */
data class HavenNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long
)
