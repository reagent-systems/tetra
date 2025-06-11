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
} 