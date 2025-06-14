package com.example.simple_agent_android.agentcore.metacognition

import com.example.simple_agent_android.agentcore.LLMClient
import org.json.JSONObject
import android.util.Log

object MetaCognition {
    private const val TAG = "AGENT_METACOGNITION"

    fun planTask(instruction: String, apiKey: String): String? {
        val llm = LLMClient(apiKey)
        val messages = listOf(
            mapOf("role" to "system", "content" to Prompts.systemPrompt),
            mapOf("role" to "user", "content" to """${Prompts.planningPrompt}

Current instruction: "$instruction"

Please analyze this task and respond in JSON format:
{
    "primary_objective": "Clear statement of what needs to be accomplished on Android",
    "success_criteria": ["Specific UI state 1", "Specific UI state 2", ...],
    "estimated_steps": ["Step 1", "Step 2", ...],
    "potential_challenges": ["Challenge 1", "Challenge 2", ...],
    "verification_methods": ["How to verify step 1", "How to verify step 2", ...]
}""")
        )
        val response = llm.sendWithTools(messages)
        Log.i(TAG, "planTask response: $response")
        val choices = response?.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.optString("content")
    }

    fun reflectOnStep(history: List<Map<String, String>>, apiKey: String): String? {
        // Only pass history up to the last non-tool message
        val trimmedHistory = history.toMutableList()
        while (trimmedHistory.isNotEmpty() && trimmedHistory.last()["role"] == "tool") {
            trimmedHistory.removeAt(trimmedHistory.size - 1)
        }
        
        val llm = LLMClient(apiKey)
        trimmedHistory.add(mapOf("role" to "user", "content" to """${Prompts.reflectionPrompt}

Please reflect on the current state and respond in JSON format:
{
    "last_interaction": "Description of what UI element was interacted with",
    "screen_changed": true/false,
    "progress_made": "How this moved closer to the goal",
    "available_elements": ["UI element 1", "UI element 2", ...],
    "next_approach": "What to try next or why current approach is working",
    "confidence_level": 0.0-1.0
}"""))
        
        val response = llm.sendWithTools(trimmedHistory)
        Log.i(TAG, "reflectOnStep response: $response")
        val choices = response?.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.optString("content")
    }

    fun shouldStop(history: List<Map<String, String>>, apiKey: String): Boolean {
        val llm = LLMClient(apiKey)
        val messages = history.toMutableList()
        messages.add(mapOf("role" to "user", "content" to """${Prompts.stoppingPrompt}

Please analyze the current state and respond in JSON format:
{
    "should_stop": true/false,
    "objective_completed": true/false,
    "ui_state_correct": true/false,
    "remaining_interactions": ["Interaction 1", "Interaction 2", ...],
    "reasoning": "Detailed explanation of decision"
}"""))
        
        val response = llm.sendWithTools(messages)
        Log.i(TAG, "shouldStop response: $response")
        val choices = response?.optJSONArray("choices") ?: return false
        if (choices.length() == 0) return false
        val message = choices.getJSONObject(0).getJSONObject("message")
        val content = message.optString("content")
        
        try {
            val json = JSONObject(content)
            return json.optBoolean("should_stop", false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse shouldStop response as JSON", e)
            return content.trim().lowercase().startsWith("yes")
        }
    }
} 