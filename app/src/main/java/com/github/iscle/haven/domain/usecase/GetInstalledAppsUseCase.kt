package com.github.iscle.haven.domain.usecase

import com.github.iscle.haven.domain.model.InstalledApp
import com.github.iscle.haven.domain.repository.AppsRepository
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val appsRepository: AppsRepository
) {
    suspend operator fun invoke(): List<InstalledApp> = appsRepository.getLaunchableApps()
}
