package com.github.iscle.haven.presentation.common

import android.app.AlarmManager
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.TemperatureUnit
import com.github.iscle.haven.domain.model.Weather
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Wall time that recomposes on the minute, or every second when seconds are shown. */
@Composable
fun rememberClockTime(tickEverySecond: Boolean = false): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickEverySecond) {
        val step = if (tickEverySecond) 1_000L else 60_000L
        while (isActive) {
            val current = System.currentTimeMillis()
            delay(((current / step) + 1) * step - current)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** The alarm the system will ring next, as any launcher shows it. */
@Composable
fun rememberNextAlarmTime(tick: Long): Long? {
    val context = LocalContext.current
    return remember(tick) { nextAlarmTime(context) }
}

fun nextAlarmTime(context: Context): Long? {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    return alarmManager?.nextAlarmClock?.triggerTime
}

fun formatClock(timeMillis: Long, use24Hour: Boolean, showSeconds: Boolean): String {
    val pattern = buildString {
        append(if (use24Hour) "H:mm" else "h:mm")
        if (showSeconds) append(":ss")
        if (!use24Hour) append(" a")
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMillis))
}

/** "Tuesday, 18 August" — rendered upper case by the caller. */
fun formatLongDate(timeMillis: Long): String =
    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timeMillis))

fun formatShortDate(timeMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timeMillis))

fun formatDateTime(timeMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(timeMillis))

fun formatTime(timeMillis: Long, use24Hour: Boolean = true): String =
    SimpleDateFormat(if (use24Hour) "H:mm" else "h:mm a", Locale.getDefault())
        .format(Date(timeMillis))

/** Hour label for the forecast strip: "12", "15" (or "3PM" on a 12-hour clock). */
fun formatForecastHour(timeMillis: Long, use24Hour: Boolean): String =
    SimpleDateFormat(if (use24Hour) "H" else "ha", Locale.getDefault())
        .format(Date(timeMillis))
        .uppercase(Locale.getDefault())

/** Relative age of a notification: "now", "2m", "1h", "3d". */
fun formatRelativeTime(timeMillis: Long, nowMillis: Long): String {
    val elapsed = (nowMillis - timeMillis).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 24 * 60 -> "${minutes / 60}h"
        else -> "${minutes / (24 * 60)}d"
    }
}

fun formatTemperature(celsius: Double, unit: TemperatureUnit): String =
    "${temperatureIn(celsius, unit).roundToInt()}°"

fun temperatureIn(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
    TemperatureUnit.CELSIUS -> celsius
    TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
}

/** Minutes past midnight, local time. */
fun minutesOfDay(timeMillis: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

fun formatMinutesOfDay(minutes: Int, use24Hour: Boolean = true): String {
    val hour = (minutes / 60) % 24
    val minute = minutes % 60
    return if (use24Hour) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } else {
        val suffix = if (hour < 12) "AM" else "PM"
        val displayHour = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, suffix)
    }
}

/**
 * The ambient window in minutes past midnight. When the schedule follows the
 * sun it runs sunset to sunrise, otherwise it uses the times the user picked.
 */
fun ambientWindow(
    preferences: AppPreferences,
    weather: Weather?
): Pair<Int, Int> {
    if (preferences.ambientFollowSun) {
        val sunset = weather?.sunsetMillis?.let(::minutesOfDay)
        val sunrise = weather?.sunriseMillis?.let(::minutesOfDay)
        if (sunset != null && sunrise != null) return sunset to sunrise
    }
    return preferences.ambientStartMinutes to preferences.ambientEndMinutes
}

/** True inside the ambient window, which normally wraps past midnight. */
fun isWithinAmbientWindow(timeMillis: Long, startMinutes: Int, endMinutes: Int): Boolean {
    if (startMinutes == endMinutes) return false
    val now = minutesOfDay(timeMillis)
    return if (startMinutes < endMinutes) {
        now >= startMinutes && now < endMinutes
    } else {
        now >= startMinutes || now < endMinutes
    }
}

/** WMO weather code to the closest available Material icon. */
fun weatherIcon(weatherCode: Int, isDay: Boolean): ImageVector = when (weatherCode) {
    0 -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightlightRound
    1, 2 -> if (isDay) Icons.Default.WbCloudy else Icons.Default.NightsStay
    3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.Dehaze
    51, 53, 55, 56, 57 -> Icons.Default.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Default.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
    95, 96, 99 -> Icons.Default.Thunderstorm
    else -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightlightRound
}
