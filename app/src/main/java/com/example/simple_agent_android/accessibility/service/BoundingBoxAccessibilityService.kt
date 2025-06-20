package com.example.simple_agent_android.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.simple_agent_android.accessibility.floating.FloatingButtonManager
import com.example.simple_agent_android.accessibility.overlay.OverlayManager
import com.example.simple_agent_android.accessibility.service.InteractiveElementUtils

class BoundingBoxAccessibilityService : AccessibilityService() {
    private val TAG = "BoundingBoxService"
    private lateinit var overlayManager: OverlayManager
    private lateinit var floatingButtonManager: FloatingButtonManager
    private lateinit var gestureHandler: AccessibilityGestureHandler
    private lateinit var nodeHandler: AccessibilityNodeHandler

    companion object {
        private var instance: BoundingBoxAccessibilityService? = null

        fun startOverlay(showBoxes: Boolean = true) {
            instance?.overlayManager?.showOverlay(showBoxes)
        }

        fun stopOverlay() {
            instance?.overlayManager?.removeOverlay()
        }

        fun isOverlayActive(): Boolean {
            return instance?.overlayManager?.isOverlayActive() ?: false
        }

        fun setOverlayEnabled(enabled: Boolean) {
            instance?.overlayManager?.setOverlayEnabled(enabled)
        }

        fun isOverlayEnabled(): Boolean = instance?.overlayManager?.isOverlayEnabled() ?: false

        fun getInteractiveElementsJson(): String {
            return InteractiveElementUtils.getInteractiveElementsJson(instance)
        }

        fun simulatePressAt(x: Int, y: Int) {
            instance?.gestureHandler?.simulatePress(x, y)
        }

        fun setTextAt(x: Int, y: Int, text: String) {
            instance?.nodeHandler?.setTextAt(x, y, text)
        }

        fun showFloatingButton() {
            instance?.floatingButtonManager?.showFloatingButton()
        }

        fun hideFloatingButton() {
            instance?.floatingButtonManager?.hideFloatingButton()
        }

        fun goHome() {
            instance?.gestureHandler?.goHome()
        }

        fun goBack() {
            instance?.gestureHandler?.goBack()
        }

        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300) {
            instance?.gestureHandler?.swipe(startX, startY, endX, endY, duration)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        instance = this
        overlayManager = OverlayManager(this)
        overlayManager.setRootNodeProvider { rootInActiveWindow }
        floatingButtonManager = FloatingButtonManager(this)
        gestureHandler = AccessibilityGestureHandler(this)
        nodeHandler = AccessibilityNodeHandler(this)
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        super.onDestroy()
        cleanup()
        if (instance == this) instance = null
    }

    override fun onServiceConnected() {
        Log.d(TAG, "Service onServiceConnected")
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or 
                        AccessibilityEvent.TYPE_VIEW_CLICKED or 
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or 
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
        Log.d(TAG, "Service initialized successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.source != null) {
            overlayManager.updateOverlay(rootInActiveWindow)
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        if (instance == this) instance = null
        return super.onUnbind(intent)
    }

    private fun cleanup() {
        overlayManager.cleanup()
        floatingButtonManager.cleanup()
    }
} 