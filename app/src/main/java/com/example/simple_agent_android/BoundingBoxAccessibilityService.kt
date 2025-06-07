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

class BoundingBoxAccessibilityService : AccessibilityService() {
    private var overlay: BoundingBoxOverlayView? = null
    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val root = rootInActiveWindow
            overlay?.updateBoundingBoxes(root)
            handler.postDelayed(this, 200)
        }
    }
    private var floatingButton: ImageView? = null
    private var floatingButtonParams: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDialogOpen = false

    companion object {
        private var instance: BoundingBoxAccessibilityService? = null
        private var verticalOffset: Int = 0
        fun startOverlay() {
            instance?.showOverlay()
            instance?.handler?.post(instance?.refreshRunnable!!)
        }
        fun stopOverlay() {
            instance?.removeOverlay()
            instance?.handler?.removeCallbacks(instance?.refreshRunnable!!)
        }
        fun isOverlayActive(): Boolean {
            return instance?.overlay != null
        }
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
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
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
        return super.onUnbind(intent)
    }

    private fun showOverlay() {
        if (overlay == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                overlay = BoundingBoxOverlayView(this)
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
                showFloatingButton()
            }
        }
    }

    private fun showFloatingButton() {
        if (floatingButton == null) {
            floatingButton = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_info_details)
                setBackgroundColor(Color.argb(180, 255, 255, 255))
                setPadding(24, 24, 24, 24)
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = floatingButtonParams?.x ?: 0
                            initialY = floatingButtonParams?.y ?: 0
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                floatingButtonParams?.x = initialX + dx
                                floatingButtonParams?.y = initialY + dy
                                windowManager?.updateViewLayout(floatingButton, floatingButtonParams)
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                return@setOnTouchListener true
                            }
                            false
                        }
                        MotionEvent.ACTION_UP -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                                v.performClick()
                            }
                            false
                        }
                        else -> false
                    }
                }
                setOnClickListener {
                    showJsonDialog()
                }
            }
            floatingButtonParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 50
                y = 200
            }
            windowManager?.addView(floatingButton, floatingButtonParams)
        }
    }

    private fun removeFloatingButton() {
        floatingButton?.let {
            windowManager?.removeView(it)
            floatingButton = null
        }
    }

    private fun showJsonDialog() {
        if (isDialogOpen) return
        isDialogOpen = true
        val json = getInteractiveElementsJson()
        val builder = AlertDialog.Builder(this).apply {
            setTitle("Interactive Elements JSON")
            setMessage(json)
            setPositiveButton("Close") { dialog, _ ->
                isDialogOpen = false
                dialog.dismiss()
            }
            setOnDismissListener {
                isDialogOpen = false
            }
        }
        val dialog = builder.create()
        dialog.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )
        dialog.show()
    }

    private fun removeOverlay() {
        overlay?.let {
            windowManager?.removeView(it)
            overlay = null
        }
        removeFloatingButton()
    }
} 