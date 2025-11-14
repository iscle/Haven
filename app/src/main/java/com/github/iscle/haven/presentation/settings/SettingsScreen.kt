package com.github.iscle.haven.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val currentIntervalIndex = remember(uiState.backgroundIntervalSeconds) {
        findClosestIntervalIndex(uiState.backgroundIntervalSeconds)
    }
    var selectedIndex by remember { mutableIntStateOf(currentIntervalIndex) }
    
    // Update selected index when uiState changes
    LaunchedEffect(uiState.backgroundIntervalSeconds) {
        selectedIndex = findClosestIntervalIndex(uiState.backgroundIntervalSeconds)
    }
    
    var cityNameInput by remember { mutableStateOf(uiState.cityName) }

    // Update local state when uiState changes
    if (uiState.cityName != cityNameInput && cityNameInput.isEmpty()) {
        cityNameInput = uiState.cityName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Background Interval Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Background Change Interval",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatInterval(backgroundIntervals[selectedIndex]),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Set how often the background image changes (10s - 24h)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // City Name Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "City for Weather",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cityNameInput,
                            onValueChange = { cityNameInput = it },
                            label = { Text("City Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Button(
                            onClick = {
                                if (cityNameInput.isNotBlank()) {
                                    viewModel.setCityName(cityNameInput)
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter the name of your city to get weather information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

