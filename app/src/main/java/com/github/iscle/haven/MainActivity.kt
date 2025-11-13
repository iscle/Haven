package com.github.iscle.haven

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.github.iscle.haven.presentation.navigation.HavenNavHost
import com.github.iscle.haven.ui.theme.HavenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var imageLoader: ImageLoader
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity: onCreate")
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Timber.d("MainActivity: Screen keep-on flag set")
        
        // Hide system bars for immersive experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        Timber.d("MainActivity: System bars hidden for immersive experience")
        
        setContent {
            HavenTheme {
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    HavenApp()
                }
            }
        }
        Timber.d("MainActivity: Content set")
    }
}

@Composable
fun HavenApp() {
    val navController = rememberNavController()
    
    HavenNavHost(navController = navController)
}