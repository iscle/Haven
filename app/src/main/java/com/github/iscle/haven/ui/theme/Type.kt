package com.github.iscle.haven.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.iscle.haven.R

// Rajdhani font family
val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani_light, FontWeight.Light),
    Font(R.font.rajdhani_regular, FontWeight.Normal),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

// Every screen is Rajdhani — the design uses a single family throughout.
private val defaults = Typography()

/**
 * Rajdhani's ascent and descent are generous. Left as-is, the extra font padding
 * makes centred text sit low in its box and bottom-aligned text of different
 * sizes drift apart, so every style trims its line box.
 */
private fun TextStyle.haven(): TextStyle = copy(
    fontFamily = RajdhaniFontFamily,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

val Typography = Typography(
    displayLarge = defaults.displayLarge.haven(),
    displayMedium = defaults.displayMedium.haven(),
    displaySmall = defaults.displaySmall.haven(),
    headlineLarge = defaults.headlineLarge.haven(),
    headlineMedium = defaults.headlineMedium.haven(),
    headlineSmall = defaults.headlineSmall.haven(),
    titleLarge = defaults.titleLarge.haven(),
    titleMedium = defaults.titleMedium.haven(),
    titleSmall = defaults.titleSmall.haven(),
    // The default style every bare Text() inherits. Line height and tracking are
    // left unset so the display sizes used across Haven are not clamped by it.
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ).haven(),
    bodyMedium = defaults.bodyMedium.haven(),
    bodySmall = defaults.bodySmall.haven(),
    labelLarge = defaults.labelLarge.haven(),
    labelMedium = defaults.labelMedium.haven(),
    labelSmall = defaults.labelSmall.haven()
)
