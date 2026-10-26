package com.myiptv.launcher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val favorites: List<AppEntry> = emptyList(),
    val otherApps: List<AppEntry> = emptyList(),
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppRepository(app)
    private val store = FavoritesStore(app)

    private var apps: List<AppEntry> = emptyList()
    private var favorites: List<String> = emptyList()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** Reload installed apps. Called every time the home screen comes back. */
    fun refresh() {
        viewModelScope.launch {
            apps = repository.loadApps()
            favorites = store.load(apps.mapTo(HashSet()) { it.packageName })
            publish()
        }
    }

    fun isFavorite(packageName: String) = packageName in favorites

    fun toggleFavorite(packageName: String) {
        favorites = if (packageName in favorites) favorites - packageName else favorites + packageName
        store.save(favorites)
        publish()
    }

    /** Moves a favorite one place left (-1) or right (+1) among the visible favorites. */
    fun moveFavorite(packageName: String, delta: Int) {
        val visible = _state.value.favorites.map { it.packageName }.toMutableList()
        val from = visible.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return
        visible.add(to, visible.removeAt(from))
        // Keep favorites that aren't installed right now at the end, so they come back if reinstalled
        favorites = visible + favorites.filter { it !in visible }
        store.save(favorites)
        publish()
    }

    fun launchIntent(packageName: String): Intent? = repository.launchIntent(packageName)

    private fun publish() {
        val byPackage = apps.associateBy { it.packageName }
        _state.value = HomeState(
            loading = false,
            favorites = favorites.mapNotNull { byPackage[it] },
            otherApps = apps.filter { it.packageName !in favorites },
        )
    }
}
