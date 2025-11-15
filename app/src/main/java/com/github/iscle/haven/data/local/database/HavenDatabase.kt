package com.github.iscle.haven.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.github.iscle.haven.data.local.database.dao.PhotoCacheDao
import com.github.iscle.haven.data.local.database.dao.WallpaperHistoryDao
import com.github.iscle.haven.data.local.database.entity.CacheMetadataEntity
import com.github.iscle.haven.data.local.database.entity.PhotoCacheEntity
import com.github.iscle.haven.data.local.database.entity.ShownPhotoEntity
import com.github.iscle.haven.data.local.database.entity.WallpaperHistoryEntity

@Database(
    entities = [
        WallpaperHistoryEntity::class,
        PhotoCacheEntity::class,
        ShownPhotoEntity::class,
        CacheMetadataEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class HavenDatabase : RoomDatabase() {
    abstract fun wallpaperHistoryDao(): WallpaperHistoryDao
    abstract fun photoCacheDao(): PhotoCacheDao
}

