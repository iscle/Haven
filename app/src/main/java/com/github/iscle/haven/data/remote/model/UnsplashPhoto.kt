package com.github.iscle.haven.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnsplashSearchResponse(
    val total: Int,
    @SerialName("total_pages")
    val totalPages: Int,
    val results: List<UnsplashPhoto>,
    val id: String? = null
)

@Serializable
data class UnsplashPhoto(
    val id: String,
    val slug: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val width: Int,
    val height: Int,
    val color: String,
    @SerialName("blur_hash")
    val blurHash: String? = null,
    val description: String? = null,
    @SerialName("alt_description")
    val altDescription: String? = null,
    val urls: UnsplashUrls,
    val links: UnsplashLinks,
    val user: UnsplashUser
)

@Serializable
data class UnsplashUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String,
    @SerialName("small_s3")
    val smallS3: String? = null
)

@Serializable
data class UnsplashLinks(
    val self: String? = null,
    val html: String,
    val download: String? = null,
    @SerialName("download_location")
    val downloadLocation: String? = null
)

@Serializable
data class UnsplashUser(
    val id: String,
    val username: String,
    val name: String,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("profile_image")
    val profileImage: UnsplashProfileImage? = null,
    val links: UnsplashUserLinks
)

@Serializable
data class UnsplashProfileImage(
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null
)

@Serializable
data class UnsplashUserLinks(
    val self: String? = null,
    val html: String,
    val photos: String? = null,
    val likes: String? = null,
    val portfolio: String? = null,
    val following: String? = null,
    val followers: String? = null
)

