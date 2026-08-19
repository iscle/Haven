package com.github.iscle.haven.presentation.history

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.iscle.haven.domain.model.WallpaperHistory
import com.github.iscle.haven.presentation.common.FlatIconButton
import com.github.iscle.haven.presentation.common.HavenFilledButton
import com.github.iscle.haven.presentation.common.HavenMotion
import com.github.iscle.haven.presentation.common.HavenOutlinedButton
import com.github.iscle.haven.presentation.common.SegmentedToggle
import com.github.iscle.haven.presentation.common.formatDateTime
import com.github.iscle.haven.presentation.common.formatShortDate
import com.github.iscle.haven.ui.theme.HavenHeart
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted
import com.github.iscle.haven.ui.theme.HavenSurface
import com.github.iscle.haven.ui.theme.HavenSurfacePanel

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selected = uiState.selectedImage

    // Leave only once the pinned wallpaper has actually been stored.
    LaunchedEffect(uiState.clockUpdated) {
        if (uiState.clockUpdated) onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenSurface)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryHeader(
                filter = uiState.filter,
                showFilter = uiState.history.isNotEmpty(),
                onSelectFilter = viewModel::setFilter,
                onNavigateBack = onNavigateBack
            )

            val visible = uiState.visibleHistory
            if (visible.isEmpty() && !uiState.isLoading) {
                EmptyHistory(filter = uiState.filter)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = visible, key = { it.image.id }) { history ->
                        HistoryGridItem(
                            history = history,
                            onClick = { viewModel.showDetails(history) },
                            onFavoriteClick = { viewModel.toggleFavorite(history.image.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // 2D — on a landscape display the details sheet is a side sheet, so it
        // arrives from the right rather than appearing on the spot.
        AnimatedVisibility(
            visible = uiState.showDetails && selected != null,
            enter = fadeIn(tween(HavenMotion.SCREEN_MILLIS)) +
                    slideInHorizontally(tween(HavenMotion.SCREEN_MILLIS)) { it / 3 },
            exit = fadeOut(tween(HavenMotion.CONTROL_MILLIS))
        ) {
            if (selected != null) {
                PhotoDetails(
                history = selected,
                onDismiss = viewModel::hideDetails,
                onFavoriteClick = { viewModel.toggleFavorite(selected.image.id) },
                onShowOnClock = { viewModel.showOnClock(selected.image) },
                onOpenUnsplash = { url -> openUrl(context, url) },
                    onOpenArtistProfile = { url -> openUrl(context, url) }
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    filter: HistoryFilter,
    showFilter: Boolean,
    onSelectFilter: (HistoryFilter) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FlatIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onNavigateBack
                )
                Text(
                    text = "Wallpaper History",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.34.sp,
                    color = HavenOnSurface
                )
            }
            if (showFilter) {
                SegmentedToggle(
                    options = listOf("All", "Favorites"),
                    selectedIndex = if (filter == HistoryFilter.ALL) 0 else 1,
                    onSelect = { index ->
                        onSelectFilter(if (index == 0) HistoryFilter.ALL else HistoryFilter.FAVORITES)
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.07f))
        )
    }
}

/** 2C — the empty state, copy unchanged from the original screen. */
@Composable
private fun EmptyHistory(filter: HistoryFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = HavenOnSurface.copy(alpha = 0.18f),
            modifier = Modifier
                .size(76.dp)
                .padding(bottom = 14.dp)
        )
        Text(
            text = if (filter == HistoryFilter.FAVORITES) "No favorites yet" else "No history yet",
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
            color = HavenOnSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (filter == HistoryFilter.FAVORITES) {
                "Wallpapers you favourite will appear here"
            } else {
                "Wallpapers you've seen will appear here"
            },
            fontSize = 22.sp,
            color = HavenOnSurface.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun HistoryGridItem(
    history: WallpaperHistory,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(history.image.url)
                .crossfade(HavenMotion.SCREEN_MILLIS)
                .build(),
            contentDescription = history.image.photographer,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.4f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onFavoriteClick),
            contentAlignment = Alignment.Center
        ) {
            val heartTint by animateColorAsState(
                targetValue = if (history.isFavorite) HavenHeart else Color.White,
                animationSpec = tween(HavenMotion.CONTROL_MILLIS),
                label = "heart_tint"
            )
            Icon(
                imageVector = if (history.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (history.isFavorite) {
                    "Remove from favorites"
                } else {
                    "Add to favorites"
                },
                tint = heartTint,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = history.image.photographer,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatShortDate(history.shownAt),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/** 2D — photo on the left, everything known about it on the right. */
@Composable
private fun PhotoDetails(
    history: WallpaperHistory,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShowOnClock: () -> Unit,
    onOpenUnsplash: (String) -> Unit,
    onOpenArtistProfile: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenSurface)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AsyncImage(
                model = history.image.url,
                contentDescription = history.image.photographer,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            OverlayIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OverlayIconButton(
                    icon = if (history.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (history.isFavorite) {
                        "Remove from favorites"
                    } else {
                        "Add to favorites"
                    },
                    onClick = onFavoriteClick,
                    tint = if (history.isFavorite) HavenHeart else Color.White
                )
                OverlayIconButton(
                    icon = Icons.Default.Wallpaper,
                    contentDescription = "Show on clock now",
                    onClick = onShowOnClock
                )
            }
        }

        Column(
            modifier = Modifier
                .width(470.dp)
                .fillMaxHeight()
                .background(HavenSurfacePanel)
                .padding(horizontal = 36.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            val profileUrl = history.image.artistProfileUrl
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .then(
                        if (!profileUrl.isNullOrBlank()) {
                            Modifier.clickable { onOpenArtistProfile(profileUrl) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val profileImage = history.image.profileImageUrl
                if (!profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImage,
                        contentDescription = "Photographer",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = HavenOnSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = history.image.photographer,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = HavenOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${history.image.photographerUsername}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface.copy(alpha = 0.65f)
                    )
                }
                if (!profileUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View profile",
                        tint = HavenOnSurfaceMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailRow(
                    icon = Icons.Default.Schedule,
                    text = "Shown on ${formatDateTime(history.shownAt)}"
                )
                DetailRow(
                    icon = Icons.Default.Visibility,
                    text = "Shown ${history.timesShown} ${if (history.timesShown == 1) "time" else "times"}"
                )
                DetailRow(
                    icon = if (history.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = if (history.isFavorite) "In favorites" else "Not in favorites"
                )
            }

            Spacer(Modifier.weight(1f))

            val unsplashUrl = history.image.unsplashUrl
            if (!unsplashUrl.isNullOrBlank()) {
                HavenOutlinedButton(
                    text = "View on Unsplash",
                    onClick = { onOpenUnsplash(unsplashUrl) },
                    icon = Icons.Default.Link,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HavenFilledButton(
                text = "Show on clock now",
                onClick = onShowOnClock,
                icon = Icons.Default.Wallpaper,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HavenOnSurfaceMuted,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            color = HavenOnSurfaceMuted
        )
    }
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        // No browser on the device.
    }
}
