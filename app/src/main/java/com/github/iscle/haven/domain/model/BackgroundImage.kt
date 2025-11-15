package com.github.iscle.haven.domain.model

data class BackgroundImage(
    val id: String,
    val url: String,
    val photographer: String,
    val photographerUsername: String,
    val unsplashUrl: String? = null,
    val artistProfileUrl: String? = null,
    val profileImageUrl: String? = null
)

