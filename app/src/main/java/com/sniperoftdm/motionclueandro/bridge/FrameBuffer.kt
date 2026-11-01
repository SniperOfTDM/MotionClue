package com.sniperoftdm.motionclueandro.bridge

import java.nio.ByteBuffer

/**
 * Reads the 128-byte shared buffer using the seqlock protocol.
 * Single implementation of the read, used by NativeMotionEngine.
 * Contract v2 — all 32 slots (byte offset = slot × 4).
 */
object FrameBuffer {
    // Slot indices (byte offset = slot * 4)
    const val SLOT_VERSION = 0          // int32, always 2
    const val SLOT_SEQUENCE = 1         // uint32, seqlock
    const val SLOT_FLAGS = 2            // int32, bit0-7
    const val SLOT_SHIFT_X = 3          // float
    const val SLOT_SHIFT_Y = 4          // float
    const val SLOT_FLOW_DIR_X = 5       // float
    const val SLOT_FLOW_DIR_Y = 6       // float
    const val SLOT_FLOW_RATE = 7        // float
    const val SLOT_SPEED_MPS = 8        // float
    const val SLOT_LONG_ACCEL = 9       // float
    const val SLOT_LAT_ACCEL = 10       // float
    const val SLOT_YAW_RATE = 11        // float
    const val SLOT_CONFIDENCE = 12      // float
    const val SLOT_SENSOR_MASK = 13     // int32
    // slots 14-15 = int64 timestampNanos (byte offset 56, 8-aligned)
    const val SLOT_TIMESTAMP = 14
    const val SLOT_AMBIENT_LUX = 16     // float
    const val SLOT_PROXIMITY_CM = 17    // float
    const val SLOT_PRESSURE_HPA = 18    // float
    const val SLOT_CLIMB_RATE = 19      // float
    const val SLOT_HEADING_RAD = 20     // float
    const val SLOT_TILT_DEG = 21        // float
    const val SLOT_VIEW_BLEND = 22      // float
    const val SLOT_STATIONARY_SEC = 23  // float
    const val SLOT_MEASURED_RATE_HZ = 24 // float
    const val SLOT_DROPPED_SAMPLES = 25 // int32
    // slots 26-31 reserved

    const val BUFFER_CAPACITY = 128
    private const val MAX_RETRIES = 3

    /**
     * Seqlock read: read sequence s1; if odd, retry; copy all fields;
     * read sequence s2; accept only if s1 == s2.
     * After 3 failed attempts, returns false (caller reuses previous frame).
     */
    fun read(buffer: ByteBuffer, out: MotionFrame): Boolean {
        for (attempt in 0 until MAX_RETRIES) {
            val s1 = buffer.getInt(SLOT_SEQUENCE * 4)
            if (s1 and 1 != 0) continue // odd = write in progress

            val flags = buffer.getInt(SLOT_FLAGS * 4)
            val shiftX = buffer.getFloat(SLOT_SHIFT_X * 4)
            val shiftY = buffer.getFloat(SLOT_SHIFT_Y * 4)
            val flowDirX = buffer.getFloat(SLOT_FLOW_DIR_X * 4)
            val flowDirY = buffer.getFloat(SLOT_FLOW_DIR_Y * 4)
            val flowRate = buffer.getFloat(SLOT_FLOW_RATE * 4)
            val speedMps = buffer.getFloat(SLOT_SPEED_MPS * 4)
            val longAccel = buffer.getFloat(SLOT_LONG_ACCEL * 4)
            val latAccel = buffer.getFloat(SLOT_LAT_ACCEL * 4)
            val yawRate = buffer.getFloat(SLOT_YAW_RATE * 4)
            val confidence = buffer.getFloat(SLOT_CONFIDENCE * 4)
            val sensorMask = buffer.getInt(SLOT_SENSOR_MASK * 4)
            // timestamp is int64 at byte offset 56 (slot 14)
            val timestampNanos = buffer.getLong(SLOT_TIMESTAMP * 4)
            val ambientLux = buffer.getFloat(SLOT_AMBIENT_LUX * 4)
            val proximityCm = buffer.getFloat(SLOT_PROXIMITY_CM * 4)
            val pressureHpa = buffer.getFloat(SLOT_PRESSURE_HPA * 4)
            val climbRate = buffer.getFloat(SLOT_CLIMB_RATE * 4)
            val headingRad = buffer.getFloat(SLOT_HEADING_RAD * 4)
            val tiltDeg = buffer.getFloat(SLOT_TILT_DEG * 4)
            val viewBlend = buffer.getFloat(SLOT_VIEW_BLEND * 4)
            val stationarySec = buffer.getFloat(SLOT_STATIONARY_SEC * 4)
            val measuredRateHz = buffer.getFloat(SLOT_MEASURED_RATE_HZ * 4)
            val droppedSamples = buffer.getInt(SLOT_DROPPED_SAMPLES * 4)

            val s2 = buffer.getInt(SLOT_SEQUENCE * 4)
            if (s1 != s2) continue // torn read

            out.flags = flags
            out.shiftX = shiftX
            out.shiftY = shiftY
            out.flowDirX = flowDirX
            out.flowDirY = flowDirY
            out.flowRate = flowRate
            out.speedMps = speedMps
            out.longAccel = longAccel
            out.latAccel = latAccel
            out.yawRate = yawRate
            out.confidence = confidence
            out.sensorMask = sensorMask
            out.timestampNanos = timestampNanos
            out.ambientLux = ambientLux
            out.proximityCm = proximityCm
            out.pressureHpa = pressureHpa
            out.climbRate = climbRate
            out.headingRad = headingRad
            out.tiltDeg = tiltDeg
            out.viewBlend = viewBlend
            out.stationarySec = stationarySec
            out.measuredRateHz = measuredRateHz
            out.droppedSamples = droppedSamples
            return true
        }
        return false
    }
}
