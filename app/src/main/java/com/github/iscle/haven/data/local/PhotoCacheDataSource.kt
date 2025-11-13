package com.github.iscle.haven.data.local

import android.content.Context
import com.github.iscle.haven.data.remote.model.UnsplashPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class PhotoCacheEntry(
    val photos: List<UnsplashPhoto>,
    val cachedAt: Long,
    val query: String,
    val shownPhotoIds: Set<String> = emptySet()
)

@Singleton
class PhotoCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    companion object {
        private const val CACHE_DIR = "photo_cache"
        private const val CACHE_EXPIRY_HOURS = 24L
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // In-memory cache: query -> (photos, shownIds)
    private val inMemoryCache = mutableMapOf<String, Pair<List<UnsplashPhoto>, MutableSet<String>>>()

    init {
        loadCacheFromDisk()
    }

    /**
     * Get a random cached photo that hasn't been shown yet
     * Returns null if no photos available or all have been shown
     */
    suspend fun getRandomCachedPhoto(query: String): UnsplashPhoto? {
        val cacheKey = query.lowercase().trim()
        
        // Check in-memory cache first
        val cached = inMemoryCache[cacheKey]
        if (cached != null) {
            val (photos, shownIds) = cached
            val unshownPhotos = photos.filter { it.id !in shownIds }
            
            if (unshownPhotos.isNotEmpty()) {
                val randomPhoto = unshownPhotos.random()
                shownIds.add(randomPhoto.id)
                Timber.d("Photo cache hit (memory): query=$query, id=${randomPhoto.id}, remaining=${unshownPhotos.size - 1}")
                saveCacheToDisk(cacheKey, photos, shownIds)
                return randomPhoto
            } else {
                // All photos shown, reset and start over
                Timber.d("All cached photos shown for query=$query, resetting rotation")
                shownIds.clear()
                if (photos.isNotEmpty()) {
                    val randomPhoto = photos.random()
                    shownIds.add(randomPhoto.id)
                    saveCacheToDisk(cacheKey, photos, shownIds)
                    return randomPhoto
                }
            }
        }

        // Check disk cache
        val file = File(cacheDir, "$cacheKey.json")
        if (file.exists()) {
            try {
                val cacheEntry = json.decodeFromString<PhotoCacheEntry>(file.readText())
                if (!isExpired(cacheEntry.cachedAt)) {
                    val photos = cacheEntry.photos
                    val shownIds = cacheEntry.shownPhotoIds.toMutableSet()
                    val unshownPhotos = photos.filter { it.id !in shownIds }
                    
                    if (unshownPhotos.isNotEmpty()) {
                        val randomPhoto = unshownPhotos.random()
                        shownIds.add(randomPhoto.id)
                        Timber.d("Photo cache hit (disk): query=$query, id=${randomPhoto.id}, remaining=${unshownPhotos.size - 1}")
                        // Update in-memory cache
                        inMemoryCache[cacheKey] = photos to shownIds
                        saveCacheToDisk(cacheKey, photos, shownIds)
                        return randomPhoto
                    } else {
                        // All photos shown, reset
                        Timber.d("All cached photos shown for query=$query, resetting rotation")
                        shownIds.clear()
                        if (photos.isNotEmpty()) {
                            val randomPhoto = photos.random()
                            shownIds.add(randomPhoto.id)
                            inMemoryCache[cacheKey] = photos to shownIds
                            saveCacheToDisk(cacheKey, photos, shownIds)
                            return randomPhoto
                        }
                    }
                } else {
                    Timber.d("Photo cache expired: query=$query")
                    file.delete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read cached photos: query=$query")
                file.delete()
            }
        }

        Timber.d("Photo cache miss or empty: query=$query")
        return null
    }

    /**
     * Check if cache has photos for the given query
     */
    suspend fun hasCachedPhotos(query: String): Boolean {
        val cacheKey = query.lowercase().trim()
        val cached = inMemoryCache[cacheKey]
        if (cached != null && cached.first.isNotEmpty()) {
            return true
        }
        
        val file = File(cacheDir, "$cacheKey.json")
        if (file.exists()) {
            try {
                val cacheEntry = json.decodeFromString<PhotoCacheEntry>(file.readText())
                return !isExpired(cacheEntry.cachedAt) && cacheEntry.photos.isNotEmpty()
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    /**
     * Cache multiple photos for the given query (replaces existing cache)
     */
    suspend fun cachePhotos(query: String, photos: List<UnsplashPhoto>) {
        if (photos.isEmpty()) {
            Timber.w("Attempted to cache empty photo list for query=$query")
            return
        }
        
        val cacheKey = query.lowercase().trim()
        val shownIds = mutableSetOf<String>()
        
        // Update in-memory cache
        inMemoryCache[cacheKey] = photos to shownIds
        
        // Save to disk
        saveCacheToDisk(cacheKey, photos, shownIds)
        Timber.d("Cached ${photos.size} photos for query=$query")
    }

    /**
     * Clear all cached photos
     */
    suspend fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            inMemoryCache.clear()
            Timber.d("Photo cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear photo cache")
        }
    }

    private fun isExpired(cachedAt: Long): Boolean {
        val expiryTime = CACHE_EXPIRY_HOURS * 60 * 60 * 1000
        return (System.currentTimeMillis() - cachedAt) > expiryTime
    }

    private suspend fun saveCacheToDisk(cacheKey: String, photos: List<UnsplashPhoto>, shownIds: Set<String>) {
        val file = File(cacheDir, "$cacheKey.json")
        try {
            val cacheEntry = PhotoCacheEntry(
                photos = photos,
                cachedAt = System.currentTimeMillis(),
                query = cacheKey,
                shownPhotoIds = shownIds
            )
            file.writeText(json.encodeToString(cacheEntry))
        } catch (e: IOException) {
            Timber.e(e, "Failed to save cached photos: query=$cacheKey")
        }
    }

    private fun loadCacheFromDisk() {
        try {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    try {
                        val cacheEntry = json.decodeFromString<PhotoCacheEntry>(file.readText())
                        if (!isExpired(cacheEntry.cachedAt) && cacheEntry.photos.isNotEmpty()) {
                            val key = file.nameWithoutExtension
                            inMemoryCache[key] = cacheEntry.photos to cacheEntry.shownPhotoIds.toMutableSet()
                        } else {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load cached photos: ${file.name}")
                        file.delete()
                    }
                }
            }
            Timber.d("Loaded ${inMemoryCache.size} cached photo sets from disk")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cache from disk")
        }
    }
}

