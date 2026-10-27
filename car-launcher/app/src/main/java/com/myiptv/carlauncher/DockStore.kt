package com.myiptv.carlauncher

import android.content.Context

/** Ordered list of dock package names, kept in SharedPreferences. */
class DockStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /**
     * Returns the saved dock. Apps from R.array.default_dock are added the first
     * time they're seen installed, but never re-added once the user has removed them.
     */
    fun load(installed: Set<String>): List<String> {
        val dock = read(KEY_DOCK).toMutableList()
        val seeded = read(KEY_SEEDED).toMutableList()
        val defaults = context.resources.getStringArray(R.array.default_dock)
        var changed = false
        for (pkg in defaults) {
            if (pkg in installed && pkg !in seeded) {
                seeded += pkg
                if (pkg !in dock) dock += pkg
                changed = true
            }
        }
        if (changed) {
            write(KEY_SEEDED, seeded)
            write(KEY_DOCK, dock)
        }
        return dock
    }

    fun save(dock: List<String>) = write(KEY_DOCK, dock)

    /** null until the user picks a unit by tapping the speed card. */
    var useMph: Boolean?
        get() = if (prefs.contains(KEY_MPH)) prefs.getBoolean(KEY_MPH, false) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_MPH) else putBoolean(KEY_MPH, value) }.apply()
        }

    private fun read(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(',').filter { it.isNotBlank() }

    private fun write(key: String, list: List<String>) {
        prefs.edit().putString(key, list.joinToString(",")).apply()
    }

    private companion object {
        const val KEY_DOCK = "dock"
        const val KEY_SEEDED = "seeded_defaults"
        const val KEY_MPH = "use_mph"
    }
}
