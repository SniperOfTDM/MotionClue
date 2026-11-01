// com/sniperoftdm/motionclueandro/MotionClueApp.kt
package com.sniperoftdm.motionclueandro

import android.app.Application
import com.sniperoftdm.motionclueandro.bridge.MotionEngine
import com.sniperoftdm.motionclueandro.bridge.MotionEngineFactory
import com.sniperoftdm.motionclueandro.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holder for engine status flags displayed in the status line. Contract v2.
 */
data class EngineStatus(
    val flags: Int = 0,
    val isNative: Boolean = false,
) {
    val sensorsText: String
        get() = when {
            (flags and 0x8) != 0 -> "error"
            (flags and 0x1) != 0 -> "running"
            else -> "stopped"
        }

    val calibrationText: String
        get() = if ((flags and 0x2) != 0) "OK" else "learning"

    val speedText: String
        get() = if ((flags and 0x4) != 0) "GPS" else "none"

    val engineText: String
        get() = if (isNative) "native" else "stub"

    /** bit4: gravity settled */
    val gravityText: String
        get() = if ((flags and 0x10) != 0) "settled" else "settling"

    /** bit5: stationary, bit6: walking — derive activity label */
    val activityText: String
        get() = when {
            (flags and 0x20) != 0 -> "stationary"
            (flags and 0x40) != 0 -> "walking"
            else -> "moving"
        }
}

/**
 * Application class. Manual DI root.
 */
class MotionClueApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _engineStatus = MutableStateFlow(EngineStatus())
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    /** True when the native library loaded successfully. */
    var isNativeEngine: Boolean = false
        private set

    @Volatile
    var activeEngine: MotionEngine? = null

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
    }

    /**
     * Creates a new engine instance. Called by MotionService.
     * Each service lifecycle gets its own engine.
     */
    fun createEngine(): MotionEngine {
        val engine = MotionEngineFactory.create()
        isNativeEngine = engine.isNative
        activeEngine = engine
        _engineStatus.value = EngineStatus(flags = 0, isNative = engine.isNative)
        return engine
    }

    fun updateEngineFlags(flags: Int) {
        val current = _engineStatus.value
        if (current.flags != flags) {
            _engineStatus.value = current.copy(flags = flags)
        }
    }

    fun setRunning(running: Boolean) {
        _isRunning.value = running
        if (!running) {
            _engineStatus.value = EngineStatus(flags = 0, isNative = isNativeEngine)
            activeEngine = null
        }
    }
}
