package com.myiptv.launcher

import android.content.Context

/** Ordered list of favorite package names, kept in SharedPreferences. */
class FavoritesStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /**
     * Returns the saved favorites. Apps from R.array.default_favorites are added
     * the first time they're seen installed (apps bundled in the firmware can be
     * installed a little after the launcher first starts), but never re-added
     * once the user has removed them.
     */
    fun load(installed: Set<String>): List<String> {
        val favorites = read(KEY_FAVORITES).toMutableList()
        val seeded = read(KEY_SEEDED).toMutableList()
        val defaults = context.resources.getStringArray(R.array.default_favorites)
        var changed = false
        for (pkg in defaults) {
            if (pkg in installed && pkg !in seeded) {
                seeded += pkg
                if (pkg !in favorites) favorites += pkg
                changed = true
            }
        }
        if (changed) {
            write(KEY_SEEDED, seeded)
            write(KEY_FAVORITES, favorites)
        }
        return favorites
    }

    fun save(favorites: List<String>) = write(KEY_FAVORITES, favorites)

    private fun read(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(',').filter { it.isNotBlank() }

    private fun write(key: String, list: List<String>) {
        prefs.edit().putString(key, list.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_SEEDED = "seeded_defaults"
    }
}
