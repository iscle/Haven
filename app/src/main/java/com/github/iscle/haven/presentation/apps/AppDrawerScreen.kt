package com.github.iscle.haven.presentation.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.iscle.haven.domain.model.AppPreferences
import com.github.iscle.haven.domain.model.InstalledApp
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.presentation.common.AppIconTile
import com.github.iscle.haven.presentation.common.FlatIconButton
import com.github.iscle.haven.presentation.common.SectionLabel
import com.github.iscle.haven.presentation.common.formatClock
import com.github.iscle.haven.presentation.common.formatTemperature
import com.github.iscle.haven.presentation.common.glass
import com.github.iscle.haven.presentation.common.rememberClockTime
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceDim
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted
import com.github.iscle.haven.ui.theme.HavenSurface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val RECENTLY_INSTALLED_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

@Composable
fun AppDrawerScreen(
    onClose: () -> Unit,
    viewModel: AppDrawerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchApp: (InstalledApp) -> Unit = { app ->
        if (launchPackage(context, app.packageName)) onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenSurface)
            .drawBehind {
                // A trace of the wallpaper's warmth, so the drawer feels like the
                // same surface as the clock it slid up from.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFC9805C).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.74f, size.height * 0.12f),
                        radius = maxOf(size.width, size.height) * 0.7f
                    )
                )
            }
    ) {
        if (uiState.isSearching) {
            AppSearchPane(
                uiState = uiState,
                onQueryChange = viewModel::onQueryChange,
                onExitSearch = viewModel::stopSearch,
                onLaunchApp = launchApp
            )
        } else {
            AppPagesPane(
                uiState = uiState,
                onStartSearch = viewModel::startSearch,
                onLaunchApp = launchApp,
                onClose = onClose
            )
        }
    }
}

