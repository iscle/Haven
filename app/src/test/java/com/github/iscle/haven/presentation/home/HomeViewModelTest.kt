package com.github.iscle.haven.presentation.home

import androidx.lifecycle.ViewModelStore
import com.github.iscle.haven.domain.model.*
import com.github.iscle.haven.domain.repository.*
import com.github.iscle.haven.domain.usecase.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val preferences = mock(PreferencesRepository::class.java)
    private val photos = mock(UnsplashRepository::class.java)
    private val weather = mock(WeatherRepository::class.java)
    private val notifications = mock(NotificationRepository::class.java)
    private val interval = MutableStateFlow(10)
    private val pin = MutableStateFlow<BackgroundImage?>(null)
    private val image = BackgroundImage("one", "image-one", "Author", "author")

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        `when`(preferences.backgroundInterval).thenReturn(interval)
        `when`(preferences.pinnedImage).thenReturn(pin)
        `when`(preferences.cityName).thenReturn(flowOf(""))
        `when`(preferences.photoSources).thenReturn(flowOf(emptyList()))
        `when`(preferences.appPreferences).thenReturn(flowOf(AppPreferences()))
        `when`(notifications.notifications).thenReturn(flowOf(emptyList()))
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun runModelTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            store.clear()
            runCurrent()
        }
    }

    private fun create() = HomeViewModel(
        GetRandomBackgroundImageUseCase(photos, preferences),
        GetWeatherUseCase(weather), GetBackgroundIntervalUseCase(preferences),
        GetCityNameUseCase(preferences), GetAppPreferencesUseCase(preferences),
        GetAllowedNotificationsUseCase(notifications, preferences),
        GetPinnedImageUseCase(preferences), SetPinnedImageUseCase(preferences)
    ).also { store.put("home", it) }

    @Test fun `initial failure at default interval retries automatically`() = runModelTest {
        `when`(photos.getRandomPhoto()).thenReturn(Result.failure(IOException("offline")), Result.success(image))
        val model = create()
        runCurrent()
        assertNull(model.uiState.value.backgroundImage)
        assertFalse(model.uiState.value.isLoadingBackground)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(image, model.uiState.value.backgroundImage)
        verify(photos, times(2)).getRandomPhoto()
    }

    @Test fun `failure at long interval retries within thirty seconds`() = runModelTest {
        interval.value = 86_400
        `when`(photos.getRandomPhoto()).thenReturn(Result.failure(IOException("offline")), Result.success(image))
        val model = create()
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(image, model.uiState.value.backgroundImage)
    }

    @Test fun `slow load and repeated refresh do not overlap`() = runModelTest {
        val gate = CompletableDeferred<Result<BackgroundImage>>()
        val source = object : UnsplashRepository by photos {
            override suspend fun getRandomPhoto(query: String, includeFavorites: Boolean) = gate.await()
        }
        val model = HomeViewModel(
            GetRandomBackgroundImageUseCase(source, preferences),
            GetWeatherUseCase(weather), GetBackgroundIntervalUseCase(preferences),
            GetCityNameUseCase(preferences), GetAppPreferencesUseCase(preferences),
            GetAllowedNotificationsUseCase(notifications, preferences),
            GetPinnedImageUseCase(preferences), SetPinnedImageUseCase(preferences)
        ).also { store.put("home", it) }
        runCurrent()
        val jobIdBefore = model.uiState.value.backgroundRequestId
        repeat(5) { model.loadBackgroundImage() }
        advanceTimeBy(25_000)
        runCurrent()
        assertTrue(model.uiState.value.isLoadingBackground)
        gate.complete(Result.success(image))
        runCurrent()
        assertEquals(jobIdBefore + 1, model.uiState.value.backgroundRequestId)
        assertEquals(image, model.uiState.value.backgroundImage)
    }

    @Test fun `pin cancels old request and stays for full interval`() = runModelTest {
        val gate = CompletableDeferred<Result<BackgroundImage>>()
        val source = object : UnsplashRepository by photos {
            override suspend fun getRandomPhoto(query: String, includeFavorites: Boolean) = gate.await()
        }
        val model = HomeViewModel(
            GetRandomBackgroundImageUseCase(source, preferences),
            GetWeatherUseCase(weather), GetBackgroundIntervalUseCase(preferences),
            GetCityNameUseCase(preferences), GetAppPreferencesUseCase(preferences),
            GetAllowedNotificationsUseCase(notifications, preferences),
            GetPinnedImageUseCase(preferences), SetPinnedImageUseCase(preferences)
        ).also { store.put("home", it) }
        runCurrent()
        advanceTimeBy(5_000)
        val pinned = image.copy(id = "pinned")
        pin.value = pinned
        runCurrent()
        gate.complete(Result.success(image))
        runCurrent()
        assertEquals(pinned, model.uiState.value.backgroundImage)
        model.onBackgroundImageLoaded(pinned, model.uiState.value.backgroundRequestId)
        advanceTimeBy(9_999)
        runCurrent()
        assertEquals(pinned, model.uiState.value.backgroundImage)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(image, model.uiState.value.backgroundImage)
    }

    @Test fun `decode failure retries even when next selection is same photo`() = runModelTest {
        interval.value = 3600
        `when`(photos.getRandomPhoto()).thenReturn(Result.success(image))
        val model = create()
        runCurrent()
        val initialId = model.uiState.value.backgroundRequestId
        model.onBackgroundImageError(image, initialId)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(initialId + 1, model.uiState.value.backgroundRequestId)
    }

    @Test fun `failed refresh retains last photo and invalid interval cannot spin`() = runModelTest {
        interval.value = 0
        `when`(photos.getRandomPhoto()).thenReturn(Result.success(image), Result.failure(IOException("offline")))
        val model = create()
        runCurrent()
        model.onBackgroundImageLoaded(image, model.uiState.value.backgroundRequestId)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(image, model.uiState.value.backgroundImage)
        assertFalse(model.uiState.value.isLoadingBackground)
        verify(photos, times(2)).getRandomPhoto()
    }
    @Test fun `slow decoding is not superseded and gets a full display interval`() = runModelTest {
        `when`(photos.getRandomPhoto()).thenReturn(Result.success(image))
        val model = create()
        runCurrent()
        advanceTimeBy(35_000)
        runCurrent()
        verify(photos, times(1)).getRandomPhoto()
        assertTrue(model.uiState.value.isLoadingBackground)
        model.onBackgroundImageLoaded(image, model.uiState.value.backgroundRequestId)
        advanceTimeBy(9_999)
        runCurrent()
        verify(photos, times(1)).getRandomPhoto()
        advanceTimeBy(1)
        runCurrent()
        verify(photos, times(2)).getRandomPhoto()
    }

    @Test fun `stale decode callbacks cannot release a newer request`() = runModelTest {
        `when`(photos.getRandomPhoto()).thenReturn(Result.success(image))
        val model = create()
        runCurrent()
        val oldRequest = model.uiState.value.backgroundRequestId
        pin.value = image
        runCurrent()
        model.onBackgroundImageLoaded(image, oldRequest)
        model.onBackgroundImageError(image, oldRequest)
        advanceTimeBy(35_000)
        runCurrent()
        verify(photos, times(1)).getRandomPhoto()
        assertTrue(model.uiState.value.isLoadingBackground)
    }

}
