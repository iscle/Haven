package com.github.iscle.haven.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.iscle.haven.presentation.home.HomeScreen
import com.github.iscle.haven.presentation.settings.SettingsScreen
import timber.log.Timber

@Composable
fun HavenNavHost(
    navController: NavHostController
) {
    Timber.d("HavenNavHost: Setting up navigation")
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            Timber.d("Navigation: Navigating to HomeRoute")
            HomeScreen(
                onNavigateToSettings = {
                    Timber.d("Navigation: Navigating to SettingsRoute")
                    navController.navigate(SettingsRoute)
                }
            )
        }
        
        composable<SettingsRoute> {
            Timber.d("Navigation: Navigating to SettingsRoute")
            SettingsScreen(
                onNavigateBack = {
                    Timber.d("Navigation: Navigating back from SettingsRoute")
                    navController.popBackStack()
                }
            )
        }
    }
}

