// com/sniperoftdm/motionclueandro/service/MotionService.kt
package com.sniperoftdm.motionclueandro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.core.content.ContextCompat
import com.sniperoftdm.motionclueandro.MotionClueApp
import com.sniperoftdm.motionclueandro.R
import com.sniperoftdm.motionclueandro.bridge.EngineConfig
import com.sniperoftdm.motionclueandro.bridge.MotionEngine
import com.sniperoftdm.motionclueandro.overlay.OverlayController
import com.sniperoftdm.motionclueandro.tile.MotionTileService
import com.sniperoftdm.motionclueandro.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service owning the engine, overlay, and GPS speed source.
 * On screen off: stops engine, GPS, and frame callbacks.
 * On screen on: resumes everything.
 * Registers a DisplayListener to push display rotation into config slot 7.
 */
class MotionService : Service() {

    companion object {
        private const val TAG = "MotionService"
        private const val CHANNEL_ID = "motion_cues"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.sniperoftdm.motionclueandro.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MotionService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MotionService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var engine: MotionEngine? = null
    private var overlay: OverlayController? = null
    private var gpsSource: GpsSpeedSource? = null
    private var displayManager: DisplayManager? = null
    private var destroyed = false

    // Cached values for rebuilding config on rotation change
    private var currentSensitivity = 1.0f
    private var currentHybridBlend = 0.5f
    private var currentRotation = Surface.ROTATION_0

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
            }
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                val rotation = getDefaultDisplayRotation()
                if (rotation != currentRotation) {
                    currentRotation = rotation
                    engine?.setConfig(
                        EngineConfig.build(currentSensitivity, currentHybridBlend, currentRotation),
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (hasLocationPermission()) {
                fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (hasLocationPermission()) {
                fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startEngine()
        return START_STICKY
    }

    private fun getDefaultDisplayRotation(): Int {
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return Surface.ROTATION_0
        return display.rotation
    }

    private fun startEngine() {
        if (engine != null) return

        val app = application as MotionClueApp
        val eng = app.createEngine()
        engine = eng
        destroyed = false

        serviceScope.launch {
            val settings = app.settingsStore
            val prefs = settings.settingsFlow.first()

            currentSensitivity = prefs.sensitivity
            currentHybridBlend = prefs.hybridBlend
            currentRotation = getDefaultDisplayRotation()

            val config = EngineConfig.build(
                sensitivity = currentSensitivity,
                hybridBlend = currentHybridBlend,
                displayRotation = currentRotation,
            )
            eng.setConfig(config)

            // Register display listener for rotation changes
            displayManager?.registerDisplayListener(displayListener, null)

            if (eng.start()) {
                val oc = OverlayController(this@MotionService, eng)
                oc.show(
                    dotSizeDp = prefs.dotSizeDp,
                    dotColor = prefs.dotColorInt,
                    dotOpacity = prefs.opacity,
                    dotCountPerColumn = prefs.dotCount,
                    edgeBandDp = prefs.edgeBandDp,
                    invertDirection = prefs.invertDirection,
                    adaptiveBrightness = prefs.adaptiveBrightness,
                )
                overlay = oc

                if (prefs.useGps && hasLocationPermission()) {
                    val gps = GpsSpeedSource(this@MotionService, eng)
                    gps.start()
                    gpsSource = gps
                }

                // Observe settings changes while running
                serviceScope.launch {
                    settings.settingsFlow.collect { s ->
                        currentSensitivity = s.sensitivity
                        currentHybridBlend = s.hybridBlend
                        eng.setConfig(
                            EngineConfig.build(s.sensitivity, s.hybridBlend, currentRotation),
                        )
                        oc.updateSettings(
                            dotSizeDp = s.dotSizeDp,
                            dotColor = s.dotColorInt,
                            dotOpacity = s.opacity,
                            dotCountPerColumn = s.dotCount,
                            edgeBandDp = s.edgeBandDp,
                            invertDirection = s.invertDirection,
                            adaptiveBrightness = s.adaptiveBrightness,
                        )
                        // If GPS setting toggled at runtime
                        if (s.useGps && hasLocationPermission()) {
                            if (gpsSource == null) {
                                val gps = GpsSpeedSource(this@MotionService, eng)
                                gps.start()
                                gpsSource = gps
                            }
                        } else {
                            gpsSource?.stop()
                            gpsSource = null
                        }
                    }
                }

                app.setRunning(true)
                tryUpdateTile()
            } else {
                Log.e(TAG, "Engine failed to start")
                stopSelf()
            }
        }
    }

    private fun onScreenOff() {
        overlay?.pauseRendering()
        engine?.stop()
        gpsSource?.stop()
    }

    private fun onScreenOn() {
        engine?.start()
        gpsSource?.start()
        overlay?.resumeRendering()
    }

    private fun tryUpdateTile() {
        try {
            TileService.requestListeningState(
                this,
                ComponentName(this, MotionTileService::class.java),
            )
        } catch (e: Exception) {
            // Not fatal if tile service cannot be updated
        }
    }

    override fun onDestroy() {
        (application as? MotionClueApp)?.setRunning(false)
        tryUpdateTile()
        overlay?.hide()
        overlay = null
        gpsSource?.stop()
        gpsSource = null
        displayManager?.unregisterDisplayListener(displayListener)
        // Stop then destroy; never touch engine after destroy
        val eng = engine
        engine = null
        if (eng != null && !destroyed) {
            destroyed = true
            eng.release() // release calls nativeStop then nativeDestroy
        }
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Motion cue overlay status"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, MotionService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(openPending)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.notification_stop),
                    stopPending,
                ).build(),
            )
            .setOngoing(true)
            .build()
    }
}
