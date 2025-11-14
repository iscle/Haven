package com.github.iscle.haven.di

import com.github.iscle.haven.data.repository.PreferencesRepositoryImpl
import com.github.iscle.haven.data.repository.UnsplashRepositoryImpl
import com.github.iscle.haven.data.repository.WeatherRepositoryImpl
import com.github.iscle.haven.domain.repository.PreferencesRepository
import com.github.iscle.haven.domain.repository.UnsplashRepository
import com.github.iscle.haven.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindUnsplashRepository(
        unsplashRepositoryImpl: UnsplashRepositoryImpl
    ): UnsplashRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
}

