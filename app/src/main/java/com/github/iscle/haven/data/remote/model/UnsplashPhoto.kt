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
data class UnsplashUser(
    val id: String,
    val username: String,
    val name: String,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null
)

