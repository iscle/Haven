package com.github.iscle.haven.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.iscle.haven.BuildConfig
import com.github.iscle.haven.data.notifications.NotificationAccess
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.InstalledApp
import com.github.iscle.haven.domain.model.TemperatureUnit
import com.github.iscle.haven.presentation.apps.AppDrawerActivity
import com.github.iscle.haven.presentation.photosource.PhotoSourceActivity
import com.github.iscle.haven.presentation.common.AppIconTile
import com.github.iscle.haven.presentation.common.FlatIconButton
import com.github.iscle.haven.presentation.common.HavenFilledButton
import com.github.iscle.haven.presentation.common.HavenMotion
import com.github.iscle.haven.presentation.common.HavenOutlinedButton
import com.github.iscle.haven.presentation.common.HavenSwitch
import com.github.iscle.haven.presentation.common.SectionLabel
import com.github.iscle.haven.presentation.common.SegmentedToggle
import com.github.iscle.haven.presentation.common.SettingsRow
import com.github.iscle.haven.presentation.common.ambientWindow
import com.github.iscle.haven.presentation.common.formatMinutesOfDay
import com.github.iscle.haven.presentation.common.formatTime
import com.github.iscle.haven.presentation.common.glass
import com.github.iscle.haven.presentation.common.havenCard
import com.github.iscle.haven.presentation.common.nextAlarmTime
import com.github.iscle.haven.ui.theme.HavenAccent
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted
import com.github.iscle.haven.ui.theme.HavenSurface
import com.github.iscle.haven.ui.theme.HavenSurfaceRaised

/** The panes in the settings rail. */
enum class SettingsPane(val title: String, val icon: ImageVector) {
    DISPLAY("Display", Icons.Default.Wallpaper),
    WEATHER("Weather", Icons.Default.WbCloudy),
    CLOCK("Clock & alarms", Icons.Default.Alarm),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    VOICE("Voice & sound", Icons.Default.Mic),
    APPS("Apps", Icons.Default.Apps),
    ABOUT("About Haven", Icons.Default.Info)
}

// Predefined intervals from 10 seconds to 24 hours
private val backgroundIntervals = listOf(
    10,      // 10 seconds
    20,      // 20 seconds
    30,      // 30 seconds
    60,      // 1 minute
    120,     // 2 minutes
    300,     // 5 minutes
    600,     // 10 minutes
    900,     // 15 minutes
    1800,    // 30 minutes
    3600,    // 1 hour
    7200,    // 2 hours
    14400,   // 4 hours
    21600,   // 6 hours
    43200,   // 12 hours
    86400    // 24 hours
)

/** Apps listed in the allow-list before "See all N apps" is used. */
private const val ALLOW_LIST_PREVIEW_SIZE = 6

/** The Unsplash search the wallpapers come from. */
private const val PHOTO_SOURCE_QUERY = "wallpaper nature"

private fun formatInterval(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        seconds < 86400 -> "${seconds / 3600}h"
        else -> "${seconds / 86400}d"
    }
}

