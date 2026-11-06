package com.github.iscle.haven.data.remote

import com.github.iscle.haven.data.remote.model.UnsplashCollection
import com.github.iscle.haven.data.remote.model.UnsplashPhoto
import com.github.iscle.haven.data.remote.model.UnsplashSearchResponse
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsplashApiService @Inject constructor(
    private val transport: UnsplashTransport,
    private val json: Json
) {
    suspend fun searchPhotos(query: String, page: Int, perPage: Int): Result<UnsplashSearchResponse> =
        request(listOf("search", "photos"), mapOf(
            "plus" to "none",
            "orientation" to "landscape",
            "query" to query,
            "page" to page.toString(),
            "per_page" to perPage.toString()
        ))

    suspend fun getCollectionPhotos(
        collectionId: String,
        page: Int,
        perPage: Int
    ): Result<List<UnsplashPhoto>> = request(
        listOf("collections", collectionId, "photos"),
        mapOf("page" to page.toString(), "per_page" to perPage.toString())
    )

    suspend fun getCollection(collectionId: String): Result<UnsplashCollection> =
        request(listOf("collections", collectionId))

    private suspend inline fun <reified T> request(
        path: List<String>,
        queryParameters: Map<String, String> = emptyMap()
    ): Result<T> {
        val url = URLBuilder("https://unsplash.com/napi/").apply {
            appendPathSegments(path, encodeSlash = true)
            queryParameters.forEach { (key, value) -> this.parameters.append(key, value) }
        }.buildString()
        repeat(3) { attempt ->
            try {
                return Result.success(json.decodeFromString<T>(transport.get(url)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val retryable = when (error) {
                    is UnsplashHttpException -> error.statusCode in 500..599 ||
                        error.statusCode == 408 || error.statusCode == 429
                    is IOException -> true
                    else -> false
                }
                if (!retryable || attempt == 2) return Result.failure(error)
                delay(500L shl attempt)
            }
        }
        error("Retry loop exhausted")
    }
}
