package com.github.iscle.haven.domain.repository

import com.github.iscle.haven.domain.model.InstalledApp

interface AppsRepository {
    suspend fun getLaunchableApps(): List<InstalledApp>
}
