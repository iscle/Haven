package com.github.iscle.haven.presentation.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.github.iscle.haven.domain.model.BackgroundImage
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class LoadedBackgroundTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val first = BackgroundImage("first", "https://example.test/first", "Author", "author")

    @Test fun photoIsPublishedOnlyAfterDecodeAndFailedReplacementKeepsItVisible() {
        val gate = CompletableDeferred<Unit>()
        val image = mutableStateOf(first)
        val failures = AtomicInteger()
        val loader = ImageLoader.Builder(context).components {
            add { chain ->
                if (chain.request.data == first.url) {
                    gate.await()
                    SuccessResult(ColorDrawable(Color.BLUE), chain.request, DataSource.MEMORY)
                } else {
                    ErrorResult(null, chain.request, IOException("decode failed"))
                }
            }
        }.build()
        try {
            compose.setContent {
                val loaded = rememberLoadedBackground(image.value, 1, loader) { _, _ -> failures.incrementAndGet() }
                Text(loaded?.image?.id ?: "loading")
            }
            compose.onNodeWithText("loading").assertIsDisplayed()
            gate.complete(Unit)
            compose.waitUntil { compose.onAllNodes(androidx.compose.ui.test.hasText("first")).fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle { image.value = first.copy(id = "second", url = "https://example.test/second") }
            compose.waitUntil { failures.get() == 1 }
            compose.onNodeWithText("first").assertIsDisplayed()
        } finally {
            loader.shutdown()
        }
    }

    @Test fun samePhotoCanBeRetriedAfterFailedDecode() {
        val requestId = mutableStateOf(1L)
        val calls = AtomicInteger()
        val failures = AtomicInteger()
        val loader = ImageLoader.Builder(context).components {
            add { chain ->
                if (calls.incrementAndGet() == 1) {
                    ErrorResult(null, chain.request, IOException("offline"))
                } else {
                    SuccessResult(ColorDrawable(Color.BLUE), chain.request, DataSource.MEMORY)
                }
            }
        }.build()
        try {
            compose.setContent {
                val loaded = rememberLoadedBackground(first, requestId.value, loader) { _, _ -> failures.incrementAndGet() }
                Text(loaded?.image?.id ?: "loading")
            }
            compose.waitUntil { failures.get() == 1 }
            compose.runOnIdle { requestId.value++ }
            compose.waitUntil { calls.get() == 2 }
            compose.waitForIdle()
            compose.onNodeWithText("first").assertIsDisplayed()
        } finally {
            loader.shutdown()
        }
    }
}
