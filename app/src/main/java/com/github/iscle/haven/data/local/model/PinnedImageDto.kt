package com.github.iscle.haven.data.local.model

import com.github.iscle.haven.domain.model.BackgroundImage
import kotlinx.serialization.Serializable

/**
 * A wallpaper the user picked from history with "Show on clock now".
 * Stored as JSON in preferences so the home screen can pick it up immediately.
 */
@Serializable
data class PinnedImageDto(
    val id: String,
    val url: String,
    val photographer: String,
    val photographerUsername: String,
    val unsplashUrl: String? = null,
    val artistProfileUrl: String? = null,
    val profileImageUrl: String? = null
) {
    fun toDomain(): BackgroundImage = BackgroundImage(
        id = id,
        url = url,
        photographer = photographer,
        photographerUsername = photographerUsername,
        unsplashUrl = unsplashUrl,
        artistProfileUrl = artistProfileUrl,
        profileImageUrl = profileImageUrl
    )

    companion object {
        fun fromDomain(image: BackgroundImage): PinnedImageDto = PinnedImageDto(
            id = image.id,
            url = image.url,
            photographer = image.photographer,
            photographerUsername = image.photographerUsername,
            unsplashUrl = image.unsplashUrl,
            artistProfileUrl = image.artistProfileUrl,
            profileImageUrl = image.profileImageUrl
        )
    }
}
