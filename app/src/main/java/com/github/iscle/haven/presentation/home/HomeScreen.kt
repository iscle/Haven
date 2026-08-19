package com.github.iscle.haven.presentation.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.HavenNotification
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.presentation.apps.AppDrawerActivity
import com.github.iscle.haven.presentation.common.GlassIconButton
import com.github.iscle.haven.presentation.common.HavenMotion
import com.github.iscle.haven.presentation.common.SectionLabel
import com.github.iscle.haven.presentation.common.formatClock
import com.github.iscle.haven.presentation.common.formatForecastHour
import com.github.iscle.haven.presentation.common.formatLongDate
import com.github.iscle.haven.presentation.common.formatRelativeTime
import com.github.iscle.haven.presentation.common.formatTemperature
import com.github.iscle.haven.presentation.common.formatTime
import com.github.iscle.haven.presentation.common.glass
import com.github.iscle.haven.presentation.common.ambientWindow
import com.github.iscle.haven.presentation.common.isWithinAmbientWindow
import com.github.iscle.haven.presentation.common.rememberAppIcon
import com.github.iscle.haven.presentation.common.rememberClockTime
import com.github.iscle.haven.presentation.common.rememberNextAlarmTime
import com.github.iscle.haven.presentation.common.weatherIcon
import com.github.iscle.haven.presentation.history.HistoryActivity
import com.github.iscle.haven.presentation.settings.SettingsActivity
import com.github.iscle.haven.presentation.settings.SettingsPane
import com.github.iscle.haven.ui.theme.HavenAccent
import com.github.iscle.haven.ui.theme.HavenGlassFill
import com.github.iscle.haven.ui.theme.HavenNightInk
import kotlinx.coroutines.delay

/** How long a tap in night mode brings the day layout back for. */
private const val NIGHT_WAKE_MILLIS = 20_000L

