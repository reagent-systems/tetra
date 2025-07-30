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
    private var rootNodeProvider: (() -> AccessibilityNodeInfo?)? = null
    private val refreshRunnable = object : Runnable {
        override fun run() {
            // Only update if overlay is visible and we have a root node provider
            if (overlay != null && overlayEnabled) {
                val rootNode = rootNodeProvider?.invoke()
                updateOverlay(rootNode)
                handler.postDelayed(this, 100) // Reduced frequency to avoid flicker
            }
        }
    }

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun setRootNodeProvider(provider: () -> AccessibilityNodeInfo?) {
        rootNodeProvider = provider
    }

    fun showOverlay(showBoxes: Boolean = true) {
        Log.d(TAG, "showOverlay called - showBoxes: $showBoxes")
        if (overlay == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                Log.d(TAG, "Creating new overlay")
                overlay = BoundingBoxOverlayView(context)
                overlay?.showBoundingBoxes = showBoxes
                overlayEnabled = showBoxes
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
                Log.d(TAG, "Overlay added to window manager")
                
                // Start refresh cycle
                handler.post(refreshRunnable)
            } else {
                Log.w(TAG, "Cannot draw overlays - permission not granted")
            }
        } else {
            Log.d(TAG, "Overlay already exists, updating visibility")
            overlay?.showBoundingBoxes = showBoxes
            overlayEnabled = showBoxes
            overlay?.invalidate()
        }
    }

    fun removeOverlay() {
        Log.d(TAG, "removeOverlay called")
        handler.removeCallbacks(refreshRunnable)
        overlay?.let {
            try {
                windowManager?.removeView(it)
                Log.d(TAG, "Overlay removed from window manager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
            overlay = null
        }
    }

    fun updateOverlay(root: AccessibilityNodeInfo?) {
        overlay?.updateBoundingBoxes(root)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        Log.d(TAG, "setOverlayEnabled: $enabled")
        overlayEnabled = enabled
        overlay?.showBoundingBoxes = enabled
        overlay?.invalidate()
    }

    fun isOverlayEnabled(): Boolean = overlayEnabled

    fun isOverlayActive(): Boolean = overlay != null

    fun cleanup() {
        Log.d(TAG, "cleanup called")
        handler.removeCallbacks(refreshRunnable)
        removeOverlay()
        windowManager = null
        rootNodeProvider = null
    }
} 