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
    "success_criteria_met": ["Criterion 1 met", "Criterion 2 not met", ...],
    "reasoning": "Detailed explanation of decision",
    "is_in_loop": {
        "detected": true/false,
        "repeated_actions": ["Action 1", "Action 2", ...],
        "loop_count": 0,
        "severity": 0
    },
    "confidence": 0.0-1.0
}

Important guidelines:
1. If the primary objective is achieved (e.g. app opened, content loaded), return should_stop: true
2. If the same action is repeated more than 2 times without progress, consider it a loop
3. If no meaningful progress is made in the last 2 steps, consider stopping
4. If the UI state matches the expected end state, stop even if there are theoretical remaining actions
5. Prioritize successful completion over exhaustive interaction
6. If confidence is high (>0.8) that the task is done, stop even if some minor criteria aren't met
7. If in a loop with severity > 2, strongly consider stopping"""))
        
        val response = llm.sendWithTools(messages)
        Log.i(TAG, "shouldStop response: $response")
        val choices = response?.optJSONArray("choices") ?: return false
        if (choices.length() == 0) return false
        val message = choices.getJSONObject(0).getJSONObject("message")
        val content = message.optString("content")
        
        try {
            val json = JSONObject(content)
            val shouldStop = json.optBoolean("should_stop", false)
            val isInLoop = json.optJSONObject("is_in_loop")
            val objectiveCompleted = json.optBoolean("objective_completed", false)
            val confidence = json.optDouble("confidence", 0.0)
            
            // Stop in any of these cases:
            // 1. Explicit recommendation to stop
            // 2. Objective is completed
            // 3. High confidence (>0.8) that task is done
            // 4. In a severe loop (severity > 2)
            if (shouldStop || objectiveCompleted || confidence > 0.8) {
                Log.i(TAG, "Stopping due to completion: shouldStop=$shouldStop, objectiveCompleted=$objectiveCompleted, confidence=$confidence")
                return true
            }
            
            // Enhanced loop detection handling
            if (isInLoop != null && isInLoop.optBoolean("detected", false)) {
                val severity = isInLoop.optInt("severity", 0)
                val loopCount = isInLoop.optInt("loop_count", 0)
                
                // Stop if:
                // 1. Loop severity is high (>2)
                // 2. Loop count is high (>3)
                // 3. Combination of moderate severity (2) and count (2)
                if (severity > 2 || loopCount > 3 || (severity >= 2 && loopCount >= 2)) {
                    Log.i(TAG, "Stopping due to severe loop: severity=$severity, loopCount=$loopCount")
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse shouldStop response as JSON", e)
            return content.trim().lowercase().startsWith("yes")
        }
    }
} 