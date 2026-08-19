package com.github.iscle.haven.domain.model

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/** The default ambient window, kept when the user has not picked their own. */
const val DEFAULT_AMBIENT_START_MINUTES = 23 * 60
const val DEFAULT_AMBIENT_END_MINUTES = 7 * 60

/** Every user-facing display preference behind the settings panes. */
data class AppPreferences(
    val ambientModeEnabled: Boolean = true,
    /** Minutes past midnight; the window wraps when start is after end. */
    val ambientStartMinutes: Int = DEFAULT_AMBIENT_START_MINUTES,
    val ambientEndMinutes: Int = DEFAULT_AMBIENT_END_MINUTES,
    /** Follow the sun instead of the fixed times: sunset to sunrise. */
    val ambientFollowSun: Boolean = false,
    val adaptiveBrightness: Boolean = true,
    val showPhotographerCredit: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val use24HourClock: Boolean = true,
    val showSeconds: Boolean = false,
    val hideNotificationContent: Boolean = false,
    val allowedNotificationPackages: Set<String> = emptySet()
)
