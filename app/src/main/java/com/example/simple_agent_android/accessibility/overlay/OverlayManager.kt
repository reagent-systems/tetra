package com.example.simple_agent_android.accessibility.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class OverlayManager(private val context: Context) {
    private val TAG = "OverlayManager"
    private var overlay: BoundingBoxOverlayView? = null
    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var overlayEnabled: Boolean = true
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateOverlay(null)
            handler.postDelayed(this, 50)
        }
    }

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun showOverlay(showBoxes: Boolean = true) {
        if (overlay == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                overlay = BoundingBoxOverlayView(context)
                overlay?.showBoundingBoxes = showBoxes
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                windowManager?.addView(overlay, params)
                handler.post(refreshRunnable)
            }
        } else {
            overlay?.showBoundingBoxes = showBoxes
            overlay?.invalidate()
        }
    }

    fun removeOverlay() {
        overlay?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
            overlay = null
        }
        handler.removeCallbacks(refreshRunnable)
    }

    fun updateOverlay(root: AccessibilityNodeInfo?) {
        overlay?.updateBoundingBoxes(root)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        overlayEnabled = enabled
        overlay?.showBoundingBoxes = enabled
        overlay?.invalidate()
    }

    fun isOverlayEnabled(): Boolean = overlayEnabled

    fun isOverlayActive(): Boolean = overlay != null

    fun cleanup() {
        removeOverlay()
        handler.removeCallbacks(refreshRunnable)
        windowManager = null
    }
} 