private fun findClosestIntervalIndex(value: Int): Int {
    // Find the closest interval to the given value
    return backgroundIntervals
        .mapIndexed { index, interval -> index to kotlin.math.abs(interval - value) }
        .minByOrNull { it.second }
        ?.first ?: 0
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    initialPane: SettingsPane = SettingsPane.DISPLAY,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPane by remember { mutableStateOf(initialPane) }
    var editingAmbientEdge by remember { mutableStateOf<AmbientEdge?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenSurface)
            .safeDrawingPadding()
    ) {
        SettingsRail(
            selectedPane = selectedPane,
            onSelectPane = { selectedPane = it },
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 44.dp, vertical = 38.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedContent(
                targetState = selectedPane,
                transitionSpec = {
                    (fadeIn(tween(HavenMotion.SCREEN_MILLIS)) +
                            slideInVertically(tween(HavenMotion.SCREEN_MILLIS)) { it / 12 })
                        .togetherWith(fadeOut(tween(HavenMotion.CONTROL_MILLIS)))
                },
                label = "settings_pane"
            ) { pane ->
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    when (pane) {
                        SettingsPane.DISPLAY -> DisplayPane(
                            uiState = uiState,
                            viewModel = viewModel,
                            onEditAmbientEdge = { editingAmbientEdge = it }
                        )
                        SettingsPane.WEATHER -> WeatherPane(uiState, viewModel)
                        SettingsPane.CLOCK -> ClockPane(uiState, viewModel)
                        SettingsPane.NOTIFICATIONS -> NotificationsPane(uiState, viewModel)
                        SettingsPane.VOICE -> VoicePane()
                        SettingsPane.APPS -> AppsPane()
                        SettingsPane.ABOUT -> AboutPane()
                    }
                }
            }
        }
    }

    val edge = editingAmbientEdge
    if (edge != null) {
        val preferences = uiState.preferences
        val (windowStart, windowEnd) = ambientWindow(preferences, uiState.weather)
        AmbientTimeDialog(
            edge = edge,
            initialMinutes = if (edge == AmbientEdge.START) windowStart else windowEnd,
            followSun = preferences.ambientFollowSun,
            sunTimeMillis = if (edge == AmbientEdge.START) {
                uiState.weather?.sunsetMillis
            } else {
                uiState.weather?.sunriseMillis
            },
            cityName = uiState.cityName,
            use24Hour = preferences.use24HourClock,
            onDismiss = { editingAmbientEdge = null },
            onConfirm = { minutes, followSun ->
                viewModel.setAmbientFollowSun(followSun)
                if (!followSun) {
                    if (edge == AmbientEdge.START) {
                        viewModel.setAmbientSchedule(minutes, preferences.ambientEndMinutes)
                    } else {
                        viewModel.setAmbientSchedule(preferences.ambientStartMinutes, minutes)
                    }
                }
                editingAmbientEdge = null
            }
        )
    }
}

@Composable
private fun SettingsRail(
    selectedPane: SettingsPane,
    onSelectPane: (SettingsPane) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .background(HavenSurfaceRaised)
            // Seven panes do not fit the short side of a smart display.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlatIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                size = 52.dp,
                iconSize = 26.dp
            )
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenOnSurface
            )
        }

        SettingsPane.entries.forEach { pane ->
            val selected = pane == selectedPane
            val railBackground by animateColorAsState(
                targetValue = if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                animationSpec = tween(HavenMotion.CONTROL_MILLIS),
                label = "rail_selection"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(color = railBackground, shape = RoundedCornerShape(14.dp))
                    .clickable { onSelectPane(pane) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = pane.icon,
                    contentDescription = null,
                    tint = if (selected) HavenOnSurface else HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = pane.title,
                    fontSize = 22.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) HavenOnSurface else HavenOnSurfaceMuted
                )
            }
        }
    }
}

