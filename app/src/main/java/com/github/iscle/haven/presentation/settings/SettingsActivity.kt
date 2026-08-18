package com.github.iscle.haven.presentation.settings

import android.content.Context
import android.content.Intent
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

    companion object {
        private const val EXTRA_PANE = "pane"

        /** Open settings straight on one pane, e.g. from the notification stack. */
        fun intent(context: Context, pane: SettingsPane = SettingsPane.DISPLAY): Intent =
            Intent(context, SettingsActivity::class.java).putExtra(EXTRA_PANE, pane.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("SettingsActivity: onCreate")

        // Show the system bars, but draw the dark surface behind them — the
        // screens inset their own content. Otherwise the bars sit as light grey
        // slabs against a near-black UI.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false

        val initialPane = intent.getStringExtra(EXTRA_PANE)
            ?.let { name -> runCatching { SettingsPane.valueOf(name) }.getOrNull() }
            ?: SettingsPane.DISPLAY

        setContent {
            HavenTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    initialPane = initialPane
                )
            }
        }
    }
}
