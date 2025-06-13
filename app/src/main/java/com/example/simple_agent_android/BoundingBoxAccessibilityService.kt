package com.example.simple_agent_android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import com.example.simple_agent_android.BoundingBoxOverlayView
import com.example.simple_agent_android.InteractiveElementUtils
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.app.AlertDialog
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import com.example.simple_agent_android.ui.floating.FloatingControlsView

class BoundingBoxAccessibilityService : AccessibilityService() {
    private var overlay: BoundingBoxOverlayView? = null
    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val root = rootInActiveWindow
            overlay?.updateBoundingBoxes(root)
            handler.postDelayed(this, 50)
        }
    }
    private var floatingControls: FloatingControlsView? = null
    private var floatingControlsParams: WindowManager.LayoutParams? = null
    private var isPaused = false
    private var lastTouchX = 0
    private var lastTouchY = 0
    private var dragInitialX = 0
    private var dragInitialY = 0
    private var dragInitialTouchX = 0f
    private var dragInitialTouchY = 0f
    private var wasDragged = false

    companion object {
        private var instance: BoundingBoxAccessibilityService? = null
        private var verticalOffset: Int = 0
        private var overlayEnabled: Boolean = true
        fun startOverlay(showBoxes: Boolean = true) {
            overlayEnabled = showBoxes
            instance?.showOverlay(showBoxes)
            instance?.handler?.post(instance?.refreshRunnable!!)
        }
        fun stopOverlay() {
            instance?.removeOverlay()
            instance?.handler?.removeCallbacks(instance?.refreshRunnable!!)
        }
        fun isOverlayActive(): Boolean {
            return instance?.overlay != null
        }
        fun setOverlayEnabled(enabled: Boolean) {
            overlayEnabled = enabled
            instance?.overlay?.showBoundingBoxes = enabled
            instance?.overlay?.invalidate()
        }
        fun isOverlayEnabled(): Boolean = overlayEnabled
        fun setVerticalOffset(context: android.content.Context, offset: Int) {
            verticalOffset = offset
            val prefs = context.getSharedPreferences("overlay_prefs", MODE_PRIVATE)
            prefs.edit().putInt("vertical_offset", offset).apply()
        }
        fun getVerticalOffset(context: android.content.Context): Int {
            if (verticalOffset == 0) {
                val prefs = context.getSharedPreferences("overlay_prefs", MODE_PRIVATE)
                verticalOffset = prefs.getInt("vertical_offset", 0)
            }
            return verticalOffset
        }
        fun getInteractiveElementsJson(): String {
            return InteractiveElementUtils.getInteractiveElementsJson(instance)
        }
        fun simulatePressAt(x: Int, y: Int) {
            instance?.simulatePress(x, y)
        }
        fun setTextAt(x: Int, y: Int, text: String) {
            instance?.setTextAtInternal(x, y, text)
        }
        fun showStopButton() {
            instance?.showStopButtonInternal()
        }
        fun hideStopButton() {
            instance?.removeStopButton()
        }
        fun isServiceEnabled(context: android.content.Context): Boolean {
            val expectedComponent = context.packageName + "/.BoundingBoxAccessibilityService"
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
        }
        fun goHome() {
            instance?.goHome()
        }
        fun goBack() {
            instance?.goBack()
        }
        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300) {
            instance?.swipe(startX, startY, endX, endY, duration)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        handler.removeCallbacks(refreshRunnable)
        windowManager = null
        if (instance == this) instance = null
        verticalOffset = 0
        overlayEnabled = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // Do not auto-start overlay here
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.source != null && overlay != null) {
            val root = rootInActiveWindow
            overlay?.updateBoundingBoxes(root)
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        removeOverlay()
        handler.removeCallbacks(refreshRunnable)
        windowManager = null
        if (instance == this) instance = null
        verticalOffset = 0
        overlayEnabled = true
        return super.onUnbind(intent)
    }

    private fun showOverlay(showBoxes: Boolean = true) {
        if (overlay == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                overlay = BoundingBoxOverlayView(this)
                overlay?.showBoundingBoxes = showBoxes
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                windowManager?.addView(overlay, params)
                showStopButton()
            }
        } else {
            overlay?.showBoundingBoxes = showBoxes
            overlay?.invalidate()
        }
    }

    private fun showStopButtonInternal() {
        if (floatingControls == null) {
            floatingControls = FloatingControlsView(this).apply {
                onPauseClick = {
                    if (!wasDragged) {
                        val currentlyPaused = com.example.simple_agent_android.agentcore.AgentOrchestrator.isPaused()
                        if (currentlyPaused) {
                            com.example.simple_agent_android.agentcore.AgentOrchestrator.resumeAgent()
                        } else {
                            com.example.simple_agent_android.agentcore.AgentOrchestrator.pauseAgent()
                        }
                        setPaused(com.example.simple_agent_android.agentcore.AgentOrchestrator.isPaused())
                    }
                }
                onStopClick = {
                    if (!wasDragged) {
                        com.example.simple_agent_android.agentcore.AgentOrchestrator.stopAgent()
                    }
                }
                onDragEvent = { action, rawX, rawY ->
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            floatingControlsParams?.let { params ->
                                dragInitialX = params.x
                                dragInitialY = params.y
                                dragInitialTouchX = rawX
                                dragInitialTouchY = rawY
                                wasDragged = false
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            floatingControlsParams?.let { params ->
                                val dx = (rawX - dragInitialTouchX).toInt()
                                val dy = (rawY - dragInitialTouchY).toInt()
                                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                    wasDragged = true
                                }
                                params.x = dragInitialX + dx
                                params.y = dragInitialY + dy
                                windowManager?.updateViewLayout(floatingControls, params)
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            // nothing needed here
                        }
                    }
                }
            }
            floatingControlsParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 200
            }
            floatingControls?.setPaused(com.example.simple_agent_android.agentcore.AgentOrchestrator.isPaused())
            windowManager?.addView(floatingControls, floatingControlsParams)
        }
    }

    private fun removeStopButton() {
        floatingControls?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingControls = null
        }
    }

    private fun removeOverlay() {
        overlay?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlay = null
        }
        removeStopButton()
    }

    fun simulatePress(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun setTextAtInternal(x: Int, y: Int, text: String) {
        val root = rootInActiveWindow ?: return
        val node = findEditableNodeAt(root, x, y)
        if (node != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } else {
                // Fallback: try focus and paste (not as reliable)
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            }
        }
    }

    private fun findEditableNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        if (rect.contains(x, y) && node.isEditable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findEditableNodeAt(it, x, y) }
            if (result != null) return result
        }
        return null
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }
} 