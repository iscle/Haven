package com.github.iscle.haven.domain.model

data class WallpaperHistory(
    val image: BackgroundImage,
    val shownAt: Long, // Timestamp
    val isFavorite: Boolean = false,
    val timesShown: Int = 1
)

