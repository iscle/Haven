package com.github.iscle.haven.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.github.iscle.haven.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the launchable apps on the device for the app drawer. */
@Singleton
class InstalledAppsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            packageManager.queryIntentActivities(intent, 0)
        }.onFailure { Timber.e(it, "Failed to query launchable apps") }.getOrDefault(emptyList())

        val collator = Collator.getInstance()
        resolved
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim()
                    .takeUnless { it.isNullOrEmpty() } ?: packageName
                InstalledApp(
                    packageName = packageName,
                    label = label,
                    isSystem = activityInfo.applicationInfo?.let { isSystemApp(it) } ?: false,
                    firstInstallTime = firstInstallTime(packageManager, packageName)
                )
            }
            .distinctBy { it.packageName }
            .sortedWith { first, second -> collator.compare(first.label, second.label) }
            .toList()
            .also { Timber.d("Loaded ${it.size} launchable apps") }
    }

    private fun isSystemApp(info: ApplicationInfo): Boolean =
        info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    private fun firstInstallTime(packageManager: PackageManager, packageName: String): Long =
        runCatching { packageManager.getPackageInfo(packageName, 0).firstInstallTime }
            .getOrDefault(0L)
}
