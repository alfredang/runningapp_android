package com.tertiaryinfotech.runtrackgps.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tertiaryinfotech.runtrackgps.model.AppScreen
import com.tertiaryinfotech.runtrackgps.vm.RunViewModel

/**
 * Top-level screen router. Switches between the three screens, hosts the shared
 * alert, and presents the History sheet (mirrors the iOS RootView).
 */
@Composable
fun RootScreen(vm: RunViewModel) {
    var showHistory by remember { mutableStateOf(false) }

    Crossfade(targetState = vm.screen, animationSpec = tween(250), label = "screen") { screen ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(vm, onShowHistory = { showHistory = true })
            AppScreen.RUNNING -> RunScreen(vm)
            AppScreen.COMPLETION -> CompletionScreen(vm)
        }
    }

    if (showHistory) {
        BackHandler { showHistory = false }
        HistoryScreen(vm, onClose = { showHistory = false })
    }

    vm.activeAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { vm.activeAlert = null },
            title = { Text(alert.title) },
            text = { Text(alert.message) },
            confirmButton = { TextButton(onClick = { vm.activeAlert = null }) { Text("OK") } },
        )
    }
}
