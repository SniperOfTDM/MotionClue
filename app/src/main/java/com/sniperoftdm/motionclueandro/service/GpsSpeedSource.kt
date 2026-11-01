// com/sniperoftdm/motionclueandro/service/GpsSpeedSource.kt
package com.sniperoftdm.motionclueandro.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.sniperoftdm.motionclueandro.bridge.MotionEngine

/**
 * GPS-based fix source using framework LocationManager GPS_PROVIDER at 1 s.
 * Passes speed, speedAccuracy, bearing, bearingAccuracy, and elapsedRealtimeNanos
 * via engine.setFix. -1 when a value is unavailable.
 */
class GpsSpeedSource(
    private val context: Context,
    private val engine: MotionEngine,
) : SpeedSource {

    companion object {
        private const val TAG = "GpsSpeedSource"
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var isListening = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speedMps = if (location.hasSpeed()) location.speed else -1f
            val speedAccMps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
                location.speedAccuracyMetersPerSecond
            } else {
                -1f
            }
            val bearingDeg = if (location.hasBearing()) location.bearing else -1f
            val bearingAccDeg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasBearingAccuracy()) {
                location.bearingAccuracyDegrees
            } else {
                -1f
            }
            val elapsedRealtimeNanos = location.elapsedRealtimeNanos
            engine.setFix(speedMps, speedAccMps, bearingDeg, bearingAccDeg, elapsedRealtimeNanos)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (isListening || locationManager == null) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Location permission not granted, GPS speed disabled")
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.w(TAG, "GPS provider disabled on device")
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener,
                Looper.getMainLooper(),
            )
            isListening = true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
        }
    }

    override fun stop() {
        if (!isListening || locationManager == null) return
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.w(TAG, "Error removing location updates", e)
        } finally {
            isListening = false
        }
    }
}
