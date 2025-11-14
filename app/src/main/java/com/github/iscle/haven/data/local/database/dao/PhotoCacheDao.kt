package com.github.iscle.haven.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.iscle.haven.data.local.database.entity.CacheMetadataEntity
import com.github.iscle.haven.data.local.database.entity.PhotoCacheEntity
import com.github.iscle.haven.data.local.database.entity.ShownPhotoEntity

@Dao
interface PhotoCacheDao {
    
    // Photo cache operations
    @Query("""
        SELECT * FROM photo_cache 
        WHERE cacheKey = :cacheKey 
        AND id NOT IN (SELECT photoId FROM shown_photos WHERE cacheKey = :cacheKey)
    """)
    suspend fun getUnshownPhotosByQuery(cacheKey: String): List<PhotoCacheEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoCacheEntity>)
    
    @Query("DELETE FROM photo_cache WHERE id = :photoId AND cacheKey = :cacheKey")
    suspend fun deletePhoto(photoId: String, cacheKey: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShownPhoto(shownPhoto: ShownPhotoEntity)
    
    // Cache metadata operations
    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :cacheKey")
    suspend fun getCacheMetadata(cacheKey: String): CacheMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: CacheMetadataEntity)
    
    @Query("DELETE FROM cache_metadata WHERE cacheKey = :cacheKey")
    suspend fun deleteCacheMetadata(cacheKey: String)
    
    @Query("DELETE FROM cache_metadata")
    suspend fun clearAllCacheMetadata()
    
    // Combined operations
    @Query("SELECT COUNT(*) FROM photo_cache WHERE cacheKey = :cacheKey")
    suspend fun getPhotoCount(cacheKey: String): Int
}

