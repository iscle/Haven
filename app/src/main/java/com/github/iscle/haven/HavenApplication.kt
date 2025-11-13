package com.github.iscle.haven

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class HavenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In release builds, you might want to use a crash reporting tree
            Timber.plant(Timber.DebugTree()) // For now, using DebugTree in release too
        }
        
        Timber.d("HavenApplication initialized")
    }
}

