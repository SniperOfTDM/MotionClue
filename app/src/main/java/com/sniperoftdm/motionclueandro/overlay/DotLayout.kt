// com/sniperoftdm/motionclueandro/overlay/DotLayout.kt
package com.sniperoftdm.motionclueandro.overlay

/**
 * Computes dot positions for two vertical columns (left and right edges, mirrored).
 * No allocations after init — positions are updated in-place each frame.
 *
 * Pattern wraps vertically using flow's Y component.
 * Shift moves dots sideways (X) and vertically (Y), clamped inside the band.
 */
class DotLayout {
    // Configuration — set before first frame
    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var dotCount: Int = 10          // dots per column
    var edgeBandDp: Float = 28f     // dp from screen edge to dot center
    var dotRadiusPx: Float = 0f     // for clamping
    var density: Float = 1f         // display density

    // Per-frame input
    var shiftX: Float = 0f          // -1..1 from engine, already inverted if needed
    var shiftY: Float = 0f          // -1..1 from engine
    var flowPhase: Float = 0f       // accumulated flow phase in pixels

    // Output arrays — index 0..dotCount-1 = left column, dotCount..2*dotCount-1 = right
    private val xPositions = FloatArray(MAX_DOTS * 2)
    private val yPositions = FloatArray(MAX_DOTS * 2)
    var totalDots: Int = 0
        private set

    /**
     * Recompute all dot positions for the current frame. Zero allocations.
     */
    fun compute(maxShiftPx: Float) {
        val bandPx = edgeBandDp * density
        val leftCenterX = bandPx
        val rightCenterX = screenWidth - bandPx
        val spacing = if (dotCount > 0) screenHeight.toFloat() / dotCount else screenHeight.toFloat()

        totalDots = dotCount * 2

        val shiftPxX = shiftX * maxShiftPx
        val shiftPxY = shiftY * maxShiftPx

        // Wrap flowPhase into [0, spacing)
        val wrappedPhase = if (spacing > 0f) {
            val p = (flowPhase % spacing)
            if (p < 0f) p + spacing else p
        } else 0f

        val totalHeight = spacing * dotCount

        for (i in 0 until dotCount) {
            // Base Y: evenly spaced, offset by half spacing so dots start mid-gap
            val baseY = spacing * 0.5f + spacing * i

            // Apply flow phase (vertical wrap) and shift
            var y = baseY + wrappedPhase + shiftPxY
            // Wrap vertically
            y = y % totalHeight
            if (y < -spacing * 0.5f) y += totalHeight
            if (y > screenHeight + spacing * 0.5f) y -= totalHeight

            // Left column: clamp X inside the band
            val lx = (leftCenterX + shiftPxX).coerceIn(dotRadiusPx, bandPx * 2f)
            xPositions[i] = lx
            yPositions[i] = y

            // Right column: mirrored shift
            val rx = (rightCenterX - shiftPxX).coerceIn(
                screenWidth - bandPx * 2f,
                screenWidth - dotRadiusPx,
            )
            xPositions[dotCount + i] = rx
            yPositions[dotCount + i] = y
        }
    }

    fun getX(index: Int): Float = xPositions[index]
    fun getY(index: Int): Float = yPositions[index]

    companion object {
        const val MAX_DOTS = 24 // supports density up to 20 dots per column
    }
}
