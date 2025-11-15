package com.github.iscle.haven.data.local

import com.github.iscle.haven.data.local.database.dao.WallpaperHistoryDao
import com.github.iscle.haven.data.local.database.entity.WallpaperHistoryEntity
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.WallpaperHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryDataSource @Inject constructor(
    private val wallpaperHistoryDao: WallpaperHistoryDao
) {
    companion object {
        private const val MAX_HISTORY_SIZE = 1000 // Maximum number of history entries
    }

    /**
     * Add a wallpaper to history
     * If the image already exists, increment the counter and update the timestamp
     */
    suspend fun addToHistory(image: BackgroundImage) {
        withContext(Dispatchers.IO) {
            val existing = wallpaperHistoryDao.getHistoryByImageId(image.id)
            val currentTime = System.currentTimeMillis()
            
            if (existing != null) {
                // Increment counter and update timestamp
                wallpaperHistoryDao.incrementTimesShown(image.id, currentTime)
                Timber.d("Incremented view count for id=${image.id}: ${existing.timesShown + 1} times")
            } else {
                // New entry
                val entity = WallpaperHistoryEntity(
                    imageId = image.id,
                    url = image.url,
                    photographer = image.photographer,
                    photographerUsername = image.photographerUsername,
                    unsplashUrl = image.unsplashUrl,
                    artistProfileUrl = image.artistProfileUrl,
                    profileImageUrl = image.profileImageUrl,
                    shownAt = currentTime,
                    isFavorite = false,
                    timesShown = 1
                )
                wallpaperHistoryDao.insert(entity)
                Timber.d("Added to history: id=${image.id}, photographer=${image.photographer}")
            }
            
            // Limit history size
            val count = wallpaperHistoryDao.getHistoryCount()
            if (count > MAX_HISTORY_SIZE) {
                // Get oldest entries and delete them
                val allHistory = wallpaperHistoryDao.getAllHistoryList()
                if (allHistory.size > MAX_HISTORY_SIZE) {
                    val toDelete = allHistory.takeLast(allHistory.size - MAX_HISTORY_SIZE)
                    toDelete.forEach { wallpaperHistoryDao.delete(it) }
                    Timber.d("Deleted ${toDelete.size} old history entries to maintain size limit")
                }
            }
        }
    }

    /**
     * Get all history entries (most recent first)
     */
    fun getHistory(): Flow<List<WallpaperHistory>> {
        return wallpaperHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Get favorite wallpapers
     */
    suspend fun getFavorites(): List<BackgroundImage> {
        return withContext(Dispatchers.IO) {
            val favorites = wallpaperHistoryDao.getFavorites()
            favorites.map { it.toBackgroundImage() }
        }
    }

    /**
     * Toggle favorite status for a wallpaper
     */
    suspend fun toggleFavorite(imageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = wallpaperHistoryDao.getHistoryByImageId(imageId)
            if (existing != null) {
                val newFavoriteStatus = !existing.isFavorite
                wallpaperHistoryDao.updateFavoriteStatus(imageId, newFavoriteStatus)
                Timber.d("Toggled favorite for id=$imageId: $newFavoriteStatus")
                newFavoriteStatus
            } else {
                Timber.w("Cannot toggle favorite: image not found in history: $imageId")
                false
            }
        }
    }

    /**
     * Check if a wallpaper is favorite
     */
    suspend fun isFavorite(imageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = wallpaperHistoryDao.getHistoryByImageId(imageId)
            existing?.isFavorite ?: false
        }
    }

    /**
     * Clear all history
     */
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            wallpaperHistoryDao.clearAll()
            Timber.d("History cleared")
        }
    }
    
    private fun WallpaperHistoryEntity.toDomainModel(): WallpaperHistory {
        return WallpaperHistory(
            image = BackgroundImage(
                id = imageId,
                url = url,
                photographer = photographer,
                photographerUsername = photographerUsername,
                unsplashUrl = unsplashUrl,
                artistProfileUrl = artistProfileUrl,
                profileImageUrl = profileImageUrl
            ),
            shownAt = shownAt,
            isFavorite = isFavorite,
            timesShown = timesShown
        )
    }
    
    private fun WallpaperHistoryEntity.toBackgroundImage(): BackgroundImage {
        return BackgroundImage(
            id = imageId,
            url = url,
            photographer = photographer,
            photographerUsername = photographerUsername,
            unsplashUrl = unsplashUrl,
            artistProfileUrl = artistProfileUrl,
            profileImageUrl = profileImageUrl
        )
    }
}

