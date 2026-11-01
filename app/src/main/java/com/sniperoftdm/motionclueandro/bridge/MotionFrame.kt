package com.sniperoftdm.motionclueandro.bridge

/**
 * Mutable holder for one frame of motion data read from the shared buffer.
 * Reused every frame to avoid per-frame allocation.
 * Contract v2 — all slots up to 25 plus reserved 26-31.
 */
class MotionFrame {
    var flags: Int = 0
    var shiftX: Float = 0f
    var shiftY: Float = 0f
    var flowDirX: Float = 0f
    var flowDirY: Float = 0f
    var flowRate: Float = 0f
    var speedMps: Float = -1f
    var longAccel: Float = 0f
    var latAccel: Float = 0f
    var yawRate: Float = 0f
    var confidence: Float = 0f
    var sensorMask: Int = 0
    var timestampNanos: Long = 0L
    var ambientLux: Float = -1f
    var proximityCm: Float = -1f
    var pressureHpa: Float = -1f
    var climbRate: Float = 0f
    var headingRad: Float = -1f
    var tiltDeg: Float = 0f
    var viewBlend: Float = 0f
    var stationarySec: Float = 0f
    var measuredRateHz: Float = 0f
    var droppedSamples: Int = 0

    /** bit0: sensors running */
    val sensorsRunning: Boolean get() = (flags and 0x1) != 0
    /** bit1: flow direction learned */
    val calibrated: Boolean get() = (flags and 0x2) != 0
    /** bit2: speed valid */
    val speedValid: Boolean get() = (flags and 0x4) != 0
    /** bit3: sensor error */
    val sensorError: Boolean get() = (flags and 0x8) != 0
    /** bit4: gravity settled */
    val gravitySettled: Boolean get() = (flags and 0x10) != 0
    /** bit5: stationary */
    val stationary: Boolean get() = (flags and 0x20) != 0
    /** bit6: walking */
    val walking: Boolean get() = (flags and 0x40) != 0
    /** bit7: proximity near */
    val proximityNear: Boolean get() = (flags and 0x80) != 0
}
