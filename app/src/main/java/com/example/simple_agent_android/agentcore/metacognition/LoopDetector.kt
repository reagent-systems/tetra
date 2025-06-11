package com.example.simple_agent_android.agentcore.metacognition

object LoopDetector {
    fun isLooping(history: List<Map<String, String>>): Boolean {
        // Simple loop detection: check if the last two tool calls are the same
        val toolCalls = history.filter { it["role"] == "tool" }
        if (toolCalls.size < 2) return false
        val last = toolCalls[toolCalls.size - 1]["content"]
        val secondLast = toolCalls[toolCalls.size - 2]["content"]
        return last == secondLast
    }

    fun isNoProgress(history: List<Map<String, String>>): Boolean {
        // Check if the last few steps did not change the screen or state
        // To be implemented: compare screen JSONs or tool results
        return false
    }
} 