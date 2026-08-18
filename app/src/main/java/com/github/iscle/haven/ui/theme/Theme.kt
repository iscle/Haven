package com.github.iscle.haven.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Haven is a fixed dark system: a wall clock should not repaint itself with the
// wallpaper palette, so dynamic colour is deliberately not used.
private val HavenColorScheme = darkColorScheme(
    primary = HavenAccent,
    onPrimary = HavenInk,
    secondary = HavenSky,
    onSecondary = HavenInk,
    tertiary = HavenMint,
    onTertiary = HavenInk,
    background = HavenSurface,
    onBackground = HavenOnSurface,
    surface = HavenSurface,
    onSurface = HavenOnSurface,
    surfaceVariant = HavenSurfaceRaised,
    onSurfaceVariant = HavenOnSurfaceMuted,
    surfaceContainer = HavenSurfacePanel,
    outline = HavenGlassBorder,
    outlineVariant = HavenGlassBorderFaint,
    error = HavenHeart,
    onError = HavenInk
)

@Composable
fun HavenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HavenColorScheme,
        typography = Typography,
        content = content
    )
}
