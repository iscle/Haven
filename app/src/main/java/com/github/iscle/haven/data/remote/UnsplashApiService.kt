package com.github.iscle.haven.data.remote

import com.github.iscle.haven.data.remote.model.UnsplashSearchResponse
import com.github.iscle.haven.di.UnsplashClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class UnsplashApiService @Inject constructor(
    @UnsplashClient private val client: HttpClient
) {
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 500L
        private const val MAX_DELAY_MS = 10000L // 10 seconds max delay
        private const val JITTER_FACTOR = 0.1 // 10% jitter
    }

    suspend fun searchPhotos(
        query: String,
        page: Int,
        perPage: Int
    ): Result<UnsplashSearchResponse> {
        var attempt = 0
        
        while (attempt <= MAX_RETRIES) {
            try {
                Timber.d("Searching Unsplash photos: query=$query, page=$page, perPage=$perPage, attempt=${attempt + 1}/${MAX_RETRIES + 1}")
                
                val response = client.get("napi/search/photos") {
                    parameter("plus", "none")
                    parameter("page", page)
                    parameter("per_page", perPage)
                    parameter("query", query)
                }
                
                // Check HTTP status code before reading body
                val statusCode = response.status
                if (statusCode.isSuccess()) {
                    val body = response.body<UnsplashSearchResponse>()
                    Timber.d("Unsplash search successful: found ${body.results.size} photos, total=${body.total}, totalPages=${body.totalPages}")
                    return Result.success(body)
                } else {
                    // Non-2xx status code - check if retryable
                    if (isRetryableError(statusCode)) {
                        // Retryable error - will retry below
                        throw HttpException(statusCode.value, "HTTP ${statusCode.value}: ${statusCode.description}")
                    } else {
                        // Non-retryable error (4xx except 429)
                        Timber.e("Non-retryable HTTP error: ${statusCode.value} for query=$query, page=$page")
                        return Result.failure(HttpException(statusCode.value, "HTTP ${statusCode.value}: ${statusCode.description}"))
                    }
                }
            } catch (e: Exception) {
                val isRetryable = isRetryableException(e)
                
                if (!isRetryable) {
                    // Non-retryable exception - return immediately
                    Timber.e(e, "Non-retryable error for Unsplash photos: query=$query, page=$page")
                    return Result.failure(e)
                }
                
                // If this was the last attempt, return failure
                if (attempt >= MAX_RETRIES) {
                    Timber.e(e, "Failed to search Unsplash photos after ${MAX_RETRIES + 1} attempts: query=$query, page=$page")
                    return Result.failure(e)
                }
                
                // Calculate exponential backoff with jitter
                val baseDelay = INITIAL_DELAY_MS * (1L shl attempt) // 500ms, 1000ms, 2000ms, 4000ms
                val delayMs = minOf(baseDelay, MAX_DELAY_MS)
                val jitter = (delayMs * JITTER_FACTOR * Random.nextDouble(-1.0, 1.0)).toLong()
                val finalDelay = maxOf(0, delayMs + jitter)
                
                attempt++
                Timber.w("Retryable error (attempt $attempt/${MAX_RETRIES + 1}): ${e.message}. Retrying in ${finalDelay}ms...")
                delay(finalDelay)
            }
        }
        
        // Should never reach here, but just in case
        return Result.failure(IllegalStateException("Failed to search Unsplash photos after ${MAX_RETRIES + 1} attempts"))
    }
    
    private fun isRetryableError(statusCode: HttpStatusCode): Boolean {
        return when (statusCode.value) {
            in 500..599 -> true // Server errors
            429 -> true // Rate limit
            408 -> true // Request timeout
            else -> false
        }
    }
    
    private fun isRetryableException(e: Exception): Boolean {
        return when (e) {
            is HttpException -> isRetryableError(HttpStatusCode.fromValue(e.statusCode))
            is IOException -> true // Network errors
            is SocketTimeoutException -> true // Timeout errors
            is TimeoutException -> true // Timeout errors
            else -> false
        }
    }
    
    private class HttpException(val statusCode: Int, message: String) : Exception(message)
}

