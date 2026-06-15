package com.tertiaryinfotech.runtrackgps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.runtrackgps.util.PaceCalculator
import com.tertiaryinfotech.runtrackgps.vm.RunViewModel

@Composable
fun RunScreen(vm: RunViewModel) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        MapSection(vm, Modifier.weight(1f))
        MetricsSection(vm)
        Controls(vm)
    }
}

@Composable
private fun MapSection(vm: RunViewModel, modifier: Modifier) {
    Box(modifier.fillMaxWidth()) {
        RouteMap(
            route = vm.route,
            currentLocation = vm.currentLocation,
            followUser = vm.followUser,
            locationPermitted = vm.isLocationAuthorized,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
            onCameraMovedByUser = { vm.setFollow(false) },
        )
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VoiceIndicator(vm.isListening)
            if (vm.isAccuracyPoor) {
                StatusChip(Icons.Filled.Warning, "Weak GPS", Color(0xFFFF9800))
            }
            RecenterButton { vm.recenter() }
        }
    }
}

@Composable
private fun VoiceIndicator(listening: Boolean) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                if (listening) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                tint = if (listening) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                if (listening) "Listening" else "Voice off",
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp, shadowElevation = 2.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecenterButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape, tonalElevation = 3.dp, shadowElevation = 2.dp,
        modifier = Modifier.size(48.dp).clickableNoRipple(onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MetricsSection(vm: RunViewModel) {
    val progress = if (vm.goalDistanceMeters > 0)
        (vm.distanceMeters / vm.goalDistanceMeters).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining = (vm.goalDistanceMeters - vm.distanceMeters).coerceAtLeast(0.0)

    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Goal: ${PaceCalculator.formatKm(vm.goalDistanceMeters)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                PaceCalculator.formatKm(vm.distanceMeters),
                fontSize = 52.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(
                "Remaining: ${PaceCalculator.formatKm(remaining)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Stat("Time", PaceCalculator.formatTime(vm.elapsed), Modifier.weight(1f))
            Divider(Modifier.size(width = 1.dp, height = 48.dp))
            Stat("Pace (min/km)", PaceCalculator.formatShort(vm.averagePaceSecPerKm), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Controls(vm: RunViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (vm.isPaused) {
            ControlButton("Resume", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.primary,
                Modifier.weight(1f)) { vm.resume() }
        } else {
            ControlButton("Pause", Icons.Filled.Pause, Color(0xFFFF9800),
                Modifier.weight(1f)) { vm.pause() }
        }
        ControlButton("Stop", Icons.Filled.Stop, Color(0xFFE53935), Modifier.weight(1f)) {
            vm.stop(completed = false)
        }
    }
}

@Composable
private fun ControlButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = tint, contentColor = Color.White),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
