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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.simple_agent_android.agentcore.metacognition.Prompts

object AgentOrchestrator {
    private const val TAG = "AGENT_CORE"
    private var agentJob: Job? = null
    private var paused: Boolean = false

    fun runAgent(instruction: String, apiKey: String, context: Context, onAgentStopped: (() -> Unit)? = null, onOutput: ((String) -> Unit)? = null) {
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            com.example.simple_agent_android.BoundingBoxAccessibilityService.showStopButton()
            Log.i(TAG, "Agent started with instruction: $instruction")
            onOutput?.invoke("Agent started with instruction: $instruction")
            delay(1000)

            // Enhanced system prompt with current date/time
            val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val currentYear = LocalDateTime.now().year
            val systemPrompt = mapOf("role" to "system", "content" to """${Prompts.systemPrompt}

Current date and time: $currentDateTime
Your knowledge cutoff might be earlier, but you should consider the current date when processing tasks.
Always work with the understanding that it is now $currentYear when handling time-sensitive information.

${Prompts.getDateReminder()}""")

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
            
            while (step < 10) {
                coroutineContext.ensureActive()
                while (paused) {
                    delay(200)
                    coroutineContext.ensureActive()
                }
                step++

                // Check for loops before proceeding
                val loopAnalysis = LoopDetector.analyzeHistory(messages)
                if (loopAnalysis.isLooping) {
                    Log.i(TAG, "Step $step: Loop detected - type: ${loopAnalysis.loopType}, severity: ${loopAnalysis.severity}")
                    onOutput?.invoke("Loop detected - analyzing situation...")
                    
                    // Add loop breaking prompt
                    messages.add(mapOf("role" to "user", "content" to """${Prompts.loopBreakingPrompt}

You are stuck in a ${loopAnalysis.loopType} loop (Severity: ${loopAnalysis.severity}).
You have repeated similar actions ${loopAnalysis.count} times across steps: ${loopAnalysis.steps.joinToString(", ")}.

Original instruction: "$instruction"

${Prompts.loopBreakingDecisionFormat}"""))
                }

                // Remove any previous screen JSON user message
                messages = messages.filterNot { it["role"] == "user" && it["content"]?.startsWith("Current screen JSON:") == true }.toMutableList()
                // Remove any trailing tool messages before LLM call
                while (messages.isNotEmpty() && messages.last()["role"] == "tool") {
                    messages.removeAt(messages.size - 1)
                }
                
                // Add the latest screen JSON as a user message
                val screenJson = AgentActions.getScreenJson()
                messages.add(mapOf("role" to "user", "content" to "Current screen JSON: $screenJson"))
                
                // Add tool call guidance
                if (messages.none { it["role"] == "system" && it["content"]?.contains("Always respond with a tool call") == true }) {
                    messages.add(0, mapOf("role" to "system", "content" to Prompts.toolCallGuidance))
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
                            
                            // Execute the tool call and get the result
                            val toolResult = when (name) {
                                "simulate_press" -> {
                                    val centerX = arguments.getInt("center_x")
                                    val centerY = arguments.getInt("center_y")
                                    Log.i(TAG, "Step $step: Simulating press at center ($centerX, $centerY)")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.simulatePressAt(centerX, centerY)
                                    }
                                    lastAction = "Pressed at center ($centerX, $centerY)"
                                    onOutput?.invoke("Simulated press at ($centerX, $centerY)")
                                    "Pressed at center ($centerX, $centerY)"
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
                                    onOutput?.invoke("Set text at ($x, $y): $text")
                                    "Set text at ($x, $y): $text"
                                }
                                "go_home" -> {
                                    Log.i(TAG, "Step $step: Going home")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.goHome()
                                    }
                                    lastAction = "Went home"
                                    onOutput?.invoke("Went home")
                                    "Went home"
                                }
                                "go_back" -> {
                                    Log.i(TAG, "Step $step: Going back")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.goBack()
                                    }
                                    lastAction = "Went back"
                                    onOutput?.invoke("Went back")
                                    "Went back"
                                }
                                "swipe" -> {
                                    val startX = arguments.getInt("startX")
                                    val startY = arguments.getInt("startY")
                                    val endX = arguments.getInt("endX")
                                    val endY = arguments.getInt("endY")
                                    val duration = if (arguments.has("duration")) arguments.getLong("duration") else 300L
                                    Log.i(TAG, "Step $step: Swiping from ($startX, $startY) to ($endX, $endY) duration $duration ms")
                                    Handler(Looper.getMainLooper()).post {
                                        AgentActions.swipe(startX, startY, endX, endY, duration)
                                    }
                                    lastAction = "Swiped from ($startX, $startY) to ($endX, $endY)"
                                    onOutput?.invoke("Swiped from ($startX, $startY) to ($endX, $endY)")
                                    "Swiped from ($startX, $startY) to ($endX, $endY)"
                                }
                                else -> {
                                    Log.e(TAG, "Step $step: Unknown tool call: $name")
                                    lastAction = "Unknown tool call: $name"
                                    onOutput?.invoke("Unknown tool call: $name")
                                    "Unknown tool call: $name"
                                }
                            }

                            // Add the tool result to messages
                            messages.add(mapOf(
                                "role" to "tool",
                                "tool_call_id" to toolCall.getString("id"),
                                "content" to toolResult
                            ))

                            // Small delay to let UI update
                            delay(1200)
                            coroutineContext.ensureActive()

                            // Get the new screen state after the action
                            val newScreenJson = AgentActions.getScreenJson()
                            messages.add(mapOf("role" to "user", "content" to "Current screen JSON: $newScreenJson"))

                            // Do reflection immediately after each action
                            val reflection = MetaCognition.reflectOnStep(messages, apiKey)
                            Log.i(TAG, "Step $step: Reflection after action: $reflection")
                            if (reflection != null) {
                                onOutput?.invoke("Reflection: $reflection")
                                messages.add(mapOf("role" to "assistant", "content" to reflection))
                            }

                            // Check if we should stop after each action
                            val shouldStop = MetaCognition.shouldStop(messages, apiKey)
                            Log.i(TAG, "Step $step: Should stop after action? $shouldStop")
                            onOutput?.invoke("Should stop? $shouldStop")
                            if (shouldStop) {
                                Log.i(TAG, "Step $step: Metacognition decided to stop.")
                                onOutput?.invoke("Agent decided to stop.")
                                return@launch
                            }
                        }
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

    fun pauseAgent() {
        paused = true
        Log.i(TAG, "Agent paused by user.")
    }

    fun resumeAgent() {
        paused = false
        Log.i(TAG, "Agent resumed by user.")
    }

    fun isPaused(): Boolean = paused
} 