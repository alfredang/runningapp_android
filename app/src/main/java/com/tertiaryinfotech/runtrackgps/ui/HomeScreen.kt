package com.tertiaryinfotech.runtrackgps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.runtrackgps.model.RunSession
import com.tertiaryinfotech.runtrackgps.util.PaceCalculator
import com.tertiaryinfotech.runtrackgps.vm.RunViewModel

private data class Preset(val meters: Double, val label: String)

private val presets = listOf(
    Preset(5_000.0, "5 KM"),
    Preset(10_000.0, "10 KM"),
    Preset(20_000.0, "20 KM"),
    Preset(40_000.0, "40 KM"),
)

@Composable
fun HomeScreen(vm: RunViewModel, onShowHistory: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Header()
        PermissionBanner(vm)
        PresetGrid(vm)
        CustomInput(vm)
        GoalDisplay(vm)
        StartButton(vm)
        RecentRun(vm, onShowHistory)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.DirectionsRun, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp),
        )
        Text("RunTrack GPS", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "Track your run. Reach your goal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionBanner(vm: RunViewModel) {
    when {
        !vm.isLocationAuthorized -> Banner(
            Icons.Filled.LocationOff,
            "Location access is required to track your run.",
            Color(0xFFFF9800),
        )
        !vm.hasBackgroundAuthorization -> Banner(
            Icons.Filled.NightlightRound,
            "Allow \"Allow all the time\" location for full background tracking.",
            Color(0xFFFFC107),
        )
    }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PresetGrid(vm: RunViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
        contentPadding = PaddingValues(0.dp),
    ) {
        items(presets, key = { it.meters }) { preset ->
            val selected = vm.isPresetSelected && vm.goalDistanceMeters == preset.meters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp),
                    )
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickableNoRipple { vm.selectPreset(preset.meters) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    preset.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CustomInput(vm: RunViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom distance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vm.customDistanceText,
                onValueChange = { vm.customDistanceText = it },
                placeholder = { Text("e.g. 7.5") },
                suffix = { Text("km") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { vm.applyCustomGoal() }, modifier = Modifier.height(56.dp)) {
                Text("Set", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GoalDisplay(vm: RunViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "GOAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            PaceCalculator.formatKm(vm.goalDistanceMeters),
            fontSize = 44.sp, fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StartButton(vm: RunViewModel) {
    Button(
        onClick = { vm.startRun() },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Start Run", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecentRun(vm: RunViewModel, onShowHistory: () -> Unit) {
    val run: RunSession = vm.mostRecentRun ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent Run", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onShowHistory) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("View All (${vm.pastRuns.size})", fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SummaryItem("Distance", PaceCalculator.formatKm(run.distanceMeters), Modifier.weight(1f))
                Divider(Modifier.height(36.dp).width(1.dp))
                SummaryItem("Time", PaceCalculator.formatTime(run.elapsedTime), Modifier.weight(1f))
                Divider(Modifier.height(36.dp).width(1.dp))
                SummaryItem("Pace", PaceCalculator.format(run.averagePaceSecPerKm), Modifier.weight(1f))
            }
            run.endTimeEpoch?.let {
                Text(
                    formatDate(it), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Text(title, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
