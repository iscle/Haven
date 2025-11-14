package com.github.iscle.haven.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_history")
data class WallpaperHistoryEntity(
    @PrimaryKey
    val imageId: String,
    val url: String,
    val photographer: String,
    val photographerUsername: String,
    val shownAt: Long,
    val isFavorite: Boolean = false,
    val timesShown: Int = 1
)

