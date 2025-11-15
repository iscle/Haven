package com.github.iscle.haven.presentation.history

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.github.iscle.haven.ui.theme.HavenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.Forest.d("HistoryActivity: onCreate")

        // Show system bars for normal app experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        Timber.Forest.d("HistoryActivity: System bars shown")

        setContent {
            HavenTheme {
                HistoryScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
        Timber.Forest.d("HistoryActivity: Content set")
    }
}