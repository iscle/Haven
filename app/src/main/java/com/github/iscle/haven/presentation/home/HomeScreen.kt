package com.github.iscle.haven.presentation.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.Weather
import com.github.iscle.haven.presentation.history.HistoryActivity
import com.github.iscle.haven.presentation.settings.SettingsActivity
import com.github.iscle.haven.ui.theme.RajdhaniFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

// Text shadow style for better readability over backgrounds
private val textShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(2f, 2f),
    blurRadius = 8f
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Launch activities directly instead of using navigation
    val launchSettings = {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }
    
    val launchHistory = {
        val intent = Intent(context, HistoryActivity::class.java)
        context.startActivity(intent)
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
                .padding(48.dp)
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
                .padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // History button
            Box {
                // Shadow layer
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(48.dp)
                        .offset(x = 3.dp, y = 3.dp)
                )
                // Main icon
                IconButton(
                    onClick = launchHistory,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Wallpaper History",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
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
                        .size(48.dp)
                        .offset(x = 3.dp, y = 3.dp)
                )
                // Main icon
                IconButton(
                    onClick = launchSettings,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Photographer credit at bottom with smooth fade animation
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = displayedImage,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(1000)
                    ) togetherWith fadeOut(
                        animationSpec = tween(1000)
                    )
                },
                label = "photographer_credit_transition"
            ) { image ->
                if (image != null) {
                    Text(
                        text = "Photo by ${image.photographer}",
                        fontSize = 18.sp,
                        style = TextStyle(
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
    weather: Weather?,
    modifier: Modifier = Modifier
) {
    weather?.let {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Main temperature and location row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${it.feelsLike.roundToInt()}°",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(shadow = textShadow)
                )
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text(
                        text = it.cityName,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        style = TextStyle(shadow = textShadow)
                    )
                    Text(
                        text = it.description.replaceFirstChar { char -> char.uppercase() },
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }
            
            // Actual temperature and humidity with icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actual temperature with thermometer icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Temperature",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(27.dp)
                    )
                    Text(
                        text = "${it.temperature.roundToInt()}°",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        style = TextStyle(shadow = textShadow)
                    )
                }
                
                // Humidity with water drop icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Humidity",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(27.dp)
                    )
                    Text(
                        text = "${it.humidity}%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
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
            fontSize = 180.sp,
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
            fontSize = 48.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = RajdhaniFontFamily,
            color = Color.White.copy(alpha = 0.95f),
            style = TextStyle(shadow = textShadow)
        )
    }
}

