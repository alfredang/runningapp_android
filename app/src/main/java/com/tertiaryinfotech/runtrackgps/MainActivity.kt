package com.tertiaryinfotech.runtrackgps

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.runtrackgps.ui.RootScreen
import com.tertiaryinfotech.runtrackgps.ui.theme.RunTrackGPSTheme
import com.tertiaryinfotech.runtrackgps.vm.RunViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var viewModel: RunViewModel? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Refresh permission snapshots so the banners/Start guard update.
        viewModel?.refreshPermissions()
        // After foreground location is granted, escalate to background on Android 10+.
        maybeRequestBackgroundLocation()
    }

    private val backgroundLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel?.refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunTrackGPSTheme {
                val vm: RunViewModel = viewModel()
                viewModel = vm
                RootScreen(vm)
            }
        }
        requestStartupPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel?.refreshPermissions()
    }

    /** Prime location + mic + notification permissions on first launch. */
    private fun requestStartupPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    /**
     * Background location must be requested separately, and only after foreground
     * location is already granted (Android policy). Slight delay so it doesn't
     * collide with the first prompt's result.
     */
    private fun maybeRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val vm = viewModel ?: return
        if (vm.isLocationAuthorized && !vm.hasBackgroundAuthorization) {
            lifecycleScope.launch {
                delay(400)
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }
}
