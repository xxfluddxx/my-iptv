package com.myiptv.carlauncher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class HomeState(
    val loading: Boolean = true,
    val dock: List<AppEntry> = emptyList(),
    val allApps: List<AppEntry> = emptyList(),
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppRepository(app)
    private val store = DockStore(app)
    val media = MediaSessions(app)
    val speed = SpeedSource(app)

    private var apps: List<AppEntry> = emptyList()
    private var dock: List<String> = emptyList()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _useMph = MutableStateFlow(store.useMph ?: defaultsToMph())
    val useMph: StateFlow<Boolean> = _useMph.asStateFlow()

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    /** Reload installed apps. Called every time the home screen comes back. */
    fun refresh() {
        viewModelScope.launch {
            apps = repository.loadApps()
            dock = store.load(apps.mapTo(HashSet()) { it.packageName })
            publish()
        }
    }

    /** Media and GPS only run while the home screen is showing. */
    fun startLive() {
        media.start()
        speed.start()
    }

    fun stopLive() {
        media.stop()
        speed.stop()
    }

    fun setDrawerOpen(open: Boolean) {
        _drawerOpen.value = open
    }

    fun toggleSpeedUnit() {
        _useMph.value = !_useMph.value
        store.useMph = _useMph.value
    }

    fun isInDock(packageName: String) = packageName in dock

    fun toggleDock(packageName: String) {
        dock = if (packageName in dock) dock - packageName else dock + packageName
        store.save(dock)
        publish()
    }

    /** Moves a dock app one place left (-1) or right (+1) among the visible dock apps. */
    fun moveInDock(packageName: String, delta: Int) {
        val visible = _state.value.dock.map { it.packageName }.toMutableList()
        val from = visible.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return
        visible.add(to, visible.removeAt(from))
        // Keep dock apps that aren't installed right now at the end, so they come back if reinstalled
        dock = visible + dock.filter { it !in visible }
        store.save(dock)
        publish()
    }

    fun launchIntent(packageName: String): Intent? = repository.launchIntent(packageName)

    override fun onCleared() {
        stopLive()
    }

    private fun publish() {
        val byPackage = apps.associateBy { it.packageName }
        _state.value = HomeState(
            loading = false,
            dock = dock.mapNotNull { byPackage[it] },
            allApps = apps,
        )
    }

    private fun defaultsToMph() = Locale.getDefault().country in setOf("US", "GB", "LR", "MM")
}
