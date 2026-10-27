package com.myiptv.carlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Current GPS speed in meters per second, or null while there's no fix. */
class SpeedSource(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _speed = MutableStateFlow<Float?>(null)
    val speed: StateFlow<Float?> = _speed.asStateFlow()

    private var listening = false

    // A full object rather than a lambda: before Android 11 these other methods
    // aren't default methods, and leaving them out crashes on head units running Android 10
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _speed.value = if (location.hasSpeed()) location.speed else null
        }

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) {
            _speed.value = null
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    @SuppressLint("MissingPermission") // checked just above the request
    fun start() {
        _hasPermission.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!_hasPermission.value || listening) return
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            listening = true
        } catch (e: SecurityException) {
            _hasPermission.value = false
        } catch (e: IllegalArgumentException) {
            // No GPS on this device
        }
    }

    fun stop() {
        if (listening) {
            manager.removeUpdates(listener)
            listening = false
        }
        _speed.value = null
    }
}
