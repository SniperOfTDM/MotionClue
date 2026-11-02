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
 * Draws dots at positions computed by DotLayout with seamless edge wrapping.
 *
 * Supports ambient-lux adaptive brightness and proximity-hide.
 */
class DotsOverlayView(
    context: Context,
    private val engine: MotionEngine,
) : View(context), Choreographer.FrameCallback {

    private val frame = MotionFrame()
    private val layout = DotLayout()
    private val app = context.applicationContext as? MotionClueApp

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        color = Color.argb(80, 0, 0, 0)
    }

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
    private var flowPhasePx: Float = 0f
    private var statusFrameCount: Int = 0
    private var running = false

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

    private fun applyAdaptiveBrightness() {
        if (!adaptiveBrightness || smoothedLux < 0f) {
            updateAlpha()
            return
        }
        val factor = (smoothedLux / 300f).coerceIn(0.5f, 1.3f)
        val adaptedOpacity = (dotOpacity * factor).coerceIn(0f, 1f)
        val a = (adaptedOpacity * 255).toInt().coerceIn(0, 255)
        dotPaint.alpha = a
        outlinePaint.alpha = (adaptedOpacity * 80).toInt().coerceIn(0, 255)
        outlinePaint.strokeWidth = baseOutlineStrokeWidth * factor.coerceIn(0.8f, 1.2f)
    }

    fun startRendering() {
        if (running) return
        running = true
        lastFrameTimeNanos = SystemClock.elapsedRealtimeNanos()
        flowPhasePx = 0f
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

        val dtNanos = frameTimeNanos - lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos
        val dt = (dtNanos / 1_000_000_000f).coerceIn(0f, 0.1f)

        if (engine.readFrame(frame)) {
            proximityHidden = frame.proximityNear

            if (frame.ambientLux >= 0f) {
                smoothedLux = if (smoothedLux < 0f) {
                    frame.ambientLux
                } else {
                    smoothedLux + 0.1f * (frame.ambientLux - smoothedLux)
                }
            }

            applyAdaptiveBrightness()

            statusFrameCount++
            if (statusFrameCount >= 30) {
                statusFrameCount = 0
                app?.updateEngineFlags(frame.flags)
            }

            val count = if (layout.dotCount > 0) layout.dotCount else 1
            val spacing = if (height > 0) height.toFloat() / count.toFloat() else 100f
            val totalHeight = if (height > 0) height.toFloat() else 1000f

            val rawFlowRate = if (frame.flowRate.isNaN() || frame.flowRate.isInfinite()) 0f else frame.flowRate
            val rawFlowDirY = if (frame.flowDirY.isNaN() || frame.flowDirY.isInfinite()) 1f else frame.flowDirY

            val deltaFlowPx = rawFlowRate * dt * spacing * rawFlowDirY
            flowPhasePx = (flowPhasePx + deltaFlowPx) % totalHeight
            if (flowPhasePx < 0f) flowPhasePx += totalHeight

            val invertSign = if (invertDirection) -1f else 1f
            layout.shiftX = (if (frame.shiftX.isNaN()) 0f else frame.shiftX) * invertSign
            layout.shiftY = (if (frame.shiftY.isNaN()) 0f else frame.shiftY) * invertSign
            layout.flowPhase = flowPhasePx * invertSign
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
        if (proximityHidden || width <= 0 || height <= 0) return

        layout.compute(maxShiftPx)
        val h = height.toFloat()

        for (i in 0 until layout.totalDots) {
            val cx = layout.getX(i)
            val cy = layout.getY(i)

            drawDot(canvas, cx, cy, h)
        }
    }

    private fun drawDot(canvas: Canvas, cx: Float, cy: Float, screenH: Float) {
        if (cy > -dotRadiusPx && cy < screenH + dotRadiusPx) {
            canvas.drawCircle(cx, cy, dotRadiusPx, outlinePaint)
            canvas.drawCircle(
                cx, cy,
                dotRadiusPx - outlinePaint.strokeWidth * 0.5f,
                dotPaint,
            )
        }

        if (cy - dotRadiusPx < 0f) {
            val wrapY = cy + screenH
            if (wrapY < screenH + dotRadiusPx) {
                canvas.drawCircle(cx, wrapY, dotRadiusPx, outlinePaint)
                canvas.drawCircle(
                    cx, wrapY,
                    dotRadiusPx - outlinePaint.strokeWidth * 0.5f,
                    dotPaint,
                )
            }
        } else if (cy + dotRadiusPx > screenH) {
            val wrapY = cy - screenH
            if (wrapY > -dotRadiusPx) {
                canvas.drawCircle(cx, wrapY, dotRadiusPx, outlinePaint)
                canvas.drawCircle(
                    cx, wrapY,
                    dotRadiusPx - outlinePaint.strokeWidth * 0.5f,
                    dotPaint,
                )
            }
        }
    }
}
