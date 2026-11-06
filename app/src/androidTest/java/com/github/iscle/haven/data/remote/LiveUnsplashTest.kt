package com.github.iscle.haven.data.remote

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in smoke test; deterministic CI tests never depend on Unsplash availability. */
class LiveUnsplashTest {
    @Test fun privateSearchCompletesBrowserVerification() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("havenLiveNetwork") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = UnsplashApiService(WebViewUnsplashTransport(context), Json { ignoreUnknownKeys = true })
        val response = service.searchPhotos("wallpaper nature", 1, 20).getOrThrow()
        assertTrue(response.results.isNotEmpty())
        assertTrue(response.results.any { it.width > it.height })
        assertTrue(response.results.all { it.urls.full.startsWith("https://") })
        // A second request exercises the retained browser session.
        assertTrue(service.searchPhotos("mountains", 2, 20).getOrThrow().results.isNotEmpty())
    }
}
