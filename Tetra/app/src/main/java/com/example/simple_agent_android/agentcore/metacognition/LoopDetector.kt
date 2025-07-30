package com.example.simple_agent_android.agentcore.metacognition

import android.util.Log
import org.json.JSONObject

object LoopDetector {
    private const val TAG = "AGENT_LOOP_DETECTOR"
    private const val MAX_SIMILAR_ACTIONS = 2
    private const val MAX_NO_PROGRESS_STEPS = 3

    data class LoopAnalysis(
        val isLooping: Boolean,
        val loopType: String,
        val severity: Int,
        val count: Int,
        val steps: List<Int>
    )

    fun analyzeHistory(history: List<Map<String, Any>>): LoopAnalysis {
        val toolCalls = history.filter { it["role"] == "tool" }
        
        // Check for exact action repetition
        val exactRepetition = detectExactRepetition(toolCalls)
        if (exactRepetition.isLooping) {
            return exactRepetition
        }

        // Check for semantic repetition (similar actions)
        val semanticRepetition = detectSemanticRepetition(toolCalls)
        if (semanticRepetition.isLooping) {
            return semanticRepetition
        }

        // Check for no progress (repeated screen states)
        val noProgress = detectNoProgress(history)
        if (noProgress.isLooping) {
            return noProgress
        }

        return LoopAnalysis(false, "none", 0, 0, emptyList())
    }

    private fun detectExactRepetition(toolCalls: List<Map<String, Any>>): LoopAnalysis {
        if (toolCalls.size < MAX_SIMILAR_ACTIONS) return LoopAnalysis(false, "none", 0, 0, emptyList())

        val lastActions = toolCalls.takeLast(MAX_SIMILAR_ACTIONS)
        val allSame = lastActions.all { it["content"] as? String == lastActions[0]["content"] as? String }
        
        if (allSame) {
            return LoopAnalysis(
                isLooping = true,
                loopType = "exact_repetition",
                severity = 2,
                count = MAX_SIMILAR_ACTIONS,
                steps = (toolCalls.size - MAX_SIMILAR_ACTIONS until toolCalls.size).toList()
            )
        }

        return LoopAnalysis(false, "none", 0, 0, emptyList())
    }

    private fun detectSemanticRepetition(toolCalls: List<Map<String, Any>>): LoopAnalysis {
        if (toolCalls.size < MAX_SIMILAR_ACTIONS) return LoopAnalysis(false, "none", 0, 0, emptyList())

        val lastActions = toolCalls.takeLast(MAX_SIMILAR_ACTIONS)
        
        // Check if all actions are of the same type (e.g., all presses or all text inputs)
        val actionTypes = lastActions.map { content ->
            val contentStr = content["content"] as? String
            when {
                contentStr?.contains("press", ignoreCase = true) == true -> {
                    if (contentStr.contains("EditText", ignoreCase = true)) "press_text_field"
                    else "press"
                }
                contentStr?.contains("text", ignoreCase = true) == true -> "text"
                contentStr?.contains("swipe", ignoreCase = true) == true -> "swipe"
                contentStr?.contains("home", ignoreCase = true) == true -> "home"
                contentStr?.contains("back", ignoreCase = true) == true -> "back"
                else -> "unknown"
            }
        }

        // Special handling for text input loops
        if (actionTypes.all { it == "press_text_field" }) {
            return LoopAnalysis(
                isLooping = true,
                loopType = "text_input_loop",
                severity = 3, // Higher severity for text input loops
                count = MAX_SIMILAR_ACTIONS,
                steps = (toolCalls.size - MAX_SIMILAR_ACTIONS until toolCalls.size).toList()
            )
        }

        if (actionTypes.all { it == actionTypes[0] } && actionTypes[0] != "unknown") {
            return LoopAnalysis(
                isLooping = true,
                loopType = "semantic_repetition",
                severity = 1,
                count = MAX_SIMILAR_ACTIONS,
                steps = (toolCalls.size - MAX_SIMILAR_ACTIONS until toolCalls.size).toList()
            )
        }

        return LoopAnalysis(false, "none", 0, 0, emptyList())
    }

    private fun detectNoProgress(history: List<Map<String, Any>>): LoopAnalysis {
        if (history.size < MAX_NO_PROGRESS_STEPS) return LoopAnalysis(false, "none", 0, 0, emptyList())

        // Get the last few screen states
        val screenStates = history
            .filter { it["role"] == "user" && (it["content"] as? String)?.startsWith("Current screen JSON:") == true }
            .takeLast(MAX_NO_PROGRESS_STEPS)
            .mapNotNull { (it["content"] as? String)?.removePrefix("Current screen JSON: ") }

        if (screenStates.size < MAX_NO_PROGRESS_STEPS) return LoopAnalysis(false, "none", 0, 0, emptyList())

        // Check if all recent screen states are identical
        val allSameState = screenStates.all { it == screenStates[0] }
        
        if (allSameState) {
            val hasRecentActions = history
                .filter { it["role"] == "tool" }
                .takeLast(MAX_NO_PROGRESS_STEPS)
                .isNotEmpty()

            return LoopAnalysis(
                isLooping = true,
                loopType = if (hasRecentActions) "no_action_confusion_with_recent_actions" else "no_action_confusion_without_recent_actions",
                severity = 3,
                count = MAX_NO_PROGRESS_STEPS,
                steps = (history.size - MAX_NO_PROGRESS_STEPS until history.size).toList()
            )
        }

        return LoopAnalysis(false, "none", 0, 0, emptyList())
    }
} 