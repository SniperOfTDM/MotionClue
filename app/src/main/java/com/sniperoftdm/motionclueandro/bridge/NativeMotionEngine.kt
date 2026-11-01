package com.sniperoftdm.motionclueandro.bridge

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real JNI bridge to libmotionclue_native.so. Contract v2.
 * Owns the direct ByteBuffer (128 bytes, nativeOrder) and the native handle.
 */
class NativeMotionEngine : MotionEngine {
    private val buffer: ByteBuffer = ByteBuffer.allocateDirect(FrameBuffer.BUFFER_CAPACITY)
        .order(ByteOrder.nativeOrder())

    @Volatile
    private var handle: Long = 0L

    override val isNative: Boolean get() = true

    init {
        handle = nativeCreate(buffer)
    }

    override fun start(): Boolean {
        val h = handle
        if (h == 0L) return false
        return nativeStart(h)
    }

    override fun stop() {
        val h = handle
        if (h == 0L) return
        nativeStop(h)
    }

    override fun setFix(
        speedMps: Float,
        speedAccMps: Float,
        bearingDeg: Float,
        bearingAccDeg: Float,
        elapsedRealtimeNanos: Long,
    ) {
        val h = handle
        if (h == 0L) return
        nativeSetFix(h, speedMps, speedAccMps, bearingDeg, bearingAccDeg, elapsedRealtimeNanos)
    }

    override fun setConfig(params: FloatArray) {
        val h = handle
        if (h == 0L) return
        nativeSetConfig(h, params)
    }

    override fun resetCalibration() {
        val h = handle
        if (h == 0L) return
        nativeResetCalibration(h)
    }

    override fun describeSensors(): String {
        val h = handle
        if (h == 0L) return ""
        return nativeDescribeSensors(h)
    }

    override fun readFrame(out: MotionFrame): Boolean {
        if (handle == 0L) return false
        return FrameBuffer.read(buffer, out)
    }

    override fun release() {
        val h = handle
        if (h == 0L) return
        handle = 0L
        nativeStop(h)
        nativeDestroy(h)
    }

    // JNI declarations — instance methods, handle passed explicitly
    private external fun nativeCreate(buffer: ByteBuffer): Long
    private external fun nativeStart(handle: Long): Boolean
    private external fun nativeStop(handle: Long)
    private external fun nativeSetFix(
        handle: Long,
        speedMps: Float,
        speedAccMps: Float,
        bearingDeg: Float,
        bearingAccDeg: Float,
        elapsedRealtimeNanos: Long,
    )
    private external fun nativeSetConfig(handle: Long, params: FloatArray)
    private external fun nativeResetCalibration(handle: Long)
    private external fun nativeDescribeSensors(handle: Long): String
    private external fun nativeDestroy(handle: Long)
}
