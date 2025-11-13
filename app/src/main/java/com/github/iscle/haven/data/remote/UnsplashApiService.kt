package com.github.iscle.haven.data.remote

import com.github.iscle.haven.data.remote.model.UnsplashPhoto
import com.github.iscle.haven.data.remote.model.UnsplashSearchResponse
import com.github.iscle.haven.di.UnsplashClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class UnsplashApiService @Inject constructor(
    @UnsplashClient private val client: HttpClient
) {
    suspend fun searchPhotos(
        query: String = "wallpaper nature",
        page: Int = 1,
        perPage: Int = 20
    ): Result<UnsplashSearchResponse> {
        return try {
            Timber.d("Searching Unsplash photos: query=$query, page=$page, perPage=$perPage")
            val response = client.get("napi/search/photos") {
                parameter("plus", "none")
                parameter("page", page)
                parameter("per_page", perPage)
                parameter("query", query)
            }.body<UnsplashSearchResponse>()
            Timber.d("Unsplash search successful: found ${response.results.size} photos, total=${response.total}, totalPages=${response.totalPages}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search Unsplash photos: query=$query, page=$page")
            Result.failure(e)
        }
    }
    
    suspend fun getRandomPhoto(
        query: String = "wallpaper nature",
        maxRetries: Int = 10,
        initialDelayMs: Long = 500
    ): Result<UnsplashPhoto> {
        var attempt = 0
        
        while (attempt < maxRetries) {
            attempt++
            Timber.d("Getting random photo from Unsplash: query=$query, attempt=$attempt/$maxRetries")
            
            try {
                // First, get page 1 to find out total_pages
                val firstPageResult = searchPhotos(query, page = 1, perPage = 1)
                
                val result = firstPageResult.fold(
                    onSuccess = { firstPageResponse ->
                        if (firstPageResponse.totalPages == 0 || firstPageResponse.results.isEmpty()) {
                            Timber.w("No photos found for query: $query")
                            Result.failure<UnsplashPhoto>(IllegalArgumentException("No photos found for query: $query"))
                        } else {
                            // Pick a random page (1 to total_pages, max 20)
                            val maxPages = minOf(firstPageResponse.totalPages, 20)
                            val randomPage = (1..maxPages).random()
                            Timber.d("Selected random page: $randomPage (max: $maxPages, total: ${firstPageResponse.totalPages})")
                            
                            // Get photos from the random page
                            val searchResult = searchPhotos(query, randomPage, 20)
                            
                            searchResult.fold(
                                onSuccess = { response ->
                                    if (response.results.isEmpty()) {
                                        Timber.w("No photos found on page $randomPage")
                                        Result.failure<UnsplashPhoto>(IllegalArgumentException("No photos found on page $randomPage"))
                                    } else {
                                        // Filter for landscape photos only (width > height)
                                        val landscapePhotos = response.results.filter { it.width > it.height }
                                        
                                        if (landscapePhotos.isEmpty()) {
                                            Timber.d("No landscape photos found on page $randomPage (found ${response.results.size} photos, all portrait/square)")
                                            Result.failure<UnsplashPhoto>(IllegalArgumentException("No landscape photos found on page $randomPage"))
                                        } else {
                                            // Pick a random landscape photo from the results
                                            val randomPhoto = landscapePhotos.random()
                                            Timber.d("Selected random landscape photo: id=${randomPhoto.id}, photographer=${randomPhoto.user.name}, dimensions=${randomPhoto.width}x${randomPhoto.height}")
                                            Result.success(randomPhoto)
                                        }
                                    }
                                },
                                onFailure = { error ->
                                    Timber.e(error, "Failed to get photos from page $randomPage")
                                    Result.failure(error)
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to get first page for query: $query")
                        Result.failure(error)
                    }
                )
                
                // If we got a successful result, return it
                result.fold(
                    onSuccess = { photo -> return Result.success(photo) },
                    onFailure = { error ->
                        // If it's the last attempt, return the failure
                        if (attempt >= maxRetries) {
                            Timber.e(error, "Failed to get landscape photo after $maxRetries attempts")
                            return Result.failure(error)
                        }
                        // Otherwise, wait and retry with exponential backoff
                        // Delay = initialDelay * 2^(attempt-1): 500ms, 1000ms, 2000ms, 4000ms, etc.
                        val delayMs = (initialDelayMs * 2.0.pow(attempt - 1)).toLong()
                        Timber.d("Retrying in ${delayMs}ms (attempt $attempt/$maxRetries)")
                        delay(delayMs)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception while getting random photo: query=$query, attempt=$attempt")
                if (attempt >= maxRetries) {
                    return Result.failure(e)
                }
                // Wait and retry with exponential backoff
                val delayMs = (initialDelayMs * 2.0.pow(attempt - 1)).toLong()
                Timber.d("Retrying after exception in ${delayMs}ms (attempt $attempt/$maxRetries)")
                delay(delayMs)
            }
        }
        
        // Should never reach here, but just in case
        return Result.failure(IllegalStateException("Failed to get landscape photo after $maxRetries attempts"))
    }
}

