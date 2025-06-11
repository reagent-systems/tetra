package com.example.simple_agent_android.agentcore.metacognition

import com.example.simple_agent_android.agentcore.LLMClient
import org.json.JSONObject

object MetaCognition {
    fun planTask(instruction: String, apiKey: String): String? {
        val llm = LLMClient(apiKey)
        val messages = listOf(
            mapOf("role" to "system", "content" to Prompts.systemPrompt),
            mapOf("role" to "user", "content" to Prompts.planningPrompt + "\nInstruction: $instruction")
        )
        val response = llm.sendWithTools(messages)
        val choices = response?.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.optString("content")
    }

    fun reflectOnStep(history: List<Map<String, String>>, apiKey: String): String? {
        // Only pass history up to the last non-tool message (OpenAI API requires tool messages only after tool_calls)
        val trimmedHistory = history.toMutableList()
        while (trimmedHistory.isNotEmpty() && trimmedHistory.last()["role"] == "tool") {
            trimmedHistory.removeAt(trimmedHistory.size - 1)
        }
        val llm = LLMClient(apiKey)
        trimmedHistory.add(mapOf("role" to "user", "content" to Prompts.reflectionPrompt))
        val response = llm.sendWithTools(trimmedHistory)
        android.util.Log.i("AGENT_CORE", "MetaCognition.reflectOnStep LLM response: $response")
        val choices = response?.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.optString("content")
    }

    fun shouldStop(history: List<Map<String, String>>, apiKey: String): Boolean {
        val llm = LLMClient(apiKey)
        val messages = history.toMutableList()
        messages.add(mapOf("role" to "user", "content" to Prompts.stoppingPrompt))
        val response = llm.sendWithTools(messages)
        val choices = response?.optJSONArray("choices") ?: return false
        if (choices.length() == 0) return false
        val message = choices.getJSONObject(0).getJSONObject("message")
        val content = message.optString("content")
        return content.trim().lowercase().startsWith("yes")
    }
} 