package com.github.iscle.haven.data.local

import com.github.iscle.haven.data.local.database.dao.PhotoCacheDao
import com.github.iscle.haven.data.local.database.entity.CacheMetadataEntity
import com.github.iscle.haven.data.local.database.entity.PhotoCacheEntity
import com.github.iscle.haven.data.local.database.entity.ShownPhotoEntity
import com.github.iscle.haven.domain.model.BackgroundImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoCacheDataSource @Inject constructor(
    private val photoCacheDao: PhotoCacheDao
) {
    companion object {
        private const val CACHE_EXPIRY_HOURS = 24L
    }

    /**
     * Get a random cached photo that hasn't been shown yet
     * Returns null if no photos available
     * Removes photos from cache after they're shown (favorites are handled separately)
     */
    suspend fun getRandomCachedPhoto(query: String): BackgroundImage? {
        return withContext(Dispatchers.IO) {
            val cacheKey = query.lowercase().trim()
            
            // Check if cache exists and is not expired
            val metadata = photoCacheDao.getCacheMetadata(cacheKey)
            if (metadata == null || isExpired(metadata.cachedAt)) {
                if (metadata != null) {
                    Timber.d("Photo cache expired: query=$query")
                    // Delete metadata - cascade will automatically delete related photos and shown photos
                    photoCacheDao.deleteCacheMetadata(cacheKey)
                }
                Timber.d("Photo cache miss or empty: query=$query")
                return@withContext null
            }
            
            // Get unshown photos directly from database
            val unshownPhotos = photoCacheDao.getUnshownPhotosByQuery(cacheKey)
            
            if (unshownPhotos.isEmpty()) {
                Timber.d("No unshown photos for query: $query")
                return@withContext null
            }
            
            // Pick a random unshown photo
            val randomPhotoEntity = unshownPhotos.random()
            val randomPhoto = mapToBackgroundImage(randomPhotoEntity)
            
            // Mark as shown
            val shownPhotoEntity = ShownPhotoEntity(
                id = "${cacheKey}_${randomPhotoEntity.id}",
                cacheKey = cacheKey,
                photoId = randomPhotoEntity.id,
                shownAt = System.currentTimeMillis()
            )
            photoCacheDao.insertShownPhoto(shownPhotoEntity)
            
            // Remove photo from cache after showing
            // Deleting the photo will cascade delete the shown photo record
            photoCacheDao.deletePhoto(randomPhotoEntity.id, cacheKey)
            
            val remainingCount = photoCacheDao.getPhotoCount(cacheKey)
            Timber.d("Photo cache hit: query=$query, id=${randomPhoto.id}, remaining=$remainingCount")
            
            randomPhoto
        }
    }

    /**
     * Check if cache has photos for the given query
     */
    suspend fun hasCachedPhotos(query: String): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheKey = query.lowercase().trim()
            val metadata = photoCacheDao.getCacheMetadata(cacheKey)
            if (metadata == null || isExpired(metadata.cachedAt)) {
                return@withContext false
            }
            val count = photoCacheDao.getPhotoCount(cacheKey)
            count > 0
        }
    }

    /**
     * Cache multiple photos for the given query (replaces existing cache)
     */
    suspend fun cachePhotos(query: String, photos: List<BackgroundImage>) {
        withContext(Dispatchers.IO) {
            if (photos.isEmpty()) {
                Timber.w("Attempted to cache empty photo list for query=$query")
                return@withContext
            }
            
            val cacheKey = query.lowercase().trim()
            val cachedAt = System.currentTimeMillis()
            
            // Delete existing metadata - cascade will automatically delete related photos and shown photos
            photoCacheDao.deleteCacheMetadata(cacheKey)
            
            // Insert new cache metadata first (required for FK constraint)
            val metadata = CacheMetadataEntity(
                cacheKey = cacheKey,
                cachedAt = cachedAt
            )
            photoCacheDao.insertCacheMetadata(metadata)
            
            // Convert to entities and insert photos
            val photoEntities = photos.map { mapToPhotoCacheEntity(it, cacheKey, cachedAt) }
            photoCacheDao.insertPhotos(photoEntities)
            
            Timber.d("Cached ${photos.size} photos for query=$query")
        }
    }

    /**
     * Clear all cached photos
     * Deleting all metadata will cascade delete all related photos and shown photos
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                photoCacheDao.clearAllCacheMetadata()
                Timber.d("Photo cache cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear photo cache")
            }
        }
    }

    private fun isExpired(cachedAt: Long): Boolean {
        val expiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000
        return (System.currentTimeMillis() - cachedAt) > expiryTime
    }
    
    private fun mapToPhotoCacheEntity(
        image: BackgroundImage,
        cacheKey: String,
        cachedAt: Long
    ): PhotoCacheEntity {
        return PhotoCacheEntity(
            id = image.id,
            cacheKey = cacheKey,
            url = image.url,
            photographer = image.photographer,
            photographerUsername = image.photographerUsername,
            cachedAt = cachedAt
        )
    }
    
    private fun mapToBackgroundImage(entity: PhotoCacheEntity): BackgroundImage {
        return BackgroundImage(
            id = entity.id,
            url = entity.url,
            photographer = entity.photographer,
            photographerUsername = entity.photographerUsername
        )
    }
}

