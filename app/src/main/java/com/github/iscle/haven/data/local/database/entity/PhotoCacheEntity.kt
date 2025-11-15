package com.github.iscle.haven.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_cache",
    foreignKeys = [
        ForeignKey(
            entity = CacheMetadataEntity::class,
            parentColumns = ["cacheKey"],
            childColumns = ["cacheKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cacheKey"])]
)
data class PhotoCacheEntity(
    @PrimaryKey
    val id: String, // photo id
    val cacheKey: String, // cache key (lowercase, trimmed) - FK to cache_metadata
    val url: String,
    val photographer: String,
    val photographerUsername: String,
    val unsplashUrl: String? = null,
    val artistProfileUrl: String? = null,
    val profileImageUrl: String? = null,
    val cachedAt: Long // timestamp when this photo was cached
)

