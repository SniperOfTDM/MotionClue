// com/sniperoftdm/motionclueandro/overlay/DotsOverlayView.kt
package com.sniperoftdm.motionclueandro.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import com.sniperoftdm.motionclueandro.MotionClueApp
import com.sniperoftdm.motionclueandro.bridge.MotionEngine
import com.sniperoftdm.motionclueandro.bridge.MotionFrame

/**
 * Custom View driven by Choreographer.FrameCallback.
 * Zero allocations in onDraw and doFrame.
 * Draws dots at positions computed by DotLayout.
 *
 * V2: smooths ambientLux into dot opacity and outline strength;
 * hides overlay when proximity-near flag (bit7) is set.
 */
class DotsOverlayView(
    context: Context,
    private val engine: MotionEngine,
) : View(context), Choreographer.FrameCallback {

    // Preallocated, reused every frame
    private val frame = MotionFrame()
    private val layout = DotLayout()
    private val app = context.applicationContext as? MotionClueApp

    // Paint objects — created once
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = Color.argb(80, 0, 0, 0)
    }

    // Settings — public setters, no allocations
    var dotSizeDp: Float = 6f
        set(value) { field = value; updateDerived() }
    var dotColor: Int = Color.WHITE
        set(value) {
            field = value
            dotPaint.color = value
            updateOutlineColor()
        }
    var dotOpacity: Float = 0.7f
        set(value) { field = value; updateAlpha() }
    var dotCountPerColumn: Int = 10
        set(value) { field = value; layout.dotCount = value }
    var edgeBandDp: Float = 28f
        set(value) { field = value; layout.edgeBandDp = value }
    var invertDirection: Boolean = true
    var maxShiftDp: Float = 28f
    var adaptiveBrightness: Boolean = true

    private var dotRadiusPx: Float = 0f
    private var maxShiftPx: Float = 0f
    private var lastFrameTimeNanos: Long = 0L
    private var flowPhase: Float = 0f
    private var statusFrameCount: Int = 0
    private var running = false

    // Smoothed ambient lux for adaptive brightness (EMA)
    private var smoothedLux: Float = -1f
    private var baseOutlineStrokeWidth: Float = 0f
    private var proximityHidden: Boolean = false

    init {
        isFocusable = false
        isClickable = false
        updateDerived()
        updateAlpha()
    }

    private fun updateDerived() {
        val d = resources.displayMetrics.density
        dotRadiusPx = dotSizeDp * d / 2f
        maxShiftPx = maxShiftDp * d
        layout.density = d
        layout.dotRadiusPx = dotRadiusPx
        baseOutlineStrokeWidth = 1.5f * d
        outlinePaint.strokeWidth = baseOutlineStrokeWidth
    }

    private fun updateAlpha() {
        val a = (dotOpacity * 255).toInt().coerceIn(0, 255)
        dotPaint.alpha = a
        outlinePaint.alpha = (dotOpacity * 80).toInt().coerceIn(0, 255)
    }

    private fun updateOutlineColor() {
        val r = Color.red(dotColor)
        val g = Color.green(dotColor)
        val b = Color.blue(dotColor)
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        outlinePaint.color = if (lum > 128) Color.argb(80, 0, 0, 0)
        else Color.argb(80, 255, 255, 255)
        updateAlpha()
    }

    /**
     * Apply ambient-lux-based adaptive brightness to dot opacity and outline strength.
     * Maps smoothed lux: 0 lux = full opacity, 10000+ lux = 30% boost (capped at 1.0).
     * Low lux (dark) dims the overlay to reduce glare.
     */
    private fun applyAdaptiveBrightness() {
        if (!adaptiveBrightness || smoothedLux < 0f) {
            // Not enabled or no sensor: use base dotOpacity
            updateAlpha()
            return
        }
        // Map lux to a multiplier: dark (< 50 lux) = 0.5, normal (~300) = 1.0, bright (> 5000) = 1.3
        val factor = (smoothedLux / 300f).coerceIn(0.5f, 1.3f)
        val adaptedOpacity = (dotOpacity * factor).coerceIn(0f, 1f)
        val a = (adaptedOpacity * 255).toInt().coerceIn(0, 255)
        dotPaint.alpha = a
        outlinePaint.alpha = (adaptedOpacity * 80).toInt().coerceIn(0, 255)
        // Also scale outline stroke width slightly for bright environments
        outlinePaint.strokeWidth = baseOutlineStrokeWidth * factor.coerceIn(0.8f, 1.2f)
    }

    fun startRendering() {
        if (running) return
        running = true
        lastFrameTimeNanos = SystemClock.elapsedRealtimeNanos()
        flowPhase = 0f
        statusFrameCount = 0
        smoothedLux = -1f
        proximityHidden = false
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopRendering() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return

        // dt from Choreographer, clamped to 0.1 s
        val dtNanos = frameTimeNanos - lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos
        val dt = (dtNanos / 1_000_000_000f).coerceIn(0f, 0.1f)

        if (engine.readFrame(frame)) {
            // Check proximity-near flag (bit7): hide overlay
            proximityHidden = frame.proximityNear

            // Smooth ambient lux with EMA (alpha ~0.1 per frame at 60fps)
            if (frame.ambientLux >= 0f) {
                smoothedLux = if (smoothedLux < 0f) {
                    frame.ambientLux
                } else {
                    smoothedLux + 0.1f * (frame.ambientLux - smoothedLux)
                }
            }

            // Apply adaptive brightness based on smoothed lux
            applyAdaptiveBrightness()

            // Forward flags to application periodically (every 30 frames, ~0.5s)
            statusFrameCount++
            if (statusFrameCount >= 30) {
                statusFrameCount = 0
                app?.updateEngineFlags(frame.flags)
            }

            // flowRate is in pattern units per second (1.0 = one dot spacing)
            // Convert to pixels: spacing = screenHeight / dotCount
            val spacing = if (layout.dotCount > 0) {
                height.toFloat() / layout.dotCount
            } else height.toFloat()
            flowPhase += frame.flowRate * dt * spacing * frame.flowDirY

            val invertSign = if (invertDirection) -1f else 1f
            layout.shiftX = frame.shiftX * invertSign
            layout.shiftY = frame.shiftY * invertSign
            layout.flowPhase = flowPhase * invertSign
        }

        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layout.screenWidth = w
        layout.screenHeight = h
        maxShiftPx = maxShiftDp * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        // Hide when proximity near
        if (proximityHidden) return

        // Zero allocations
        layout.compute(maxShiftPx)
        val h = height.toFloat()

        for (i in 0 until layout.totalDots) {
            val cx = layout.getX(i)
            val cy = layout.getY(i)
            if (cy > -dotRadiusPx && cy < h + dotRadiusPx) {
                canvas.drawCircle(cx, cy, dotRadiusPx, outlinePaint)
                canvas.drawCircle(
                    cx, cy,
                    dotRadiusPx - outlinePaint.strokeWidth * 0.5f,
                    dotPaint,
                )
            }
        }
    }
}
