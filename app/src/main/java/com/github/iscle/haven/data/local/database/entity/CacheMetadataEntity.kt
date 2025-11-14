package com.github.iscle.haven.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey
    val cacheKey: String, // cache key (lowercase, trimmed)
    val cachedAt: Long // timestamp when cache was created
)

