package com.sniperoftdm.motionclueandro.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import com.sniperoftdm.motionclueandro.bridge.MotionEngine

/**
 * Manages the lifecycle of the overlay window and the DotsOverlayView.
 * TYPE_APPLICATION_OVERLAY, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE,
 * covers full screen including cutout area, TRANSLUCENT.
 */
class OverlayController(
    private val context: Context,
    private val engine: MotionEngine,
) {
    private var overlayView: DotsOverlayView? = null
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    fun show(
        dotSizeDp: Float = 9f,
        dotColor: Int = Color.WHITE,
        dotOpacity: Float = 0.7f,
        dotCountPerColumn: Int = 4,
        edgeBandDp: Float = 26f,
        invertDirection: Boolean = true,
        adaptiveBrightness: Boolean = true,
    ) {
        if (overlayView != null) return

        val view = DotsOverlayView(context, engine).apply {
            this.dotSizeDp = dotSizeDp
            this.dotColor = dotColor
            this.dotOpacity = dotOpacity
            this.dotCountPerColumn = dotCountPerColumn
            this.edgeBandDp = edgeBandDp
            this.maxShiftDp = edgeBandDp
            this.invertDirection = invertDirection
            this.adaptiveBrightness = adaptiveBrightness
        }

        windowManager.addView(view, createLayoutParams())
        view.startRendering()
        overlayView = view
    }

    fun updateSettings(
        dotSizeDp: Float,
        dotColor: Int,
        dotOpacity: Float,
        dotCountPerColumn: Int,
        edgeBandDp: Float,
        invertDirection: Boolean,
        adaptiveBrightness: Boolean,
    ) {
        overlayView?.apply {
            this.dotSizeDp = dotSizeDp
            this.dotColor = dotColor
            this.dotOpacity = dotOpacity
            this.dotCountPerColumn = dotCountPerColumn
            this.edgeBandDp = edgeBandDp
            this.maxShiftDp = edgeBandDp
            this.invertDirection = invertDirection
            this.adaptiveBrightness = adaptiveBrightness
        }
    }

    fun hide() {
        val view = overlayView ?: return
        view.stopRendering()
        windowManager.removeView(view)
        overlayView = null
    }

    fun pauseRendering() {
        overlayView?.stopRendering()
    }

    fun resumeRendering() {
        overlayView?.startRendering()
    }
}
