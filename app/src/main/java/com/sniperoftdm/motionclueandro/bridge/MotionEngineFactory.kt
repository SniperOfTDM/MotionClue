package com.sniperoftdm.motionclueandro.bridge

import android.util.Log

/**
 * Creates the appropriate MotionEngine implementation.
 * Tries to load the native library; falls back to StubMotionEngine on failure.
 */
object MotionEngineFactory {
    private const val TAG = "MotionEngineFactory"
    private var loggedFallback = false

    fun create(): MotionEngine {
        return try {
            System.loadLibrary("motionclue_native")
            NativeMotionEngine()
        } catch (e: UnsatisfiedLinkError) {
            if (!loggedFallback) {
                Log.w(TAG, "Native library not found, using stub engine", e)
                loggedFallback = true
            }
            StubMotionEngine()
        }
    }
}
