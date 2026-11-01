package com.sniperoftdm.motionclueandro.bridge

import android.os.SystemClock

/**
 * Pure-Kotlin simulation for testing without native library. Contract v2.
 * Slow sinusoidal shift, constant flow along +Y, fake speed.
 * Fills v2 fields with plausible values (lux, proximity far, flags).
 */
class StubMotionEngine : MotionEngine {
    @Volatile private var running = false
    private var startTimeNanos = 0L

    override val isNative: Boolean get() = false

    override fun start(): Boolean {
        if (running) return true
        running = true
        startTimeNanos = SystemClock.elapsedRealtimeNanos()
        return true
    }

    override fun stop() {
        running = false
    }

    override fun setFix(
        speedMps: Float,
        speedAccMps: Float,
        bearingDeg: Float,
        bearingAccDeg: Float,
        elapsedRealtimeNanos: Long,
    ) {
        // stub: ignored
    }

    override fun setConfig(params: FloatArray) {
        // stub: ignored
    }

    override fun resetCalibration() {
        // stub: ignored
    }

    override fun describeSensors(): String {
        return "[stub] Accelerometer: Fake Sensor (StubVendor) 0-200 Hz non-wakeup\n" +
            "[stub] Gyroscope: Fake Sensor (StubVendor) 0-200 Hz non-wakeup\n" +
            "[stub] Light: Fake Sensor (StubVendor) 0-50 Hz non-wakeup\n" +
            "[stub] Proximity: Fake Sensor (StubVendor) 0-5 Hz wakeup\n" +
            "Engine uses: accelerometer, gyroscope"
    }

    override fun readFrame(out: MotionFrame): Boolean {
        if (!running) return false
        val elapsed = (SystemClock.elapsedRealtimeNanos() - startTimeNanos) / 1_000_000_000.0
        val sin = kotlin.math.sin(elapsed * 0.8).toFloat()
        val cos = kotlin.math.cos(elapsed * 0.5).toFloat()

        // flags: sensors running + calibrated + speed valid + gravity settled
        out.flags = 0x17 // bits 0,1,2,4
        out.shiftX = sin * 0.3f
        out.shiftY = cos * 0.15f
        out.flowDirX = 0f
        out.flowDirY = 1f // forward = screen-up
        out.flowRate = 0.5f // half a dot spacing per second
        out.speedMps = 12f // ~43 km/h fake speed
        out.longAccel = sin * 1.5f
        out.latAccel = cos * 0.8f
        out.yawRate = 0f
        out.confidence = 0.9f
        out.sensorMask = 0x103 // accel + gyro + light
        out.timestampNanos = SystemClock.elapsedRealtimeNanos()
        out.ambientLux = 350f // indoor ambient
        out.proximityCm = 5f  // far
        out.pressureHpa = 1013.25f
        out.climbRate = 0f
        out.headingRad = 0f
        out.tiltDeg = 15f
        out.viewBlend = 0.3f
        out.stationarySec = 0f
        out.measuredRateHz = 100f
        out.droppedSamples = 0
        return true
    }

    override fun release() {
        running = false
    }
}
