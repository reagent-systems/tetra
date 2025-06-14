package com.example.simple_agent_android.agentcore

import com.example.simple_agent_android.BoundingBoxAccessibilityService

object AgentActions {
    fun getScreenJson(): String {
        return BoundingBoxAccessibilityService.getInteractiveElementsJson()
    }

    fun simulatePressAt(x: Int, y: Int) {
        BoundingBoxAccessibilityService.simulatePressAt(x, y)
    }

    fun setTextAt(x: Int, y: Int, text: String) {
        BoundingBoxAccessibilityService.setTextAt(x, y, text)
    }

    fun goHome() {
        BoundingBoxAccessibilityService.goHome()
    }

    fun goBack() {
        BoundingBoxAccessibilityService.goBack()
    }

    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300) {
        BoundingBoxAccessibilityService.swipe(startX, startY, endX, endY, duration)
    }

    fun waitFor(durationMs: Long) {
        Thread.sleep(durationMs)
    }

    fun waitForElement(
        text: String? = null,
        contentDescription: String? = null,
        className: String? = null,
        timeoutMs: Long = 5000L,
        pollIntervalMs: Long = 300L
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val json = BoundingBoxAccessibilityService.getInteractiveElementsJson()
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if ((text == null || obj.optString("text") == text) &&
                        (contentDescription == null || obj.optString("contentDescription") == contentDescription) &&
                        (className == null || obj.optString("className") == className)) {
                        return true
                    }
                }
            } catch (_: Exception) {}
            Thread.sleep(pollIntervalMs)
        }
        return false
    }
} 