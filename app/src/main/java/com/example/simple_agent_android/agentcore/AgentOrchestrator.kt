package com.example.simple_agent_android.agentcore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.simple_agent_android.agentcore.AgentActions
import com.example.simple_agent_android.agentcore.metacognition.MetaCognition
import com.example.simple_agent_android.agentcore.metacognition.LoopDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

object AgentOrchestrator {
    private const val TAG = "AGENT_CORE"
    private var agentJob: Job? = null

    fun runAgent(instruction: String, apiKey: String, context: Context, onAgentStopped: (() -> Unit)? = null, onOutput: ((String) -> Unit)? = null) {
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            com.example.simple_agent_android.BoundingBoxAccessibilityService.showStopButton()
            Log.i(TAG, "Agent started with instruction: $instruction")
            onOutput?.invoke("Agent started with instruction: $instruction")
            delay(1000)
            val systemPrompt = mapOf("role" to "system", "content" to "You are an Android agent. Use the available tools to interact with the phone. Only use the provided tools.")
            val userInstruction = mapOf("role" to "user", "content" to instruction)
            val plan = MetaCognition.planTask(instruction, apiKey)
            Log.i(TAG, "Agent plan: $plan")
            if (plan != null) onOutput?.invoke("Plan: $plan")
            val assistantPlan = if (plan != null) mapOf("role" to "assistant", "content" to plan) else null
            var step = 0
            var lastAction: String? = null
            // Start with initial message history
            val baseMessages = mutableListOf(systemPrompt, userInstruction)
            if (assistantPlan != null) baseMessages.add(assistantPlan)
            var messages = baseMessages.toMutableList()
            var pendingReflection = false
            var pendingShouldStop = false
            var lastReflection: String? = null
            var lastShouldStop: Boolean = false
            while (step < 10) {
                coroutineContext.ensureActive()
                step++
                // Remove any previous screen JSON user message
                messages = messages.filterNot { it["role"] == "user" && it["content"]?.startsWith("Current screen JSON:") == true }.toMutableList()
                // Remove any trailing tool messages before LLM call
                while (messages.isNotEmpty() && messages.last()["role"] == "tool") {
                    messages.removeAt(messages.size - 1)
                }
                // Add the latest screen JSON as a user message
                val screenJson = AgentActions.getScreenJson()
                messages.add(mapOf("role" to "user", "content" to "Current screen JSON: $screenJson"))
                if (messages.none { it["role"] == "system" && it["content"]?.contains("Always respond with a tool call") == true }) {
                    messages.add(0, mapOf("role" to "system", "content" to "Always respond with a tool call (function call) if possible, even if you are unsure. If you are stuck, make your best guess based on the screen JSON and the last action."))
                }

                // If pending, do reflection and stopping check on the latest state
                if (pendingReflection) {
                    coroutineContext.ensureActive()
                    val reflection = MetaCognition.reflectOnStep(messages, apiKey)
                    Log.i(TAG, "Step $step: Reflection: $reflection")
                    lastReflection = reflection
                    pendingReflection = false
                    if (reflection != null) onOutput?.invoke("Reflection: $reflection")
                }
                if (pendingShouldStop) {
                    coroutineContext.ensureActive()
                    val shouldStop = MetaCognition.shouldStop(messages, apiKey)
                    Log.i(TAG, "Step $step: Should stop? $shouldStop")
                    lastShouldStop = shouldStop
                    pendingShouldStop = false
                    onOutput?.invoke("Should stop? $shouldStop")
                    if (shouldStop) {
                        Log.i(TAG, "Step $step: Metacognition decided to stop.")
                        onOutput?.invoke("Agent decided to stop.")
                        break
                    }
                }

                Log.i(TAG, "Step $step: MESSAGES before LLM call: ${messages.map { it.toString() }}")
                try {
                    coroutineContext.ensureActive()
                    val llm = LLMClient(apiKey)
                    Log.i(TAG, "Step $step: Calling LLM with tools...")
                    val response = llm.sendWithTools(messages)
                    coroutineContext.ensureActive()
                    Log.i(TAG, "Step $step: LLM response: $response")
                    if (response == null || response.has("error")) {
                        Log.e(TAG, "LLM error: ${response?.optString("error")}")
                        Log.e(TAG, "Stopping agent due to LLM error.")
                        onOutput?.invoke("LLM error: ${response?.optString("error")}")
                        break
                    }
                    val choices = response.optJSONArray("choices") ?: break
                    if (choices.length() == 0) break
                    val choice = choices.getJSONObject(0)
                    val message = choice.getJSONObject("message")
                    val toolCalls = message.optJSONArray("tool_calls")
                    val content = message.optString("content")
                    if (toolCalls != null && toolCalls.length() > 0) {
                        // For each tool call, perform the action and add a tool message
                        for (i in 0 until toolCalls.length()) {
                            val toolCall = toolCalls.getJSONObject(i)
                            val function = toolCall.getJSONObject("function")
                            val name = function.getString("name")
                            val arguments = JSONObject(function.getString("arguments"))
                            when (name) {
                                "simulate_press" -> {
                                    val centerX = arguments.getInt("center_x")
                                    val centerY = arguments.getInt("center_y")
                                    Log.i(TAG, "Step $step: Simulating press at center ($centerX, $centerY)")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.simulatePressAt(centerX, centerY)
                                    }
                                    lastAction = "Pressed at center ($centerX, $centerY)"
                                    // Add tool message immediately after assistant message with tool_calls
                                    messages.add(mapOf(
                                        "role" to "tool",
                                        "tool_call_id" to toolCall.getString("id"),
                                        "content" to "Pressed at center ($centerX, $centerY)"
                                    ))
                                    onOutput?.invoke("Simulated press at ($centerX, $centerY)")
                                    delay(500)
                                    coroutineContext.ensureActive()
                                }
                                "set_text" -> {
                                    val x = arguments.getInt("x")
                                    val y = arguments.getInt("y")
                                    val text = arguments.getString("text")
                                    Log.i(TAG, "Step $step: Setting text at ($x, $y): $text")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.setTextAt(x, y, text)
                                    }
                                    lastAction = "Set text at ($x, $y): $text"
                                    messages.add(mapOf(
                                        "role" to "tool",
                                        "tool_call_id" to toolCall.getString("id"),
                                        "content" to "Set text at ($x, $y): $text"
                                    ))
                                    onOutput?.invoke("Set text at ($x, $y): $text")
                                }
                                else -> {
                                    Log.e(TAG, "Step $step: Unknown tool call: $name")
                                    lastAction = "Unknown tool call: $name"
                                    onOutput?.invoke("Unknown tool call: $name")
                                }
                            }
                        }
                        // Remove any previous screen JSON user message before adding new one
                        messages = messages.filterNot { it["role"] == "user" && it["content"]?.startsWith("Current screen JSON:") == true }.toMutableList()
                        // Remove any trailing tool messages before LLM call (for reflection, stopping, etc)
                        while (messages.isNotEmpty() && messages.last()["role"] == "tool") {
                            messages.removeAt(messages.size - 1)
                        }
                        val newScreenJson = AgentActions.getScreenJson()
                        messages.add(mapOf("role" to "user", "content" to "Current screen JSON: $newScreenJson"))
                        // Set flags to do reflection and stopping check at the start of the next loop
                        pendingReflection = true
                        pendingShouldStop = true
                        continue
                    } else if (content.isNotBlank()) {
                        Log.i(TAG, "Step $step: No tool calls, but got content: $content. Adding to messages and continuing.")
                        messages.add(mapOf("role" to "assistant", "content" to content))
                        onOutput?.invoke(content)
                        delay(200)
                        coroutineContext.ensureActive()
                        continue
                    } else {
                        Log.i(TAG, "Step $step: No tool calls and no content. Agent done.")
                        onOutput?.invoke("Agent done.")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Step $step: Exception in agent loop", e)
                }
                delay(300)
                coroutineContext.ensureActive()
            }
            Log.i(TAG, "Agent finished.")
            onOutput?.invoke("Agent finished.")
            com.example.simple_agent_android.BoundingBoxAccessibilityService.hideStopButton()
            onAgentStopped?.invoke()
        }
    }

    fun stopAgent() {
        agentJob?.cancel()
        agentJob = null
        Log.i(TAG, "Agent stopped by user.")
        com.example.simple_agent_android.BoundingBoxAccessibilityService.hideStopButton()
    }
} 