/** 2A — paged, alphabetical grid of every launchable app. */
@Composable
private fun AppPagesPane(
    uiState: AppDrawerUiState,
    onStartSearch: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    onClose: () -> Unit
) {
    val pages = uiState.pages
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = { if (travelled > 40f) onClose() }
                ) { _, delta -> travelled += delta }
            }
    ) {
        DrawerHeader(
            appCount = uiState.apps.size,
            weather = uiState.weather,
            preferences = uiState.preferences,
            onStartSearch = onStartSearch
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 40.dp)
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex).orEmpty()
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                page.chunked(6).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { app ->
                            AppGridItem(
                                app = app,
                                onClick = { onLaunchApp(app) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Keep the last row on the same 6-column rhythm.
                        repeat(6 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        DrawerFooter(
            pageIndex = pagerState.currentPage,
            pageCount = pages.size
        )
    }
}

@Composable
private fun DrawerHeader(
    appCount: Int,
    weather: Weather?,
    preferences: AppPreferences,
    onStartSearch: () -> Unit
) {
    val tick = rememberClockTime()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 44.dp, top = 34.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = "Apps",
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.76.sp,
                color = HavenOnSurface,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "$appCount installed · A–Z",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = HavenOnSurface.copy(alpha = 0.5f),
                modifier = Modifier.alignByBaseline()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = 300.dp)
                    .height(56.dp)
                    .glass(
                        shape = CircleShape,
                        fill = Color.White.copy(alpha = 0.07f),
                        border = Color.White.copy(alpha = 0.1f)
                    )
                    .clickable(onClick = onStartSearch)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = HavenOnSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Search apps",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = HavenOnSurface.copy(alpha = 0.42f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = formatClock(tick, preferences.use24HourClock, showSeconds = false),
                    fontSize = 30.sp,
                    color = HavenOnSurface,
                    modifier = Modifier.alignByBaseline()
                )
                if (weather != null) {
                    Text(
                        text = formatTemperature(weather.temperature, preferences.temperatureUnit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface.copy(alpha = 0.5f),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: InstalledApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        AppIconTile(packageName = app.packageName, label = app.label)
        Text(
            text = app.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = HavenOnSurface.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerFooter(
    pageIndex: Int,
    pageCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 44.dp, top = 20.dp, bottom = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Page ${pageIndex + 1} of ${pageCount.coerceAtLeast(1)}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.02.sp,
            color = HavenOnSurfaceDim,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount.coerceAtLeast(1)) { index ->
                val active = index == pageIndex
                Box(
                    modifier = Modifier
                        .size(width = if (active) 34.dp else 8.dp, height = 8.dp)
                        .background(
                            color = Color.White.copy(alpha = if (active) 0.85f else 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
        Text(
            text = "Swipe for more · swipe down for the clock",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.02.sp,
            color = HavenOnSurfaceDim,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/** 2G — search, the answer once 48 apps become 200. */
@Composable
private fun AppSearchPane(
    uiState: AppDrawerUiState,
    onQueryChange: (String) -> Unit,
    onExitSearch: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val results = uiState.results

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 44.dp, top = 30.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .glass(
                        shape = CircleShape,
                        fill = Color.White.copy(alpha = 0.09f),
                        border = Color.White.copy(alpha = 0.18f)
                    )
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = HavenOnSurfaceMuted,
                    modifier = Modifier.size(28.dp)
                )
                BasicTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White.copy(alpha = 0.7f)),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
            }
            FlatIconButton(
                icon = Icons.Default.Mic,
                contentDescription = "Voice search",
                onClick = { startVoiceSearch(context) },
                size = 64.dp
            )
            FlatIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close search",
                onClick = onExitSearch,
                size = 64.dp
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 44.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionLabel(
                        text = if (uiState.query.isBlank()) {
                            "${results.size} apps"
                        } else {
                            "${results.size} results"
                        },
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(items = results, key = { it.packageName }) { app ->
                    AppResultRow(app = app, onClick = { onLaunchApp(app) })
                }
                if (uiState.query.isNotBlank()) {
                    item {
                        PlayStoreRow(
                            query = uiState.query,
                            onClick = { searchPlayStore(context, uiState.query) }
                        )
                    }
                }
            }

            AlphabetRail(
                apps = results,
                onLetterSelected = { index ->
                    // +1 for the "N results" header item.
                    scope.launch { listState.scrollToItem(index + 1) }
                }
            )
        }

        Text(
            text = "Drag the rail to jump · say \"open Calendar\" to skip all of this",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.02.sp,
            color = HavenOnSurfaceDim,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 26.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AppResultRow(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .glass(
                shape = RoundedCornerShape(20.dp),
                fill = Color.White.copy(alpha = 0.05f),
                border = Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AppIconTile(
            packageName = app.packageName,
            label = app.label,
            tileSize = 68.dp,
            iconSize = 44.dp,
            cornerRadius = 20.dp
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = app.label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = app.subtitle()
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 18.sp,
                    color = HavenOnSurfaceFaint
                )
            }
        }
        Icon(
            imageVector = Icons.Default.NorthEast,
            contentDescription = null,
            tint = HavenOnSurfaceDim,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun PlayStoreRow(
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .glass(
                shape = RoundedCornerShape(20.dp),
                fill = Color.White.copy(alpha = 0.03f),
                border = Color.White.copy(alpha = 0.12f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Shop,
            contentDescription = null,
            tint = HavenOnSurfaceMuted,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Search \"$query\" in the Play Store",
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            color = HavenOnSurfaceMuted,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.NorthEast,
            contentDescription = null,
            tint = HavenOnSurfaceDim,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * The A–Z rail. Only letters that actually start an app name are listed, and
 * dragging down the rail jumps the list to the first app under the finger.
 */
@Composable
private fun AlphabetRail(
    apps: List<InstalledApp>,
    onLetterSelected: (Int) -> Unit
) {
    val letters = remember(apps) {
        apps.mapIndexed { index, app -> app.initial() to index }
            .distinctBy { it.first }
    }
    if (letters.isEmpty()) {
        Spacer(Modifier.width(96.dp))
        return
    }
    Column(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight()
            .padding(end = 24.dp)
            .pointerInput(letters) {
                fun select(offset: Offset) {
                    val slot = (offset.y / size.height * letters.size)
                        .toInt()
                        .coerceIn(0, letters.size - 1)
                    onLetterSelected(letters[slot].second)
                }
                detectVerticalDragGestures(
                    onDragStart = { offset -> select(offset) }
                ) { change, _ -> select(change.position) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        letters.forEach { (letter, index) ->
            Text(
                text = letter,
                fontSize = 19.sp,
                lineHeight = 28.5.sp,
                fontWeight = FontWeight.Medium,
                color = HavenOnSurface.copy(alpha = 0.35f),
                modifier = Modifier.clickable { onLetterSelected(index) }
            )
        }
    }
}

private fun InstalledApp.initial(): String =
    label.firstOrNull()?.uppercase() ?: "#"

private fun InstalledApp.subtitle(): String? = when {
    isSystem -> "System"
    firstInstallTime > 0 &&
            System.currentTimeMillis() - firstInstallTime < RECENTLY_INSTALLED_WINDOW_MILLIS -> {
        val days = ((System.currentTimeMillis() - firstInstallTime) / (24 * 60 * 60 * 1000.0))
            .roundToInt()
        when (days) {
            0 -> "Installed today"
            1 -> "Installed yesterday"
            else -> "Installed $days days ago"
        }
    }
    else -> null
}

private fun launchPackage(context: Context, packageName: String): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackageCompat(packageName)
        ?: return false
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun android.content.pm.PackageManager.getLaunchIntentForPackageCompat(
    packageName: String
): Intent? = getLaunchIntentForPackage(packageName)
    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun startVoiceSearch(context: Context) {
    listOf(Intent.ACTION_VOICE_COMMAND, Intent.ACTION_ASSIST).any { action ->
        try {
            context.startActivity(Intent(action))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}

private fun searchPlayStore(context: Context, query: String) {
    val encoded = Uri.encode(query)
    val store = Intent(Intent.ACTION_VIEW, "market://search?q=$encoded".toUri())
    try {
        context.startActivity(store)
    } catch (e: ActivityNotFoundException) {
        val web = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/search?q=$encoded".toUri()
        )
        try {
            context.startActivity(web)
        } catch (e: ActivityNotFoundException) {
            // No store and no browser — nothing to open.
        }
    }
}