/** 2E — Display: the interval slider that was already here, plus night mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayPane(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onEditAmbientEdge: (AmbientEdge) -> Unit
) {
    val context = LocalContext.current
    val preferences = uiState.preferences

    var selectedIndex by remember { mutableIntStateOf(findClosestIntervalIndex(uiState.backgroundIntervalSeconds)) }
    LaunchedEffect(uiState.backgroundIntervalSeconds) {
        selectedIndex = findClosestIntervalIndex(uiState.backgroundIntervalSeconds)
    }

    SectionLabel("Display")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Background Change Interval",
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenOnSurface,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = formatInterval(backgroundIntervals[selectedIndex]),
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenAccent,
                modifier = Modifier.alignByBaseline()
            )
        }

        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { newValue ->
                selectedIndex = newValue.toInt().coerceIn(0, backgroundIntervals.size - 1)
            },
            onValueChangeFinished = {
                viewModel.setBackgroundInterval(backgroundIntervals[selectedIndex])
            },
            valueRange = 0f..(backgroundIntervals.size - 1).toFloat(),
            steps = backgroundIntervals.size - 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(HavenAccent, CircleShape)
                )
            },
            track = { state ->
                val fraction = if (state.valueRange.endInclusive > state.valueRange.start) {
                    (state.value - state.valueRange.start) /
                            (state.valueRange.endInclusive - state.valueRange.start)
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .background(HavenAccent, CircleShape)
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Set how often the background image changes (10s - 24h)",
                fontSize = 18.sp,
                color = HavenOnSurfaceFaint
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(text = "10s", fontSize = 18.sp, color = HavenOnSurfaceFaint)
                Text(text = "24h", fontSize = 18.sp, color = HavenOnSurfaceFaint)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        val sourceCount = uiState.enabledPhotoSourceCount
        val activeSource = uiState.photoSources.firstOrNull { it.enabled }
        SettingsRow(
            title = "Photo source",
            subtitle = listOfNotNull(
                "Unsplash",
                activeSource?.label,
                "$sourceCount ${if (sourceCount == 1) "source" else "sources"} on"
            ).joinToString(" · "),
            onClick = { context.startActivity(Intent(context, PhotoSourceActivity::class.java)) },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )

        // Ambient mode carries its own schedule, so the row grows a second line.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ambient mode",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HavenOnSurface
                    )
                    Text(
                        text = "Dim the clock and hide the photo overnight",
                        fontSize = 18.sp,
                        color = HavenOnSurfaceFaint
                    )
                }
                HavenSwitch(
                    checked = preferences.ambientModeEnabled,
                    onCheckedChange = viewModel::setAmbientModeEnabled
                )
            }

            val (windowStart, windowEnd) = ambientWindow(preferences, uiState.weather)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AmbientEdgeField(
                    label = "Starts",
                    value = formatMinutesOfDay(windowStart, preferences.use24HourClock),
                    onClick = { onEditAmbientEdge(AmbientEdge.START) },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HavenOnSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp)
                )
                AmbientEdgeField(
                    label = "Ends",
                    value = formatMinutesOfDay(windowEnd, preferences.use24HourClock),
                    onClick = { onEditAmbientEdge(AmbientEdge.END) },
                    modifier = Modifier.weight(1f)
                )
            }
            // At this type size the sun shortcut cannot share the row with the
            // times, so it spans the block beneath them and lines up with both.
            HavenOutlinedButton(
                text = if (preferences.ambientFollowSun) {
                    "Following sunset → sunrise"
                } else {
                    "Use sunset → sunrise"
                },
                onClick = { viewModel.setAmbientFollowSun(!preferences.ambientFollowSun) },
                icon = Icons.Default.WbTwilight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
                height = 56.dp,
                cornerRadius = 14.dp,
                border = BorderStroke(
                    1.dp,
                    if (preferences.ambientFollowSun) {
                        HavenAccent
                    } else {
                        Color.White.copy(alpha = 0.16f)
                    }
                ),
                contentColor = if (preferences.ambientFollowSun) {
                    HavenAccent
                } else {
                    HavenOnSurfaceMuted
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )
        }

        SettingsRow(
            title = "Adaptive brightness",
            subtitle = "Follow the room's light level",
            showDivider = false,
            trailing = {
                HavenSwitch(
                    checked = preferences.adaptiveBrightness,
                    onCheckedChange = viewModel::setAdaptiveBrightness
                )
            }
        )
    }
}

/** One end of the ambient window, styled as a read-only field that opens 2J. */
@Composable
private fun AmbientEdgeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(64.dp)
            .glass(
                shape = RoundedCornerShape(14.dp),
                fill = Color.Black.copy(alpha = 0.24f),
                border = Color.White.copy(alpha = 0.14f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = HavenOnSurfaceFaint,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = HavenAccent,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** 2F top — the city field, kept verbatim, plus the unit switch. */
@Composable
private fun WeatherPane(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    var cityNameInput by remember(uiState.cityName) { mutableStateOf(uiState.cityName) }

    SectionLabel("City for Weather")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .glass(
                        shape = RoundedCornerShape(14.dp),
                        fill = Color.Black.copy(alpha = 0.2f),
                        border = Color.White.copy(alpha = 0.22f)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = cityNameInput,
                    onValueChange = { cityNameInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    cursorBrush = SolidColor(HavenAccent),
                    textStyle = TextStyle(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HavenFilledButton(
                text = "Save",
                onClick = {
                    if (cityNameInput.isNotBlank()) {
                        viewModel.setCityName(cityNameInput.trim())
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Enter the name of your city to get weather information",
                fontSize = 18.sp,
                color = HavenOnSurfaceFaint,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(16.dp))
            SegmentedToggle(
                options = listOf("°C", "°F"),
                selectedIndex = if (uiState.preferences.temperatureUnit == TemperatureUnit.CELSIUS) 0 else 1,
                onSelect = { index ->
                    viewModel.setTemperatureUnit(
                        if (index == 0) TemperatureUnit.CELSIUS else TemperatureUnit.FAHRENHEIT
                    )
                },
                horizontalPadding = 20.dp,
                verticalPadding = 7.dp,
                fontSize = 19.sp
            )
        }
    }
}

/**
 * 2F bottom — Clock & alarms. Haven reports the alarm the system will ring and
 * hands editing to the clock app that owns it, rather than keeping a second
 * schedule of its own.
 */
@Composable
private fun ClockPane(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val preferences = uiState.preferences
    // The clock app owns alarms, so re-read the next one on the way back in.
    var nextAlarm by remember { mutableStateOf(nextAlarmTime(context)) }
    LifecycleResumeEffect(context) {
        nextAlarm = nextAlarmTime(context)
        onPauseOrDispose { }
    }

    val alarmTime = nextAlarm

    SectionLabel("Clock & alarms")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        SettingsRow(
            title = if (alarmTime != null) "Next alarm" else "No alarm set",
            subtitle = if (alarmTime != null) {
                "Set in the clock app that owns it"
            } else {
                "Alarms are kept by the system clock app"
            },
            onClick = { openAlarms(context) },
            verticalPadding = 20.dp,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (alarmTime != null) {
                        Text(
                            text = formatTime(alarmTime, preferences.use24HourClock),
                            fontSize = 40.sp,
                            color = HavenAccent
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HavenOnSurfaceMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        )
        SettingsRow(
            title = "24-hour clock",
            subtitle = "Matches the system locale by default",
            verticalPadding = 20.dp,
            trailing = {
                HavenSwitch(
                    checked = preferences.use24HourClock,
                    onCheckedChange = viewModel::setUse24HourClock
                )
            }
        )
        SettingsRow(
            title = "Show seconds",
            subtitle = "Off on an always-on display to save the panel",
            showDivider = false,
            verticalPadding = 20.dp,
            trailing = {
                HavenSwitch(
                    checked = preferences.showSeconds,
                    onCheckedChange = viewModel::setShowSeconds
                )
            }
        )
    }

    HavenOutlinedButton(
        text = "Add alarm",
        onClick = { openSetAlarm(context) },
        icon = Icons.Default.Add,
        modifier = Modifier.fillMaxWidth(),
        height = 62.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        contentColor = HavenOnSurfaceMuted,
        dashed = true
    )
}

/** 2H — the allow-list behind the notification stack on the clock. */
@Composable
private fun NotificationsPane(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val preferences = uiState.preferences
    var showAllApps by remember { mutableStateOf(false) }
    // Access is granted in system settings, so re-check it every time the user
    // comes back to this pane.
    var accessGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    LifecycleResumeEffect(context) {
        accessGranted = NotificationAccess.isGranted(context)
        onPauseOrDispose { }
    }

    if (!accessGranted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .havenCard()
                .clickable { openNotificationAccess(context) }
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Grant notification access",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HavenOnSurface
                )
                Text(
                    text = "Haven cannot read notifications until you allow it in system settings",
                    fontSize = 18.sp,
                    color = HavenOnSurfaceFaint
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = HavenAccent,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Hide notification content",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenOnSurface
            )
            Text(
                text = "Show the sender only — for a clock in a shared room",
                fontSize = 18.sp,
                color = HavenOnSurfaceFaint
            )
        }
        HavenSwitch(
            checked = preferences.hideNotificationContent,
            onCheckedChange = viewModel::setHideNotificationContent
        )
    }

    val allowed = preferences.allowedNotificationPackages
    // Allowed apps first, so the list reads as the allow-list it is.
    val ordered = remember(uiState.apps, allowed) {
        uiState.apps.sortedWith(
            compareByDescending<InstalledApp> { it.packageName in allowed }
                .thenBy { it.label }
        )
    }
    val visible = if (showAllApps) ordered else ordered.take(ALLOW_LIST_PREVIEW_SIZE)

    SectionLabel("Allowed on the clock · ${allowed.size} of ${uiState.apps.size} apps")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 2.dp)
    ) {
        visible.forEachIndexed { index, app ->
            val isAllowed = app.packageName in allowed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        AppIconTile(
                            packageName = app.packageName,
                            label = app.label,
                            tileSize = 52.dp,
                            iconSize = 32.dp,
                            cornerRadius = 16.dp
                        )
                        Text(
                            text = app.label,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAllowed) HavenOnSurface else HavenOnSurface.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        HavenSwitch(
                            checked = isAllowed,
                            onCheckedChange = { checked ->
                                viewModel.setNotificationPackageAllowed(app.packageName, checked)
                            }
                        )
                    }
                    if (index != visible.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.07f))
                        )
                    }
                }
            }
        }
    }

    if (!showAllApps && ordered.size > ALLOW_LIST_PREVIEW_SIZE) {
        HavenOutlinedButton(
            text = "See all ${ordered.size} apps",
            onClick = { showAllApps = true },
            icon = Icons.AutoMirrored.Filled.List,
            modifier = Modifier.fillMaxWidth(),
            height = 60.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            contentColor = HavenOnSurfaceMuted
        )
    }
}

