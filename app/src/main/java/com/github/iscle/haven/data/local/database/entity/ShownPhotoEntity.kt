package com.github.iscle.haven.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shown_photos",
    foreignKeys = [
        ForeignKey(
            entity = CacheMetadataEntity::class,
            parentColumns = ["cacheKey"],
            childColumns = ["cacheKey"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotoCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cacheKey"]),
        Index(value = ["photoId"])
    ]
)
data class ShownPhotoEntity(
    @PrimaryKey
    val id: String, // composite: cacheKey + photoId
    val cacheKey: String, // cache key (lowercase, trimmed) - FK to cache_metadata
    val photoId: String, // FK to photo_cache.id
    val shownAt: Long
)