/** Notification cards shown on the clock before collapsing into "N more". */
private const val VISIBLE_NOTIFICATIONS = 2

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = uiState.preferences

    val launchSettings = { pane: SettingsPane ->
        context.startActivity(SettingsActivity.intent(context, pane))
    }
    val launchHistory = {
        context.startActivity(Intent(context, HistoryActivity::class.java))
    }
    val launchAppDrawer = {
        context.startActivity(Intent(context, AppDrawerActivity::class.java))
    }
    val launchAssistant = {
        val started = listOf(Intent.ACTION_VOICE_COMMAND, Intent.ACTION_ASSIST).any { action ->
            try {
                context.startActivity(Intent(action))
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
        if (!started) launchAppDrawer()
    }

    // Track the displayed image (only updates when new image is fully loaded)
    var displayedImage by remember { mutableStateOf<BackgroundImage?>(null) }
    val newImage = uiState.backgroundImage

    // Initialize displayedImage on first load
    LaunchedEffect(newImage?.url) {
        if (newImage != null && displayedImage == null) {
            displayedImage = newImage
        }
    }

    // Night mode follows the 23:00–07:00 window; a tap brings the day layout
    // back briefly so the screen stays usable without leaving ambient mode.
    var wakeRequested by remember { mutableStateOf(false) }
    LaunchedEffect(wakeRequested) {
        if (wakeRequested) {
            delay(NIGHT_WAKE_MILLIS)
            wakeRequested = false
        }
    }
    val tick = rememberClockTime(tickEverySecond = preferences.showSeconds)
    val (ambientStart, ambientEnd) = ambientWindow(preferences, uiState.weather)
    val isNight = preferences.ambientModeEnabled &&
            isWithinAmbientWindow(tick, ambientStart, ambientEnd) &&
            !wakeRequested
    val nextAlarm = rememberNextAlarmTime(tick)

    // Adaptive brightness hands the panel back to the system's own light sensor;
    // with it off the clock pins its own level, dimmed in ambient mode.
    val window = (context as? Activity)?.window
    LaunchedEffect(window, preferences.adaptiveBrightness, isNight) {
        window?.let {
            it.attributes = it.attributes.apply {
                screenBrightness = when {
                    preferences.adaptiveBrightness -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    isNight -> 0.15f
                    else -> 1f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .swipeUpToOpen(launchAppDrawer)
            .then(
                if (isNight) Modifier.clickable { wakeRequested = true } else Modifier
            )
    ) {
        // Preload the new image in the background (invisible) and track when it's loaded
        // Only preload if it's different from the currently displayed image
        LaunchedEffect(newImage?.url) {
            if (newImage != null && newImage.url != displayedImage?.url) {
                val request = ImageRequest.Builder(context)
                    .data(newImage.url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()

                val result = imageLoader.execute(request)

                if (result is SuccessResult) {
                    displayedImage = newImage
                }
            }
        }

        // The photo fades down to a trace of itself in night mode.
        val photoAlpha by animateFloatAsState(
            targetValue = if (isNight) 0.16f else 1f,
            animationSpec = tween(1200),
            label = "photo_alpha"
        )

        // Background Image with smooth crossfade transition
        AnimatedContent(
            targetState = displayedImage,
            transitionSpec = {
                fadeIn(animationSpec = tween(1000)) togetherWith
                        fadeOut(animationSpec = tween(1000))
            },
            modifier = Modifier.alpha(photoAlpha),
            label = "background_transition"
        ) { image ->
            if (image != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(image.url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        // Day and ambient are two different screens over the same photo, so
        // they cross-fade rather than snapping at 23:00.
        Crossfade(
            targetState = isNight,
            animationSpec = tween(HavenMotion.SCREEN_MILLIS * 3),
            label = "ambient_transition",
            modifier = Modifier.matchParentSize()
        ) { ambient ->
        Box(modifier = Modifier.fillMaxSize()) {
        if (ambient) {
            NightGlow()
            NightClock(
                timeMillis = tick,
                weather = uiState.weather,
                preferences = preferences,
                nextAlarmMillis = nextAlarm,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            PhotoScrims()

            WeatherChip(
                weather = uiState.weather,
                preferences = preferences,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 56.dp, top = 44.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 56.dp, top = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassIconButton(
                    icon = Icons.Default.History,
                    contentDescription = "Wallpaper History",
                    onClick = launchHistory
                )
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = { launchSettings(SettingsPane.DISPLAY) }
                )
            }

            ClockBlock(
                timeMillis = tick,
                preferences = preferences,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 56.dp, bottom = 180.dp)
            )

            NotificationStack(
                notifications = uiState.notifications,
                nowMillis = tick,
                onReview = { launchSettings(SettingsPane.NOTIFICATIONS) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 56.dp, top = 150.dp)
            )

            if (nextAlarm != null) {
                NextAlarmChip(
                    alarmMillis = nextAlarm,
                    preferences = preferences,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 56.dp, bottom = 180.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 56.dp, bottom = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ForecastStrip(
                    weather = uiState.weather,
                    preferences = preferences
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val credit = displayedImage
                    if (credit != null && preferences.showPhotographerCredit) {
                        Text(
                            text = "${credit.photographer} · Unsplash",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.85.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Default.Apps,
                        contentDescription = "Apps",
                        onClick = launchAppDrawer
                    )
                    GlassIconButton(
                        icon = Icons.Default.Mic,
                        contentDescription = "Voice",
                        onClick = launchAssistant,
                        emphasized = true
                    )
                }
            }

            DrawerHandle(
                onOpen = launchAppDrawer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
        }
        }
    }
}

/** Swipe up anywhere on the clock to reach the app drawer. */
private fun Modifier.swipeUpToOpen(onOpen: () -> Unit): Modifier = pointerInput(Unit) {
    var travelled = 0f
    detectVerticalDragGestures(
        onDragStart = { travelled = 0f },
        onDragEnd = { if (travelled < -40f) onOpen() }
    ) { _, delta ->
        travelled += delta
    }
}

/**
 * Soft scrims replacing the hard offset shadows of the previous screen: a
 * diagonal wash that keeps the top-left and bottom corners readable, plus a
 * vignette so nothing floats on a bright photo.
 */
@Composable
private fun BoxScope.PhotoScrims() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.38f to Color.Black.copy(alpha = 0.18f),
                            0.55f to Color.Black.copy(alpha = 0.05f),
                            1f to Color.Black.copy(alpha = 0.60f)
                        ),
                        start = Offset(size.width * 0.2f, 0f),
                        end = Offset(size.width * 0.8f, size.height)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.35f)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width, size.height) * 0.75f
                    )
                )
            }
    )
}

/** A warm breath of light behind the ambient clock. */
@Composable
private fun BoxScope.NightGlow() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD6AA).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height * 0.55f),
                        radius = maxOf(size.width, size.height) * 0.7f
                    )
                )
            }
    )
}

