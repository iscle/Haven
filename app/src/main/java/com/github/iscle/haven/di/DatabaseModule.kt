package com.github.iscle.haven.di

import android.content.Context
import androidx.room.Room
import com.github.iscle.haven.data.local.database.HavenDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideHavenDatabase(
        @ApplicationContext context: Context
    ): HavenDatabase {
        return Room.databaseBuilder(
            context,
            HavenDatabase::class.java,
            "haven_database"
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    @Singleton
    fun provideWallpaperHistoryDao(
        database: HavenDatabase
    ) = database.wallpaperHistoryDao()
    
    @Provides
    @Singleton
    fun providePhotoCacheDao(
        database: HavenDatabase
    ) = database.photoCacheDao()
}



