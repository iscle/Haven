package com.github.iscle.haven.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Real WebView with intercepted fixture responses: no network or external browser check. */
class WebViewUnsplashTransportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val url = "https://unsplash.com/napi/haven-test/${UUID.randomUUID()}"
    private val destroyed = AtomicInteger()

    private fun response(body: String, mime: String = "application/json", status: Int = 200) =
        WebResourceResponse(mime, "utf-8", status, if (status == 200) "OK" else "Not Found",
            mapOf("Cache-Control" to "no-store"), body.byteInputStream())

    private fun transport(
        timeoutMillis: Long = 5_000,
        respond: (WebResourceRequest) -> WebResourceResponse
    ) = WebViewUnsplashTransport(
        createWebView = { FixtureWebView(context, respond) { destroyed.incrementAndGet() } },
        timeoutMillis = timeoutMillis
    )

    @Test fun htmlCheckRunsJavaScriptThenReturnsJsonWithoutCorruptingEscapes() = runBlocking {
        val calls = AtomicInteger()
        val expected = """{"name":"A \"quoted\" photographer","url":"https://example.test/a?b=1&c=2"}"""
        val client = transport { request ->
            if (request.url.toString() == url && calls.incrementAndGet() == 1) {
                response("""<html><body>Checking browser<script>
                    setTimeout(function() { location.reload(); }, 100);
                    </script></body></html>""", "text/html")
            } else response(expected)
        }
        assertEquals(expected, client.get(url))
        assertEquals(1, destroyed.get())
        assertTrue(calls.get() >= 2)
    }

    @Test fun nonHtmlHttpFailureIsReportedAndBrowserIsDestroyed() = runBlocking {
        val client = transport { response("""{"errors":["Not found"]}""", status = 404) }
        try {
            client.get(url)
            fail("Expected HTTP failure")
        } catch (error: UnsplashHttpException) {
            assertEquals(404, error.statusCode)
        }
        assertEquals(1, destroyed.get())
    }

    @Test fun stalledChallengeTimesOutAndReleasesBrowser() = runBlocking {
        val client = transport(timeoutMillis = 1_000) { response("<html>Waiting</html>", "text/html") }
        try {
            client.get(url)
            fail("Expected verification timeout")
        } catch (error: IOException) {
            assertTrue(error.message!!.contains("Timed out"))
        }
        assertEquals(1, destroyed.get())
    }

    @Test fun cancellationReleasesBrowserAndNextRequestCanProceed() = runBlocking {
        val requested = CompletableDeferred<Unit>()
        val stalledUrl = "$url?stalled=true"
        val client = transport { request ->
            if (request.url.toString() == stalledUrl) {
                requested.complete(Unit)
                response("<html>Waiting</html>", "text/html")
            } else response("[]")
        }
        val pending = async { client.get(stalledUrl) }
        withTimeout(5_000) { requested.await() }
        pending.cancelAndJoin()
        assertTrue(pending.isCancelled)
        assertEquals(1, destroyed.get())
        assertEquals("[]", client.get(url))
        assertEquals(2, destroyed.get())
    }

    private class FixtureWebView(
        context: Context,
        private val respond: (WebResourceRequest) -> WebResourceResponse,
        private val onDestroy: () -> Unit
    ) : WebView(context) {
        override fun setWebViewClient(delegate: WebViewClient) {
            super.setWebViewClient(object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                    respond(request).also { response ->
                        // Older WebViews do not emit HTTP-error callbacks for intercepted
                        // responses. Deliver the server event explicitly in this fixture.
                        if (response.statusCode >= 400) {
                            // View.post can stay queued forever while this view is unattached.
                            Handler(Looper.getMainLooper()).post {
                                delegate.onReceivedHttpError(view, request, response)
                            }
                        }
                    }
                override fun onPageFinished(view: WebView, url: String) = delegate.onPageFinished(view, url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                    delegate.shouldOverrideUrlLoading(view, request)
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) =
                    delegate.onReceivedHttpError(view, request, response)
            })
        }

        override fun destroy() {
            super.destroy()
            onDestroy()
        }
    }
}
