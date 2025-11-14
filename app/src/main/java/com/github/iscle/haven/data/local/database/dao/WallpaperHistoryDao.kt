package com.github.iscle.haven.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.iscle.haven.data.local.database.entity.WallpaperHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperHistoryDao {
    
    @Query("SELECT * FROM wallpaper_history ORDER BY shownAt DESC")
    fun getAllHistory(): Flow<List<WallpaperHistoryEntity>>
    
    @Query("SELECT * FROM wallpaper_history ORDER BY shownAt DESC")
    suspend fun getAllHistoryList(): List<WallpaperHistoryEntity>
    
    @Query("SELECT * FROM wallpaper_history WHERE imageId = :imageId LIMIT 1")
    suspend fun getHistoryByImageId(imageId: String): WallpaperHistoryEntity?
    
    @Query("SELECT * FROM wallpaper_history WHERE isFavorite = 1 ORDER BY shownAt DESC")
    suspend fun getFavorites(): List<WallpaperHistoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WallpaperHistoryEntity)
    
    @Update
    suspend fun update(history: WallpaperHistoryEntity)
    
    @Delete
    suspend fun delete(history: WallpaperHistoryEntity)
    
    @Query("DELETE FROM wallpaper_history WHERE imageId = :imageId")
    suspend fun deleteByImageId(imageId: String)
    
    @Query("UPDATE wallpaper_history SET isFavorite = :isFavorite WHERE imageId = :imageId")
    suspend fun updateFavoriteStatus(imageId: String, isFavorite: Boolean)
    
    @Query("UPDATE wallpaper_history SET timesShown = timesShown + 1, shownAt = :shownAt WHERE imageId = :imageId")
    suspend fun incrementTimesShown(imageId: String, shownAt: Long)
    
    @Query("SELECT COUNT(*) FROM wallpaper_history")
    suspend fun getHistoryCount(): Int
    
    @Query("DELETE FROM wallpaper_history")
    suspend fun clearAll()
}

