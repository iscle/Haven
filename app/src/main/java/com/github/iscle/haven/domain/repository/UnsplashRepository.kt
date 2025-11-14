package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.WallpaperHistory
import kotlinx.coroutines.flow.Flow

interface UnsplashRepository {
    suspend fun getRandomPhoto(
        query: String = "wallpaper nature",
        includeFavorites: Boolean = true
    ): Result<BackgroundImage>
    
    suspend fun addToHistory(image: BackgroundImage)
    
    fun getHistory(): Flow<List<WallpaperHistory>>
    
    suspend fun toggleFavorite(imageId: String): Boolean
    
    suspend fun isFavorite(imageId: String): Boolean
    
    suspend fun getFavorites(): List<BackgroundImage>
}

