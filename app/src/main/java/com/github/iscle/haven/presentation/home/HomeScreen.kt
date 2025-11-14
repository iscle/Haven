package com.github.iscle.haven.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.iscle.haven.ui.theme.RajdhaniFontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import coil.compose.LocalImageLoader
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.SuccessResult
import com.github.iscle.haven.domain.model.BackgroundImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Text shadow style for better readability over backgrounds
private val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
    blurRadius = 8f
)

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val uiState by viewModel.uiState.collectAsState()
    
    // Track the displayed image (only updates when new image is fully loaded)
    var displayedImage by remember { mutableStateOf<BackgroundImage?>(null) }
    val newImage = uiState.backgroundImage
    
    // Initialize displayedImage on first load
    LaunchedEffect(newImage?.url) {
        if (newImage != null && displayedImage == null) {
            displayedImage = newImage
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
        
        // Background Image with smooth crossfade transition
        // Only animates when displayedImage changes (after new image is fully loaded)
        AnimatedContent(
            targetState = displayedImage,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(1000, delayMillis = 0)
                ) togetherWith fadeOut(
                    animationSpec = tween(1000, delayMillis = 0)
                )
            },
            label = "background_transition"
        ) { image ->
            if (image != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.url)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .networkCachePolicy(coil.request.CachePolicy.ENABLED)
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

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // Weather at top left
        WeatherWidget(
            weather = uiState.weather,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
        )

        // Time and Date at center
        ClockWidget(
            modifier = Modifier
                .align(Alignment.Center)
        )

        // Settings and History buttons at top right with shadow effect
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // History button
            Box {
                // Shadow layer
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 2.dp, y = 2.dp)
                )
                // Main icon
                IconButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = "Wallpaper History",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Settings button
            Box {
                // Shadow layer
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = 2.dp, y = 2.dp)
                )
                // Main icon
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Photographer credit at bottom with smooth fade animation
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = displayedImage,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(1000, delayMillis = 0)
                    ) togetherWith fadeOut(
                        animationSpec = tween(1000, delayMillis = 0)
                    )
                },
                label = "photographer_credit_transition"
            ) { image ->
                if (image != null) {
                    Text(
                        text = "Photo by ${image.photographer}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = textShadow
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else {
                    // Empty space to maintain layout
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
fun WeatherWidget(
    weather: com.github.iscle.haven.domain.model.Weather?,
    modifier: Modifier = Modifier
) {
    weather?.let {
        Column(
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${it.temperature.roundToInt()}°",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(shadow = textShadow)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = it.cityName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        style = TextStyle(shadow = textShadow)
                    )
                    Text(
                        text = it.description.replaceFirstChar { char -> char.uppercase() },
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }
        }
    }
}

@Composable
fun ClockWidget(
    modifier: Modifier = Modifier
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Update time every minute
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = System.currentTimeMillis()
            val nextMinute = ((now / 60000) + 1) * 60000
            val delay = nextMinute - now
            delay(delay)
            calendar = Calendar.getInstance()
        }
    }
    
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val dayOfWeekName = calendar.getDisplayName(
        Calendar.DAY_OF_WEEK,
        Calendar.LONG,
        Locale.getDefault()
    )!!
    val monthName = calendar.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale.getDefault()
    )!!
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time with shadow
        Text(
            text = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hour, minute
            ),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RajdhaniFontFamily,
            color = Color.White,
            style = TextStyle(shadow = textShadow)
        )
        
        // Date with shadow
        val dayOfWeek = dayOfWeekName.lowercase().replaceFirstChar { it.uppercase() }
        val month = monthName.lowercase().replaceFirstChar { it.uppercase() }
        val dateText = "$dayOfWeek, $month $dayOfMonth"
        
        Text(
            text = dateText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = RajdhaniFontFamily,
            color = Color.White.copy(alpha = 0.95f),
            style = TextStyle(shadow = textShadow)
        )
    }
}

