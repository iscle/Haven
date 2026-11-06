package com.github.iscle.haven.presentation.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.github.iscle.haven.domain.model.BackgroundImage

internal data class LoadedBackground(val image: BackgroundImage, val drawable: Drawable)

/** Hold on to the decoded photo until its replacement is ready, even if decoding fails. */
@Composable
internal fun rememberLoadedBackground(
    image: BackgroundImage?,
    requestId: Long,
    imageLoader: ImageLoader,
    onLoaded: (BackgroundImage, Long) -> Unit = { _, _ -> },
    onError: (BackgroundImage, Long) -> Unit
): LoadedBackground? {
    val context = LocalContext.current
    val size = LocalWindowInfo.current.containerSize
    val width = size.width.coerceAtLeast(1)
    val height = size.height.coerceAtLeast(1)
    val latestOnError by rememberUpdatedState(onError)
    val latestOnLoaded by rememberUpdatedState(onLoaded)
    var loaded by remember { mutableStateOf<LoadedBackground?>(null) }
    LaunchedEffect(image, requestId, width, height, imageLoader) {
        if (image == null) return@LaunchedEffect
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .data(image.url)
                .size(width, height)
                .scale(Scale.FILL)
                .build()
        )
        if (result is SuccessResult) {
            loaded = LoadedBackground(image, result.drawable)
            latestOnLoaded(image, requestId)
        } else {
            latestOnError(image, requestId)
        }
    }
    return loaded
}
