package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.*
import com.github.iscle.haven.domain.repository.PreferencesRepository
import com.github.iscle.haven.domain.repository.UnsplashRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.io.IOException

class GetRandomBackgroundImageUseCaseTest {
    private val preferences = mock(PreferencesRepository::class.java)
    private val photos = mock(UnsplashRepository::class.java)
    private val useCase = GetRandomBackgroundImageUseCase(photos, preferences)
    private val image = BackgroundImage("one", "image-one", "Author", "author")

    @Test fun `bookkeeping failures do not discard a usable photo`() = runTest {
        val source = PhotoSource("device", PhotoSourceType.DEVICE, "Device", "content://photo/1")
        `when`(preferences.photoSources).thenReturn(flowOf(listOf(source)))
        `when`(photos.getRandomPhotoForSource(source)).thenReturn(Result.success(image))
        doThrow(IllegalStateException("storage unavailable")).`when`(preferences).setLastPhotoSourceId(source.id)
        doThrow(IllegalStateException("database unavailable")).`when`(photos).addToHistory(image)
        assertEquals(image, useCase().getOrThrow())
    }

    @Test fun `disabled sources are skipped and enabled failures are exhausted`() = runTest {
        val first = PhotoSource("first", PhotoSourceType.UNSPLASH_QUERY, "First", "first")
        val second = first.copy(id = "second", value = "second")
        val disabled = first.copy(id = "disabled", enabled = false)
        `when`(preferences.photoSources).thenReturn(flowOf(listOf(first, second, disabled)))
        `when`(photos.getRandomPhotoForSource(first)).thenReturn(Result.failure(IOException("offline")))
        `when`(photos.getRandomPhotoForSource(second)).thenReturn(Result.failure(IOException("offline")))
        assertTrue(useCase().isFailure)
        verify(photos).getRandomPhotoForSource(first)
        verify(photos).getRandomPhotoForSource(second)
        verifyNoMoreInteractions(photos)
    }

    @Test fun `history cancellation propagates`() = runTest {
        `when`(preferences.photoSources).thenReturn(flowOf(emptyList()))
        `when`(photos.getRandomPhoto()).thenReturn(Result.success(image))
        doThrow(CancellationException("cancelled")).`when`(photos).addToHistory(image)
        try {
            useCase()
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
    }
}
