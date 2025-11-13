package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.PhotoCacheDataSource
import com.github.iscle.haven.data.remote.UnsplashApiService
import com.github.iscle.haven.data.remote.model.UnsplashPhoto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsplashRepository @Inject constructor(
    private val unsplashApiService: UnsplashApiService,
    private val photoCacheDataSource: PhotoCacheDataSource
) {
    suspend fun getRandomPhoto(
        query: String = "wallpaper nature"
    ): Result<UnsplashPhoto> {
        Timber.d("Repository: Getting random photo, query=$query")
        
        // Try to get from cache first (rotates through cached photos)
        val cachedPhoto = photoCacheDataSource.getRandomCachedPhoto(query)
        if (cachedPhoto != null) {
            Timber.d("Repository: Using cached photo: id=${cachedPhoto.id}")
            return Result.success(cachedPhoto)
        }
        
        // If cache is empty or expired, fetch a new page from API
        Timber.d("Repository: Cache empty, fetching new photos from API")
        return fetchAndCachePhotos(query)
    }
    
    private suspend fun fetchAndCachePhotos(query: String): Result<UnsplashPhoto> {
        // Fetch a page of photos from the API
        val searchResult = unsplashApiService.searchPhotos(query, page = 1, perPage = 20)
        
        return searchResult.fold(
            onSuccess = { response ->
                // Filter for landscape photos only
                val landscapePhotos = response.results.filter { it.width > it.height }
                
                if (landscapePhotos.isEmpty()) {
                    Timber.w("Repository: No landscape photos found in API response")
                    Result.failure(IllegalArgumentException("No landscape photos found"))
                } else {
                    // Cache all the photos
                    photoCacheDataSource.cachePhotos(query, landscapePhotos)
                    Timber.d("Repository: Cached ${landscapePhotos.size} photos from API")
                    
                    // Return a random one
                    val randomPhoto = landscapePhotos.random()
                    Timber.d("Repository: Returning random photo: id=${randomPhoto.id}")
                    Result.success(randomPhoto)
                }
            },
            onFailure = { error ->
                Timber.e(error, "Repository: Failed to fetch photos from API")
                Result.failure(error)
            }
        )
    }
}

