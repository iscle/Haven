package com.github.iscle.haven.presentation.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.github.iscle.haven.ui.theme.HavenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.Forest.d("SettingsActivity: onCreate")

        // Show system bars for normal app experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        Timber.Forest.d("SettingsActivity: System bars shown")

        setContent {
            HavenTheme {
                SettingsScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
        Timber.Forest.d("SettingsActivity: Content set")
    }
}