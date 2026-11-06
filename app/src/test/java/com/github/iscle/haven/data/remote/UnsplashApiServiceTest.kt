package com.github.iscle.haven.data.remote

import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UnsplashApiServiceTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val emptySearch = """{"total":0,"total_pages":0,"results":[],"new_field":true}"""

    private fun service(get: suspend (String) -> String) = UnsplashApiService(
        object : UnsplashTransport {
            override suspend fun get(url: String) = get(url)
        }, json
    )

    @Test fun `search stays on private API and encodes parameters`() = runTest {
        val api = service { url ->
            val request = Url(url)
            assertEquals("https://unsplash.com/napi/search/photos", url.substringBefore('?'))
            assertEquals("forest & sea/雪", request.parameters["query"])
            assertEquals("none", request.parameters["plus"])
            assertEquals("landscape", request.parameters["orientation"])
            assertEquals("2", request.parameters["page"])
            assertEquals("20", request.parameters["per_page"])
            emptySearch
        }
        assertTrue(api.searchPhotos("forest & sea/雪", 2, 20).isSuccess)
    }

    @Test fun `search retries transient errors with bounded backoff`() = runTest {
        var calls = 0
        val api = service {
            calls++
            if (calls < 3) throw UnsplashHttpException(503)
            emptySearch
        }
        assertTrue(api.searchPhotos("nature", 1, 20).isSuccess)
        assertEquals(3, calls)
        assertEquals(1500, testScheduler.currentTime)
    }

    @Test fun `permanent HTTP errors and malformed JSON are not retried`() = runTest {
        for (response in listOf<suspend (String) -> String>(
            { throw UnsplashHttpException(404) }, { "<html>Checking your browser</html>" }
        )) {
            var calls = 0
            val api = service { calls++; response(it) }
            assertTrue(api.searchPhotos("nature", 1, 20).isFailure)
            assertEquals(1, calls)
        }
    }

    @Test fun `network failures stop after three attempts`() = runTest {
        var calls = 0
        val api = service { calls++; throw IOException("offline") }
        assertTrue(api.searchPhotos("nature", 1, 20).isFailure)
        assertEquals(3, calls)
    }

    @Test fun `cancellation propagates through every endpoint without retry`() = runTest {
        var calls = 0
        val api = service { calls++; throw CancellationException("cancelled") }
        val requests = listOf<suspend () -> Unit>(
            { api.searchPhotos("nature", 1, 20); Unit },
            { api.getCollectionPhotos("123", 1, 20); Unit },
            { api.getCollection("123"); Unit }
        )
        for (request in requests) {
            try {
                request()
                fail("Cancellation was swallowed")
            } catch (_: CancellationException) { }
        }
        assertEquals(3, calls)
    }

    @Test fun `collections use private paths and same retry policy`() = runTest {
        var calls = 0
        val api = service { url ->
            calls++
            if (calls == 1) throw UnsplashHttpException(429)
            assertTrue(url.startsWith("https://unsplash.com/napi/collections/123/photos?"))
            "[]"
        }
        assertEquals(emptyList<Any>(), api.getCollectionPhotos("123", 1, 20).getOrThrow())
        assertEquals(2, calls)
        val info = service { url ->
            assertEquals("https://unsplash.com/napi/collections/123", url)
            """{"id":"123","title":"Nature","total_photos":42}"""
        }.getCollection("123").getOrThrow()
        assertEquals(42, info.totalPhotos)
    }

    @Test fun `browser navigation is confined to HTTPS Unsplash origin`() {
        assertTrue(isUnsplashUrl("https://unsplash.com/.within.website?redir=%2Fnapi"))
        assertTrue(isUnsplashUrl("https://unsplash.com:443/napi/search/photos"))
        for (url in listOf(
            "http://unsplash.com/napi", "https://unsplash.com.evil.example/napi",
            "https://unsplash.com@evil.example/napi", "https://user@unsplash.com/napi",
            "file:///etc/passwd", "javascript:alert(1)", "https://unsplash.com:8443/napi",
            "not a url"
        )) assertFalse(url, isUnsplashUrl(url))
    }
}
