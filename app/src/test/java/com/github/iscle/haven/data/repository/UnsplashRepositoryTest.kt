package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.HistoryDataSource
import com.github.iscle.haven.data.local.PhotoCacheDataSource
import com.github.iscle.haven.data.remote.UnsplashApiService
import com.github.iscle.haven.data.remote.UnsplashHttpException
import com.github.iscle.haven.data.remote.model.*
import com.github.iscle.haven.domain.model.BackgroundImage
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.PhotoSourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class UnsplashRepositoryTest {
    private val api = mock(UnsplashApiService::class.java)
    private val cache = mock(PhotoCacheDataSource::class.java)
    private val history = mock(HistoryDataSource::class.java)
    private val repository = UnsplashRepositoryImpl(api, cache, history)
    private val image = BackgroundImage("one", "https://images.unsplash.com/one", "Author", "author")

    private fun photo(id: String, width: Int = 2000, height: Int = 1000) = UnsplashPhoto(
        id = id, createdAt = "2026-11-06", width = width, height = height, color = "#000000",
        urls = UnsplashUrls("raw", "full-$id", "regular", "small", "thumb"),
        links = UnsplashLinks(html = "https://unsplash.com/photos/$id"),
        user = UnsplashUser("author", "author", "Author", links = UnsplashUserLinks(html = "profile"))
    )

    @Test fun `fetched page remains cached and selected photo is consumed once`() = runTest {
        val photos = listOf(photo("one"), photo("two"), photo("portrait", 1000, 2000))
        `when`(cache.getRandomCachedPhoto("nature")).thenReturn(null, image)
        `when`(api.searchPhotos("nature", 1, 20))
            .thenReturn(Result.success(UnsplashSearchResponse(3, 1, photos)))

        assertEquals(image, repository.getRandomPhoto("nature", false).getOrThrow())
        verify(cache).cachePhotos("nature", listOf("one", "two").map { id ->
            BackgroundImage(id, "full-$id", "Author", "author", "https://unsplash.com/photos/$id", "profile")
        })
        verify(cache, times(2)).getRandomCachedPhoto("nature")
        verifyNoMoreInteractions(cache)
        verify(api).searchPhotos("nature", 1, 20)
        verifyNoMoreInteractions(api)
    }

    @Test fun `cached photos do not request the network`() = runTest {
        `when`(cache.getRandomCachedPhoto("nature")).thenReturn(image)
        assertEquals(image, repository.getRandomPhoto("nature", false).getOrThrow())
        verifyNoInteractions(api)
    }

    @Test fun `empty first page tries each remaining page only once`() = runTest {
        `when`(api.searchPhotos("nature", 1, 20))
            .thenReturn(Result.success(UnsplashSearchResponse(40, 2, listOf(photo("portrait", 100, 200)))))
        `when`(api.searchPhotos("nature", 2, 20))
            .thenReturn(Result.success(UnsplashSearchResponse(40, 2, emptyList())))
        assertTrue(repository.getRandomPhoto("nature", false).isFailure)
        verify(api).searchPhotos("nature", 1, 20)
        verify(api).searchPhotos("nature", 2, 20)
        verifyNoMoreInteractions(api)
    }

    @Test fun `collection failures retain original error without repeated page requests`() = runTest {
        val error = UnsplashHttpException(404)
        `when`(api.getCollection("123")).thenReturn(Result.failure(error))
        val result = repository.getRandomPhotoForSource(
            PhotoSource("collection", PhotoSourceType.UNSPLASH_COLLECTION, "Collection", "123")
        )
        assertSame(error, result.exceptionOrNull())
        verify(api).getCollection("123")
        verifyNoMoreInteractions(api)
    }
}
