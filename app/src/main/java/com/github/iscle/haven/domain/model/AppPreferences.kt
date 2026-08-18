package com.github.iscle.haven.domain.model

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/** Every user-facing display preference behind the settings panes. */
data class AppPreferences(
    val nightModeEnabled: Boolean = true,
    val adaptiveBrightness: Boolean = true,
    val showPhotographerCredit: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val use24HourClock: Boolean = true,
    val showSeconds: Boolean = false,
    val hideNotificationContent: Boolean = false,
    val allowedNotificationPackages: Set<String> = emptySet()
)