@Composable
private fun VoicePane() {
    val context = LocalContext.current
    SectionLabel("Voice & sound")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        SettingsRow(
            title = "Assistant",
            subtitle = "The mic on the clock opens the assistant you picked here",
            onClick = { openVoiceInputSettings(context) },
            showDivider = false,
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )
    }
}

@Composable
private fun AppsPane() {
    val context = LocalContext.current
    SectionLabel("Apps")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        SettingsRow(
            title = "Default launcher",
            subtitle = "Choose which app owns the home screen",
            onClick = { openHomeSettings(context) },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )
        SettingsRow(
            title = "App drawer",
            subtitle = "Every launchable app, alphabetical, 12 per page",
            showDivider = false,
            onClick = { context.startActivity(Intent(context, AppDrawerActivity::class.java)) },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )
    }
}

@Composable
private fun AboutPane() {
    val context = LocalContext.current
    SectionLabel("About Haven")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .havenCard()
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        SettingsRow(
            title = "Version",
            subtitle = BuildConfig.VERSION_NAME
        )
        SettingsRow(
            title = "Photos",
            subtitle = "Wallpapers from Unsplash",
            onClick = { openUnsplashSource(context) },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )
        SettingsRow(
            title = "Source",
            subtitle = "github.com/iscle/Haven",
            showDivider = false,
            onClick = { openUrl(context, "https://github.com/iscle/Haven") },
            trailing = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        )
    }
    Spacer(Modifier.height(4.dp))
}

private fun openUnsplashSource(context: Context) {
    openUrl(
        context,
        "https://unsplash.com/s/photos/${Uri.encode(PHOTO_SOURCE_QUERY)}"
    )
}

private fun openUrl(context: Context, url: String) {
    startSafely(context, Intent(Intent.ACTION_VIEW, url.toUri()))
}

private fun openNotificationAccess(context: Context) {
    startSafely(context, NotificationAccess.settingsIntent())
}

private fun openVoiceInputSettings(context: Context) {
    startSafely(context, Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
}

private fun openHomeSettings(context: Context) {
    startSafely(context, Intent(Settings.ACTION_HOME_SETTINGS))
}

private fun openAlarms(context: Context) {
    startSafely(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))
}

private fun openSetAlarm(context: Context) {
    startSafely(context, Intent(AlarmClock.ACTION_SET_ALARM))
}

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing on the device handles it; leave the user where they are.
    }
}
