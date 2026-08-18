package com.github.iscle.haven.data.repository

import com.github.iscle.haven.data.local.InstalledAppsDataSource
import com.github.iscle.haven.domain.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton
import com.github.iscle.haven.domain.repository.AppsRepository as AppsRepositoryInterface

@Singleton
class AppsRepositoryImpl @Inject constructor(
    private val installedAppsDataSource: InstalledAppsDataSource
) : AppsRepositoryInterface {
    override suspend fun getLaunchableApps(): List<InstalledApp> =
        installedAppsDataSource.getLaunchableApps()
}
