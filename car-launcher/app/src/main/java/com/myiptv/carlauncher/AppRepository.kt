package com.myiptv.carlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    /** Every launchable app except this launcher, sorted by name. */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val byPackage = LinkedHashMap<String, ResolveInfo>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for (info in pm.queryIntentActivities(intent, 0)) {
            byPackage.putIfAbsent(info.activityInfo.packageName, info)
        }
        byPackage.remove(context.packageName)
        byPackage.values.map(::toEntry).sortedBy { it.label.lowercase() }
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun toEntry(info: ResolveInfo): AppEntry {
        val icon = runCatching {
            info.loadIcon(pm)?.toBitmap(ICON_SIZE, ICON_SIZE)?.asImageBitmap()
        }.getOrNull()
        return AppEntry(
            packageName = info.activityInfo.packageName,
            label = info.loadLabel(pm).toString(),
            icon = icon,
        )
    }

    private companion object {
        const val ICON_SIZE = 144
    }
}
