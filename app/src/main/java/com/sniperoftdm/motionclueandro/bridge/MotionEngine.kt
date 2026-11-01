package com.sniperoftdm.motionclueandro.bridge

/**
 * Abstraction over the native motion processing engine. Contract v2.
 */
interface MotionEngine {
    fun start(): Boolean
    fun stop()
    fun setFix(
        speedMps: Float,
        speedAccMps: Float,
        bearingDeg: Float,
        bearingAccDeg: Float,
        elapsedRealtimeNanos: Long,
    )
    fun setConfig(params: FloatArray)
    fun resetCalibration()
    fun describeSensors(): String
    fun readFrame(out: MotionFrame): Boolean
    fun release()

    /** True if this is the real native engine, false for stub. */
    val isNative: Boolean
}
