package com.tertiaryinfotech.runtrackgps

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.runtrackgps.ui.RootScreen
import com.tertiaryinfotech.runtrackgps.ui.theme.RunTrackGPSTheme
import com.tertiaryinfotech.runtrackgps.vm.RunViewModel

class MainActivity : ComponentActivity() {

    private var viewModel: RunViewModel? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Refresh permission snapshots so the banners/Start guard update.
        viewModel?.refreshPermissions()
    }

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

    /** Prime location + mic permissions on first launch. */
    private fun requestStartupPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }
}
