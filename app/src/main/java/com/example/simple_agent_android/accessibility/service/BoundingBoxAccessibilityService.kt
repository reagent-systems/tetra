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
import com.example.simple_agent_android.accessibility.overlay.DebugCursorView
import com.example.simple_agent_android.accessibility.service.InteractiveElementUtils
import com.example.simple_agent_android.sentry.AgentErrorTracker
import com.example.simple_agent_android.sentry.trackFeatureUsage
import com.example.simple_agent_android.utils.SharedPrefsUtils

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
            try {
                // Get the vertical offset and reverse it for actual touch coordinates
                val context = instance?.applicationContext
                val verticalOffset = if (context != null) {
                    SharedPrefsUtils.getVerticalOffset(context)
                } else {
                    0 // Fallback if context is null
                }
                val actualX = x
                val actualY = y - verticalOffset // Reverse the offset applied in InteractiveElementUtils
                
                Log.d("BoundingBoxAccessibilityService", "simulatePressAt: LLM coordinates ($x, $y) -> actual touch coordinates ($actualX, $actualY), offset: $verticalOffset")
                
                instance?.gestureHandler?.simulatePress(actualX, actualY)
                
                // Show debug cursor if enabled - use original coordinates for visual alignment
                if (DebugCursorView.isActive()) {
                    DebugCursorView.showPress(x, y)
                }
                
                trackFeatureUsage("accessibility_gesture", "press", true, mapOf("x" to actualX, "y" to actualY))
            } catch (e: Exception) {
                AgentErrorTracker.trackAccessibilityError(
                    error = e,
                    action = "simulate_press",
                    elementInfo = mapOf("x" to x, "y" to y)
                )
                throw e
            }
        }

        fun setTextAt(x: Int, y: Int, text: String) {
            try {
                instance?.nodeHandler?.setTextAt(x, y, text)
                trackFeatureUsage("accessibility_text", "set_text", true, mapOf("x" to x, "y" to y, "text_length" to text.length))
            } catch (e: Exception) {
                AgentErrorTracker.trackAccessibilityError(
                    error = e,
                    action = "set_text",
                    elementInfo = mapOf("x" to x, "y" to y, "text_length" to text.length)
                )
                throw e
            }
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
            // Get the vertical offset and reverse it for actual touch coordinates
            val context = instance?.applicationContext
            val verticalOffset = if (context != null) {
                SharedPrefsUtils.getVerticalOffset(context)
            } else {
                0 // Fallback if context is null
            }
            val actualStartX = startX
            val actualStartY = startY - verticalOffset // Reverse the offset
            val actualEndX = endX
            val actualEndY = endY - verticalOffset // Reverse the offset
            
            instance?.gestureHandler?.swipe(actualStartX, actualStartY, actualEndX, actualEndY, duration)
            
            // Show debug cursor for swipe if enabled - use original coordinates for visual alignment
            if (DebugCursorView.isActive()) {
                DebugCursorView.showSwipe(startX, startY, endX, endY)
            }
        }
        
        fun testCompletionScreen() {
            instance?.floatingButtonManager?.testShowCompletionScreen()
        }
        
        // Debug cursor functions
        fun showDebugCursor() {
            instance?.let { service ->
                DebugCursorView.show(service.applicationContext)
            }
        }
        
        fun hideDebugCursor() {
            DebugCursorView.hide()
        }
        
        fun isDebugCursorActive(): Boolean {
            return DebugCursorView.isActive()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        try {
            instance = this
            overlayManager = OverlayManager(this)
            overlayManager.setRootNodeProvider { rootInActiveWindow }
            floatingButtonManager = FloatingButtonManager(this)
            gestureHandler = AccessibilityGestureHandler(this)
            nodeHandler = AccessibilityNodeHandler(this)
            
            // Initialize debug cursor
            DebugCursorView.initialize(this)
            
            trackFeatureUsage("accessibility_service", "created", true)
        } catch (e: Exception) {
            AgentErrorTracker.trackAccessibilityError(
                error = e,
                action = "service_create"
            )
            throw e
        }
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
        
        try {
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
            
            trackFeatureUsage("accessibility_service", "connected", true)
        } catch (e: Exception) {
            AgentErrorTracker.trackAccessibilityError(
                error = e,
                action = "service_connect"
            )
            throw e
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            // Log accessibility events for debugging text input issues
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d("AccessibilityEvent", "VIEW_CLICKED: ${accessibilityEvent.className} - ${accessibilityEvent.text}")
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    Log.d("AccessibilityEvent", "VIEW_FOCUSED: ${accessibilityEvent.className} - ${accessibilityEvent.text}")
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    Log.d("AccessibilityEvent", "VIEW_TEXT_CHANGED: ${accessibilityEvent.className} - before: '${accessibilityEvent.beforeText}' after: '${accessibilityEvent.text}'")
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    Log.d("AccessibilityEvent", "VIEW_TEXT_SELECTION_CHANGED: ${accessibilityEvent.className} - ${accessibilityEvent.text}")
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d("AccessibilityEvent", "WINDOW_STATE_CHANGED: ${accessibilityEvent.className} - ${accessibilityEvent.text}")
                }
            }
            
            // Update overlay if needed
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
        DebugCursorView.hide()
    }
} 