@Composable
private fun WeatherChip(
    weather: Weather?,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    if (weather == null) return
    Row(
        modifier = modifier
            .glass(cornerRadius = 26.dp)
            .padding(start = 20.dp, top = 16.dp, end = 26.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = weatherIcon(weather.weatherCode, weather.isDay),
            contentDescription = weather.description,
            tint = Color.White,
            modifier = Modifier.size(52.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = formatTemperature(weather.temperature, preferences.temperatureUnit),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "feels ${formatTemperature(weather.feelsLike, preferences.temperatureUnit)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.alignByBaseline()
                )
            }
            Text(
                text = "${weather.description} · ${weather.cityName}",
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.76.sp,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun ClockBlock(
    timeMillis: Long,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = formatClock(timeMillis, preferences.use24HourClock, preferences.showSeconds),
            fontSize = 200.sp,
            lineHeight = 156.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-4).sp,
            color = Color.White,
            // The digits sit at the top of their line box, which leaves them
            // crowding the weather chip and floating above the date. Shifting
            // them into that slack centres the block optically.
            modifier = Modifier.offset(y = 26.dp)
        )
        Text(
            text = formatLongDate(timeMillis).uppercase(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.8.sp,
            color = Color.White.copy(alpha = 0.82f)
        )
    }
}

/**
 * Filtered notification stack: allowed apps only, newest first, the rest
 * collapsed into a single row that opens the allow-list.
 */
@Composable
private fun NotificationStack(
    notifications: List<HavenNotification>,
    nowMillis: Long,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return
    val visible = notifications.take(VISIBLE_NOTIFICATIONS)
    val hidden = notifications.size - visible.size

    Column(
        modifier = modifier.width(430.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel(
                text = "Notifications",
                color = Color.White.copy(alpha = 0.55f)
            )
            Text(
                text = "Allowed apps only",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.45f)
            )
        }

        visible.forEachIndexed { index, notification ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(HavenMotion.SCREEN_MILLIS)) +
                        expandVertically(tween(HavenMotion.SCREEN_MILLIS)),
                exit = fadeOut(tween(HavenMotion.CONTROL_MILLIS)) +
                        shrinkVertically(tween(HavenMotion.CONTROL_MILLIS))
            ) {
                if (index == 0) {
                    NotificationCard(notification, nowMillis, onReview)
                } else {
                    CompactNotificationRow(notification, nowMillis, onReview)
                }
            }
        }

        if (hidden > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(
                        cornerRadius = 22.dp,
                        fill = Color(0xFF0E1016).copy(alpha = 0.22f),
                        border = Color.White.copy(alpha = 0.08f)
                    )
                    .clickable(onClick = onReview)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "$hidden more · tap to review",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: HavenNotification,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val hasBody = notification.text.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(
                cornerRadius = 22.dp,
                fill = Color(0xFF0E1016).copy(alpha = 0.34f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        // Sender-only cards (content hidden) are a single line: centre them on
        // the icon rather than leaving the text hanging at the top.
        verticalAlignment = if (hasBody) Alignment.Top else Alignment.CenterVertically
    ) {
        NotificationAppIcon(
            notification = notification,
            modifier = if (hasBody) Modifier.padding(top = 2.dp) else Modifier
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notification.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .alignByBaseline()
                )
                Text(
                    text = formatRelativeTime(notification.postTime, nowMillis),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.alignByBaseline()
                )
            }
            if (hasBody) {
                Text(
                    text = notification.text,
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactNotificationRow(
    notification: HavenNotification,
    nowMillis: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(
                cornerRadius = 22.dp,
                fill = HavenGlassFill,
                border = Color.White.copy(alpha = 0.11f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NotificationAppIcon(notification)
        Text(
            text = listOf(notification.title, notification.text)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatRelativeTime(notification.postTime, nowMillis),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun NotificationAppIcon(
    notification: HavenNotification,
    modifier: Modifier = Modifier
) {
    val icon = rememberAppIcon(notification.packageName)
    if (icon != null) {
        androidx.compose.foundation.Image(
            bitmap = icon,
            contentDescription = notification.appLabel,
            modifier = modifier.size(30.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = notification.appLabel,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = modifier.size(30.dp)
        )
    }
}

/** The alarm the system will ring next — the launcher only reports it. */
@Composable
private fun NextAlarmChip(
    alarmMillis: Long,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .glass(shape = CircleShape, border = Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = HavenAccent,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "Alarm ${formatTime(alarmMillis, preferences.use24HourClock)}",
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.63.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun ForecastStrip(
    weather: Weather?,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    if (weather == null || weather.forecast.isEmpty()) {
        Spacer(modifier)
        return
    }
    Row(
        modifier = modifier
            .glass(cornerRadius = 24.dp, border = Color.White.copy(alpha = 0.11f))
            .padding(horizontal = 30.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        ForecastColumn(
            label = "NOW",
            icon = weatherIcon(weather.weatherCode, weather.isDay),
            temperature = formatTemperature(weather.temperature, preferences.temperatureUnit),
            emphasized = true
        )
        weather.forecast.forEach { hour ->
            ForecastColumn(
                label = formatForecastHour(hour.timeMillis, preferences.use24HourClock),
                icon = weatherIcon(hour.weatherCode, hour.isDay),
                temperature = formatTemperature(hour.temperature, preferences.temperatureUnit),
                emphasized = false
            )
        }
    }
}

@Composable
private fun ForecastColumn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    temperature: String,
    emphasized: Boolean
) {
    val contentColor = if (emphasized) Color.White else Color.White.copy(alpha = 0.9f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = temperature,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun DrawerHandle(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onOpen)
            .padding(horizontal = 40.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Open apps",
            tint = Color.White.copy(alpha = 0.32f),
            modifier = Modifier.size(22.dp)
        )
        Box(
            modifier = Modifier
                .size(width = 132.dp, height = 5.dp)
                .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
        )
    }
}

/** Ambient clock: warm ink, no photo, nothing to read from across a dark room. */
@Composable
private fun NightClock(
    timeMillis: Long,
    weather: Weather?,
    preferences: AppPreferences,
    nextAlarmMillis: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            text = formatClock(timeMillis, preferences.use24HourClock, preferences.showSeconds),
            fontSize = 216.sp,
            lineHeight = 170.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
            color = HavenNightInk.copy(alpha = 0.9f)
        )
        Text(
            text = formatLongDate(timeMillis).uppercase(),
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 6.24.sp,
            color = HavenNightInk.copy(alpha = 0.42f)
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (weather != null) {
                Text(
                    text = "${formatTemperature(weather.temperature, preferences.temperatureUnit)} ${weather.cityName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.32.sp,
                    color = HavenNightInk.copy(alpha = 0.4f)
                )
            }
            if (weather != null && nextAlarmMillis != null) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(HavenNightInk.copy(alpha = 0.3f), CircleShape)
                )
            }
            if (nextAlarmMillis != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = HavenNightInk.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = formatTime(nextAlarmMillis, preferences.use24HourClock),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.32.sp,
                        color = HavenNightInk.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
