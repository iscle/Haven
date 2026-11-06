package com.github.iscle.haven.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Unsplash serves a JavaScript browser check before /napi responses. Navigate to
 * the API in a real WebView so its check, redirects and cookies work normally.
 * No native JavaScript bridge or Activity is retained. Each request owns and
 * destroys its WebView; CookieManager retains the website session between calls.
 */
@Singleton
class WebViewUnsplashTransport internal constructor(
    private val createWebView: () -> WebView,
    private val timeoutMillis: Long = 60_000L
) : UnsplashTransport {
    @Inject constructor(@ApplicationContext context: Context) : this({ WebView(context) })

    private val requests = Mutex()

    @SuppressLint("SetJavaScriptEnabled") // Required by Unsplash's browser verification.
    override suspend fun get(url: String): String {
        require(isUnsplashUrl(url) && URI(url).path.startsWith("/napi/"))
        return requests.withLock {
            try {
                withTimeout(timeoutMillis) {
                    withContext(Dispatchers.Main.immediate) {
                        val webView = createWebView()
                        try {
                            webView.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            suspendCancellableCoroutine { continuation ->
                                fun fail(error: Exception) {
                                    if (continuation.isActive) continuation.resumeWithException(error)
                                }
                                webView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): Boolean {
                                        val blocked = !isUnsplashUrl(request.url.toString())
                                        if (blocked && request.isForMainFrame) {
                                            fail(IOException("Unsplash redirected outside its website"))
                                        }
                                        return blocked
                                    }

                                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                                        if (!continuation.isActive || loadedUrl != url) return
                                        // HTML is the browser check, never a photo API response.
                                        view.evaluateJavascript(
                                            """document.contentType === 'application/json'
                                                ? document.body.innerText : null""".trimIndent()
                                        ) { value ->
                                            if (!continuation.isActive || value == "null") return@evaluateJavascript
                                            try {
                                                continuation.resume(Json.decodeFromString<String>(value))
                                            } catch (error: Exception) {
                                                fail(IOException("Invalid Unsplash browser response", error))
                                            }
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        error: WebResourceError
                                    ) {
                                        if (request.isForMainFrame) {
                                            fail(IOException("Unsplash browser error: ${error.description}"))
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView,
                                        request: WebResourceRequest,
                                        response: WebResourceResponse
                                    ) {
                                        // A browser-check page may use a non-2xx status and still
                                        // complete successfully. Let its scripts/redirects run.
                                        if (request.isForMainFrame && response.mimeType != "text/html") {
                                            fail(UnsplashHttpException(response.statusCode))
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView,
                                        handler: SslErrorHandler,
                                        error: SslError
                                    ) {
                                        handler.cancel()
                                        fail(IOException("Unsplash TLS verification failed"))
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView,
                                        detail: RenderProcessGoneDetail
                                    ): Boolean {
                                        fail(IOException("Unsplash browser process stopped"))
                                        return true
                                    }
                                }
                                webView.loadUrl(url)
                            }
                        } finally {
                            webView.stopLoading()
                            webView.webViewClient = WebViewClient()
                            webView.destroy()
                        }
                    }
                }
            } catch (error: TimeoutCancellationException) {
                // Parent cancellation must remain cancellation, including parent timeouts.
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                throw IOException("Timed out waiting for Unsplash browser verification", error)
            }
        }
    }
}

internal fun isUnsplashUrl(url: String): Boolean = try {
    val uri = URI(url)
    uri.scheme == "https" && uri.host == "unsplash.com" &&
        uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443)
} catch (_: java.net.URISyntaxException) {
    false
}
