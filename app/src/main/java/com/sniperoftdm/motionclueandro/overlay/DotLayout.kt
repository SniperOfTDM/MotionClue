// com/sniperoftdm/motionclueandro/overlay/DotLayout.kt
package com.sniperoftdm.motionclueandro.overlay

/**
 * Computes dot positions for two vertical columns along screen edges.
 * Zero allocations in compute().
 *
 * Pattern wraps vertically using flow and shifts with acceleration.
 * Both columns shift in unison with vehicle lateral acceleration.
 */
class DotLayout {
    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var dotCount: Int = 10
    var edgeBandDp: Float = 28f
    var dotRadiusPx: Float = 0f
    var density: Float = 1f

    var shiftX: Float = 0f
    var shiftY: Float = 0f
    var flowPhase: Float = 0f

    private val xPositions = FloatArray(MAX_DOTS * 2)
    private val yPositions = FloatArray(MAX_DOTS * 2)
    var totalDots: Int = 0
        private set

    /**
     * Clamps value safely between min and max without throwing on inverted bounds or NaN.
     */
    private fun safeClamp(value: Float, boundA: Float, boundB: Float): Float {
        if (value.isNaN()) return boundA
        val low = minOf(boundA, boundB)
        val high = maxOf(boundA, boundB)
        return value.coerceIn(low, high)
    }

    /**
     * Mathematical modulo that always yields a non-negative result in [0, m).
     */
    private fun mod(value: Float, m: Float): Float {
        if (m <= 0f) return 0f
        val r = value % m
        return if (r < 0f) r + m else r
    }

    /**
     * Recompute dot positions in-place. Zero allocations.
     */
    fun compute(maxShiftPx: Float) {
        if (screenWidth <= 0 || screenHeight <= 0 || dotCount <= 0) {
            totalDots = 0
            return
        }

        val clampedDotCount = dotCount.coerceIn(1, MAX_DOTS)
        totalDots = clampedDotCount * 2

        val bandPx = edgeBandDp * density
        val leftCenterX = bandPx
        val rightCenterX = screenWidth.toFloat() - bandPx
        val spacing = screenHeight.toFloat() / clampedDotCount.toFloat()
        val totalHeight = screenHeight.toFloat()

        // Sanitize shift and flow inputs against NaN or Inf
        val safeShiftX = if (shiftX.isNaN() || shiftX.isInfinite()) 0f else shiftX.coerceIn(-1f, 1f)
        val safeShiftY = if (shiftY.isNaN() || shiftY.isInfinite()) 0f else shiftY.coerceIn(-1f, 1f)
        val safeFlow = if (flowPhase.isNaN() || flowPhase.isInfinite()) 0f else flowPhase

        val shiftPxX = safeShiftX * maxShiftPx
        val shiftPxY = safeShiftY * maxShiftPx

        // Wrap flow smoothly within one cycle of totalHeight
        val wrappedFlow = mod(safeFlow, totalHeight)

        // Left column X: clamped safely inside edge band
        val lx = safeClamp(leftCenterX + shiftPxX, dotRadiusPx, bandPx * 2f)
        // Right column X: shifts in the SAME direction as left column, clamped safely inside right band
        val rx = safeClamp(
            rightCenterX + shiftPxX,
            screenWidth.toFloat() - bandPx * 2f,
            screenWidth.toFloat() - dotRadiusPx,
        )

        for (i in 0 until clampedDotCount) {
            val baseY = spacing * 0.5f + spacing * i
            // Base position advanced by flow, wrapped smoothly across the full column
            val flowY = mod(baseY + wrappedFlow, totalHeight)
            // Inertial shift moves the entire column together
            val y = flowY + shiftPxY

            // Left column dot
            xPositions[i] = lx
            yPositions[i] = y

            // Right column dot
            xPositions[clampedDotCount + i] = rx
            yPositions[clampedDotCount + i] = y
        }
    }

    fun getX(index: Int): Float = xPositions[index]
    fun getY(index: Int): Float = yPositions[index]

    companion object {
        const val MAX_DOTS = 24
    }
}
