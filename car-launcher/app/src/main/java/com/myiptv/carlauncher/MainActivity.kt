package com.myiptv.carlauncher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.speed.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onRequestLocation = {
                        askLocation.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    },
                )
            }
        }
    }

    // Pressing Home while already home lands here: go back to the main screen
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.setDrawerOpen(false)
    }

    override fun onStart() {
        super.onStart()
        // Also re-checks access after the user comes back from the permission screens
        viewModel.startLive()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopLive()
    }
}
