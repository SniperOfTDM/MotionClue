package com.sniperoftdm.motionclueandro.bridge

/**
 * Builds the 12-float config array in contract v2 order from user settings.
 * Only sensitivity, hybridBlend, and displayRotation are caller-supplied;
 * all other slots use contract defaults.
 */
object EngineConfig {
    // Config array indices (contract order)
    private const val IDX_SENSITIVITY = 0       // range 0.2-3.0, default 1.0
    private const val IDX_HYBRID_BLEND = 1      // range 0-1, default 0.5
    private const val IDX_SPRING_STIFFNESS = 2  // default 18.0
    private const val IDX_SPRING_DAMPING = 3    // default 6.0
    private const val IDX_REFERENCE_ACCEL = 4   // default 3.0 m/s²
    private const val IDX_FLOW_GAIN = 5         // default 0.5
    private const val IDX_DEAD_ZONE = 6         // default 0.15 m/s²
    private const val IDX_DISPLAY_ROTATION = 7  // 0-3, Surface.ROTATION_*
    private const val IDX_FLOW_FLOOR_KMH = 8    // default 5.0
    private const val IDX_FLOW_CAP_UNITS = 9    // default 4.0
    private const val IDX_USE_MAGNETOMETER = 10 // 0 or 1, default 1
    private const val IDX_SAMPLE_RATE_HZ = 11   // 50-200, default 100

    const val CONFIG_LENGTH = 12

    fun build(
        sensitivity: Float = 1.0f,
        hybridBlend: Float = 0.5f,
        displayRotation: Int = 0,
    ): FloatArray {
        return FloatArray(CONFIG_LENGTH).also { a ->
            a[IDX_SENSITIVITY] = sensitivity.coerceIn(0.2f, 3.0f)
            a[IDX_HYBRID_BLEND] = hybridBlend.coerceIn(0f, 1f)
            a[IDX_SPRING_STIFFNESS] = 18.0f
            a[IDX_SPRING_DAMPING] = 6.0f
            a[IDX_REFERENCE_ACCEL] = 3.0f
            a[IDX_FLOW_GAIN] = 0.5f
            a[IDX_DEAD_ZONE] = 0.15f
            a[IDX_DISPLAY_ROTATION] = displayRotation.coerceIn(0, 3).toFloat()
            a[IDX_FLOW_FLOOR_KMH] = 5.0f
            a[IDX_FLOW_CAP_UNITS] = 4.0f
            a[IDX_USE_MAGNETOMETER] = 1f
            a[IDX_SAMPLE_RATE_HZ] = 100f
        }
    }
}
