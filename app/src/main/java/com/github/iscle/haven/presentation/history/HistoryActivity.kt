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

        // Show the system bars, but draw the dark surface behind them — the
        // screens inset their own content. Otherwise the bars sit as light grey
        // slabs against a near-black UI.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
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