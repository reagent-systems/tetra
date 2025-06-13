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
} 