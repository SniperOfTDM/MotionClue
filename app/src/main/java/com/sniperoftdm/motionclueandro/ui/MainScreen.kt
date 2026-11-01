// com/sniperoftdm/motionclueandro/ui/MainScreen.kt
package com.sniperoftdm.motionclueandro.ui

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sniperoftdm.motionclueandro.EngineStatus
import com.sniperoftdm.motionclueandro.MotionClueApp
import com.sniperoftdm.motionclueandro.service.MotionService

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MotionClueApp
    val isRunning by app.isRunning.collectAsState()
    val engineStatus by app.engineStatus.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "MotionClue",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Motion cues to reduce motion sickness",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // 1. Setup card
            SetupCard()

            Spacer(Modifier.height(24.dp))

            // 2. Big Start/Stop button
            Button(
                onClick = {
                    if (isRunning) {
                        MotionService.stop(context)
                    } else {
                        if (!Settings.canDrawOverlays(context)) return@Button
                        MotionService.start(context)
                    }
                },
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE53935)
                    else Color(0xFF43A047),
                ),
            ) {
                Text(
                    if (isRunning) "Stop" else "Start",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Status line: Sensors, Calibration, Speed, Engine, Gravity, Activity
            StatusLine(status = engineStatus, isRunning = isRunning)

            Spacer(Modifier.height(24.dp))

            // 3. Settings section
            SettingsSection(
                settingsStore = app.settingsStore,
                onResetCalibration = {
                    app.activeEngine?.resetCalibration()
                },
            )

            Spacer(Modifier.height(24.dp))

            // 4. Battery and background section
            BatterySection()

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusLine(status: EngineStatus, isRunning: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatusChip(
            label = "Sensors",
            value = if (isRunning) status.sensorsText else "stopped",
        )
        StatusChip(
            label = "Calibration",
            value = if (isRunning) status.calibrationText else "none",
        )
        StatusChip(
            label = "Speed",
            value = if (isRunning) status.speedText else "none",
        )
        StatusChip(
            label = "Gravity",
            value = if (isRunning) status.gravityText else "none",
        )
        StatusChip(
            label = "Activity",
            value = if (isRunning) status.activityText else "none",
        )
        StatusChip(
            label = "Engine",
            value = status.engineText,
        )
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
