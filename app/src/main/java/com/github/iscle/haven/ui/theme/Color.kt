package com.github.iscle.haven.ui.theme

import androidx.compose.ui.graphics.Color

// Haven surfaces — one dark system, shared by every screen that is not a photo.
val HavenSurface = Color(0xFF0E1014)
val HavenSurfaceRaised = Color(0xFF12141A)
val HavenSurfacePanel = Color(0xFF14161B)
val HavenInk = Color(0xFF14161A)

// Text on the dark system
val HavenOnSurface = Color(0xFFECEFF3)
val HavenOnSurfaceMuted = Color(0xFFECEFF3).copy(alpha = 0.62f)
val HavenOnSurfaceFaint = Color(0xFFECEFF3).copy(alpha = 0.45f)
val HavenOnSurfaceDim = Color(0xFFECEFF3).copy(alpha = 0.38f)

// Accents
val HavenAccent = Color(0xFFF3C98B)
val HavenMint = Color(0xFF9FE0BB)
val HavenSky = Color(0xFFA9C2F2)
val HavenLilac = Color(0xFFCBB2F0)
val HavenCyan = Color(0xFF9DD3E8)
val HavenCoral = Color(0xFFF0A8A8)
val HavenAsh = Color(0xFFD8D3DD)
val HavenHeart = Color(0xFFFF5C5C)
val HavenVoice = Color(0xFF7FD1FF)

// Warm ink used by the night/ambient clock
val HavenNightInk = Color(0xFFFFECD6)

// Glass — translucent panels that sit on top of a photo
val HavenGlassFill = Color(0xFF0E1016).copy(alpha = 0.30f)
val HavenGlassFillStrong = Color(0xFF0E1016).copy(alpha = 0.42f)
val HavenGlassBorder = Color.White.copy(alpha = 0.13f)
val HavenGlassBorderFaint = Color.White.copy(alpha = 0.08f)
val HavenGlassHighlight = Color.White.copy(alpha = 0.16f)
