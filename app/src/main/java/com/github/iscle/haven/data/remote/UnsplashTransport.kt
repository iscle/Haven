package com.github.iscle.haven.data.remote

/** JSON transport for Unsplash's private website API, including its browser session. */
interface UnsplashTransport {
    suspend fun get(url: String): String
}

class UnsplashHttpException(val statusCode: Int) : java.io.IOException("Unsplash HTTP $statusCode")
