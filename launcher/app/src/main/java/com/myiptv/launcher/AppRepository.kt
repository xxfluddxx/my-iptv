package com.myiptv.launcher

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
    /** TV banner (16:9) if the app has one, else null and [icon] is shown. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
)

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    /** Every launchable app except this launcher, sorted by name. */
    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val byPackage = LinkedHashMap<String, ResolveInfo>()
        // TV entry points first, so an app's TV activity (and banner) wins over its phone one
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, 0)) {
                byPackage.putIfAbsent(info.activityInfo.packageName, info)
            }
        }
        byPackage.remove(context.packageName)
        byPackage.values.map(::toEntry).sortedBy { it.label.lowercase() }
    }

    fun launchIntent(packageName: String): Intent? =
        (pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName))
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun toEntry(info: ResolveInfo): AppEntry {
        val activity = info.activityInfo
        val banner = runCatching {
            activity.loadBanner(pm)?.toBitmap(BANNER_W, BANNER_H)?.asImageBitmap()
        }.getOrNull()
        val icon = runCatching {
            info.loadIcon(pm)?.toBitmap(ICON_SIZE, ICON_SIZE)?.asImageBitmap()
        }.getOrNull()
        return AppEntry(
            packageName = activity.packageName,
            label = info.loadLabel(pm).toString(),
            banner = banner,
            icon = icon,
        )
    }

    private companion object {
        const val BANNER_W = 320
        const val BANNER_H = 180
        const val ICON_SIZE = 144
    }
}
