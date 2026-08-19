package com.github.iscle.haven.domain.model

enum class PhotoSourceType {
    /** A free-text Unsplash search, e.g. "fog". */
    UNSPLASH_QUERY,

    /** An Unsplash collection, identified by its numeric id. */
    UNSPLASH_COLLECTION,

    /** The wallpapers the user hearted in history. */
    FAVORITES,

    /** A photo picked from this device, stored as a content URI. */
    DEVICE
}

/**
 * One place Haven draws wallpapers from. The clock shuffles across every
 * source that is switched on.
 */
data class PhotoSource(
    val id: String,
    val type: PhotoSourceType,
    val label: String,
    /** Search text, collection id, or content URI, depending on [type]. */
    val value: String = "",
    val enabled: Boolean = true,
    val subtitle: String? = null,
    /** Thumbnail shown on the source card once one photo has been seen. */
    val previewUrl: String? = null
)

/** The Unsplash search Haven shipped with before sources were configurable. */
const val DEFAULT_PHOTO_QUERY = "wallpaper nature"

val DEFAULT_PHOTO_SOURCES = listOf(
    PhotoSource(
        id = "default-query",
        type = PhotoSourceType.UNSPLASH_QUERY,
        label = DEFAULT_PHOTO_QUERY,
        value = DEFAULT_PHOTO_QUERY,
        enabled = true,
        subtitle = "Search query"
    ),
    PhotoSource(
        id = "favorites",
        type = PhotoSourceType.FAVORITES,
        label = "My favorites",
        enabled = false,
        subtitle = "Wallpapers you hearted"
    )
)
