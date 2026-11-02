// com/sniperoftdm/motionclueandro/ui/SettingsSection.kt
package com.sniperoftdm.motionclueandro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sniperoftdm.motionclueandro.data.SettingsStore
import com.sniperoftdm.motionclueandro.data.TransportPreset
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsSection(
    settingsStore: SettingsStore,
    onResetCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsStore.settingsFlow.collectAsState(initial = SettingsStore.Settings())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)

            // Transport Presets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Vehicle Profile",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TransportPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.presetName == preset.name,
                            onClick = {
                                coroutineScope.launch { settingsStore.applyPreset(preset) }
                            },
                            label = { Text(preset.title) },
                        )
                    }
                }
            }

            // Sensitivity: 0.2 to 3.0
            SettingSlider(
                label = String.format(Locale.US, "Sensitivity: %.1fx", settings.sensitivity),
                value = settings.sensitivity,
                valueRange = 0.2f..3.0f,
                onValueChange = { coroutineScope.launch { settingsStore.updateSensitivity(it) } },
            )

            // Hybrid blend: 0.0 to 1.0 (Acceleration <-> Speed flow)
            SettingSlider(
                label = String.format(
                    Locale.US,
                    "Acceleration <-> Speed flow: %.2f",
                    settings.hybridBlend,
                ),
                value = settings.hybridBlend,
                valueRange = 0f..1f,
                onValueChange = { coroutineScope.launch { settingsStore.updateHybridBlend(it) } },
            )

            // Dot size: 3 to 12 dp
            SettingSlider(
                label = String.format(Locale.US, "Dot size: %.0f dp", settings.dotSizeDp),
                value = settings.dotSizeDp,
                valueRange = 3f..12f,
                steps = 8,
                onValueChange = { coroutineScope.launch { settingsStore.updateDotSizeDp(it) } },
            )

            // Opacity: 0.2 to 1.0
            SettingSlider(
                label = String.format(Locale.US, "Opacity: %.0f%%", settings.opacity * 100f),
                value = settings.opacity,
                valueRange = 0.2f..1f,
                onValueChange = { coroutineScope.launch { settingsStore.updateOpacity(it) } },
            )

            // Color: White, Black, Yellow, Gray
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Dot color",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val colors = listOf("White", "Black", "Yellow", "Gray")
                    colors.forEach { colorName ->
                        FilterChip(
                            selected = settings.dotColorName == colorName,
                            onClick = {
                                coroutineScope.launch { settingsStore.updateDotColor(colorName) }
                            },
                            label = { Text(colorName) },
                        )
                    }
                }
            }

            // Density: 6 to 20 dots per edge
            SettingSlider(
                label = String.format(
                    Locale.US,
                    "Density: %d dots per long edge",
                    settings.dotCount,
                ),
                value = settings.dotCount.toFloat(),
                valueRange = 6f..20f,
                steps = 13,
                onValueChange = {
                    coroutineScope.launch { settingsStore.updateDotCount(it.toInt()) }
                },
            )

            // Edge band width: 16 to 64 dp
            SettingSlider(
                label = String.format(
                    Locale.US,
                    "Edge band width: %.0f dp",
                    settings.edgeBandDp,
                ),
                value = settings.edgeBandDp,
                valueRange = 16f..64f,
                onValueChange = { coroutineScope.launch { settingsStore.updateEdgeBandDp(it) } },
            )

            // Invert direction: boolean
            SettingSwitch(
                label = "Invert direction",
                subtitle = "Dots move opposite to vehicle motion",
                checked = settings.invertDirection,
                onCheckedChange = {
                    coroutineScope.launch { settingsStore.updateInvertDirection(it) }
                },
            )

            // Use GPS: boolean
            SettingSwitch(
                label = "Use GPS",
                subtitle = "Enables vehicle speed flow",
                checked = settings.useGps,
                onCheckedChange = {
                    coroutineScope.launch { settingsStore.updateUseGps(it) }
                },
            )

            // Adaptive brightness: boolean (default on)
            SettingSwitch(
                label = "Adaptive brightness",
                subtitle = "Adjusts dot opacity based on ambient light",
                checked = settings.adaptiveBrightness,
                onCheckedChange = {
                    coroutineScope.launch { settingsStore.updateAdaptiveBrightness(it) }
                },
            )

            // Reset calibration button
            Button(
                onClick = onResetCalibration,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset calibration")
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
