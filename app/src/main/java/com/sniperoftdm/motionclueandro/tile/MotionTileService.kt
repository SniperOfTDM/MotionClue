// com/sniperoftdm/motionclueandro/tile/MotionTileService.kt
package com.sniperoftdm.motionclueandro.tile

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.sniperoftdm.motionclueandro.MotionClueApp
import com.sniperoftdm.motionclueandro.R
import com.sniperoftdm.motionclueandro.service.MotionService
import com.sniperoftdm.motionclueandro.ui.MainActivity

/**
 * Quick Settings tile. Toggles MotionService on/off.
 * If required permissions are missing, collapses the shade and opens MainActivity.
 */
class MotionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val hasOverlay = Settings.canDrawOverlays(this)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasOverlay || !hasNotification) {
            openMainActivity()
            return
        }

        val app = application as? MotionClueApp
        val isRunning = app?.isRunning?.value ?: false

        if (isRunning) {
            MotionService.stop(this)
            setTileActive(false)
        } else {
            MotionService.start(this)
            setTileActive(true)
        }
    }

    private fun updateTileState() {
        val app = application as? MotionClueApp
        val isRunning = app?.isRunning?.value ?: false
        setTileActive(isRunning)
    }

    private fun setTileActive(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
