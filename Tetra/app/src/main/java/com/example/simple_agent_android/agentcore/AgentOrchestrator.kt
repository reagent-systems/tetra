package com.example.simple_agent_android.agentcore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import org.json.JSONObject
import com.example.simple_agent_android.sentry.AgentErrorTracker
import com.example.simple_agent_android.sentry.trackUserAction
import com.example.simple_agent_android.sentry.sentryAgentOperation
import com.example.simple_agent_android.utils.LogManager
import com.example.simple_agent_android.utils.LogManager.LogLevel

object AgentOrchestrator {
    private const val TAG = "AGENT_CORE"
    private const val MAX_STEPS = 15
    private var agentJob: Job? = null
    private var paused: Boolean = false
    private var stopping: Boolean = false

    fun runAgent(instruction: String, apiKey: String, context: Context, baseUrl: String = "https://api.openai.com", model: String = "gpt-4o", onAgentStopped: (() -> Unit)? = null, onOutput: ((String) -> Unit)? = null) {
        stopping = false
        
        // Track agent start
        trackUserAction("agent_start", "home", mapOf("instruction_length" to instruction.length))
        val agentTransaction = AgentErrorTracker.startAgentTransaction(instruction)
        val startTime = System.currentTimeMillis()
        
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            var step = 0
            try {
                LogManager.log(TAG, "Agent started with instruction: $instruction")
                onOutput?.invoke("🤖 Starting task: $instruction")
                delay(1000)

                // Simple system prompt
                val systemPrompt = mapOf("role" to "system", "content" to """You are an Android automation agent. Your job is to help users complete tasks on their Android device by interacting with the UI.

You have access to these functions:
- simulate_press(center_x, center_y): Press at coordinates from the screen JSON
- set_text(x, y, text): Set text in input field at coordinates
- go_home(): Go to home screen
- go_back(): Press back button  
- swipe(startX, startY, endX, endY, duration): Swipe gesture
- wait_for(duration_ms): Wait for specified time
- wait_for_element(text, contentDescription, className, timeout_ms): Wait for element to appear

Guidelines:
1. Analyze the screen JSON to understand available UI elements
2. Use function calls to interact with the UI step by step
3. For text input: ALWAYS use this sequence:
   - First: simulate_press on the text field to focus it
   - Then: wait_for(500) to ensure the field is focused
   - Finally: set_text with the SAME coordinates to input the text
4. For EditText fields, use the center_x and center_y coordinates for both pressing and setting text
5. Be patient with loading screens - use wait_for or wait_for_element
6. If text input fails, try pressing the field again and retry set_text
7. If you achieve the user's goal, say "task complete" in your response
8. Work efficiently and directly towards the goal

Focus on completing the user's request efficiently and accurately.""")

                val userInstruction = mapOf("role" to "user", "content" to instruction)
                
                // Start with initial message history
                val messages = mutableListOf<Map<String, Any>>(systemPrompt, userInstruction)
                var lastAction: String? = null
                val previousActions = mutableListOf<String>() // Simple loop detection
                
                while (step < MAX_STEPS && !stopping) {
                    if (stopping) {
                        LogManager.log(TAG, "Agent stopping requested, breaking loop")
                        break
                    }
                    coroutineContext.ensureActive()
                    while (paused) {
                        if (stopping) {
                            LogManager.log(TAG, "Agent stopping requested while paused")
                            break
                        }
                        delay(200)
                        coroutineContext.ensureActive()
                    }
                    if (stopping) break
                    step++

                    LogManager.log(TAG, "Step $step: Getting screen state")
                    onOutput?.invoke("📱 Step $step: Analyzing screen...")

                    // Get current screen state with retry
                    var screenJson = AgentActions.getScreenJson()
                    var emptyTries = 0
                    while ((screenJson.trim() == "[]" || screenJson.trim().isEmpty()) && emptyTries < 3) {
                        LogManager.log(TAG, "Screen JSON is empty, waiting for app to load... (try ${emptyTries+1})")
                        onOutput?.invoke("⏳ Screen is empty, waiting for app to load... (try ${emptyTries+1})")
                        AgentActions.waitFor(2000) // Wait 2 seconds
                        screenJson = AgentActions.getScreenJson()
                        emptyTries++
                    }
                    
                    if (screenJson.trim() == "[]" || screenJson.trim().isEmpty()) {
                        LogManager.log(TAG, "Screen JSON still empty after retries, stopping")
                        onOutput?.invoke("❌ Cannot read screen after retries, stopping")
                        break
                    }

                    // Remove any previous screen JSON messages to keep conversation clean
                    messages.removeAll { it["role"] == "user" && (it["content"] as? String)?.startsWith("Current screen JSON:") == true }
                    
                    // Add current screen state to conversation
                    messages.add(mapOf("role" to "user", "content" to "Current screen JSON: $screenJson"))
                    
                    LogManager.log(TAG, "Step $step: Calling LLM with tools...")
                    
                    try {
                        if (stopping) {
                            LogManager.log(TAG, "Agent stopping requested before LLM call")
                            break
                        }
                        coroutineContext.ensureActive()
                        
                        val llm = LLMClient(apiKey, baseUrl, model)
                        val response = llm.sendWithTools(messages)
                        
                        if (stopping) {
                            LogManager.log(TAG, "Agent stopping requested after LLM call")
                            break
                        }
                        coroutineContext.ensureActive()
                        
                        LogManager.log(TAG, "Step $step: LLM response: $response")
                        
                        if (response == null || response.has("error")) {
                            val errorMessage = response?.optString("error") ?: "Unknown LLM error"
                            LogManager.log(TAG, "LLM error: $errorMessage", LogLevel.ERROR)
                            onOutput?.invoke("❌ LLM error: $errorMessage")
                            break
                        }
                        
                        val choices = response.optJSONArray("choices") ?: break
                        if (choices.length() == 0) break
                        
                        val choice = choices.getJSONObject(0)
                        val message = choice.getJSONObject("message")
                        val content = message.optString("content", "")
                        val toolCalls = message.optJSONArray("tool_calls")
                        
                        // Check if task is complete
                        if (content.contains("task complete", true) || 
                            content.contains("objective achieved", true) ||
                            content.contains("successfully completed", true)) {
                            LogManager.log(TAG, "Step $step: Task completed!")
                            onOutput?.invoke("✅ Task completed successfully!")
                            
                            val executionTime = System.currentTimeMillis() - startTime
                            AgentErrorTracker.trackTaskSuccess(
                                instruction = instruction,
                                totalSteps = step,
                                executionTimeMs = executionTime,
                                context = mapOf("completion_method" to "content_analysis")
                            )
                            agentTransaction.finish()
                            break
                        }
                        
                        if (toolCalls != null && toolCalls.length() > 0) {
                            // Add assistant message to conversation
                            val toolCallsList = mutableListOf<Map<String, Any>>()
                            for (i in 0 until toolCalls.length()) {
                                val toolCall = toolCalls.getJSONObject(i)
                                toolCallsList.add(mapOf(
                                    "id" to toolCall.getString("id"),
                                    "type" to toolCall.getString("type"),
                                    "function" to mapOf(
                                        "name" to toolCall.getJSONObject("function").getString("name"),
                                        "arguments" to toolCall.getJSONObject("function").getString("arguments")
                                    )
                                ))
                            }
                            
                            messages.add(mapOf(
                                "role" to "assistant",
                                "content" to content,
                                "tool_calls" to toolCallsList
                            ))
                            
                            // Execute tool calls
                            for (i in 0 until toolCalls.length()) {
                                val toolCall = toolCalls.getJSONObject(i)
                                val function = toolCall.getJSONObject("function")
                                val name = function.getString("name")
                                val arguments = JSONObject(function.getString("arguments"))
                                
                                // Simple loop detection
                                val actionSignature = "$name:${arguments.toString()}"
                                if (previousActions.takeLast(3).count { it == actionSignature } >= 2) {
                                    LogManager.log(TAG, "Step $step: Loop detected, trying different approach")
                                    onOutput?.invoke("🔄 Loop detected, trying different approach...")
                                    
                                    // Still need to add tool result to prevent API error
                                    messages.add(mapOf(
                                        "role" to "tool",
                                        "tool_call_id" to toolCall.getString("id"),
                                        "content" to "Loop detected: This action was repeated too many times. Try a different approach."
                                    ))
                                    
                                    // Add user message to guide the agent
                                    messages.add(mapOf(
                                        "role" to "user",
                                        "content" to "You seem to be repeating the same action. Try a different approach or declare the task complete if the goal is achieved."
                                    ))
                                    continue // Continue to next tool call instead of breaking
                                }
                                previousActions.add(actionSignature)
                                if (previousActions.size > 10) previousActions.removeAt(0)
                                
                                // Execute the tool call
                                val toolResult = executeToolCall(name, arguments, step, onOutput)
                                lastAction = toolResult
                                
                                // Add tool result to conversation
                                messages.add(mapOf(
                                    "role" to "tool",
                                    "tool_call_id" to toolCall.getString("id"),
                                    "content" to toolResult
                                ))
                                
                                // Small delay to let UI update
                                delay(1500)
                                coroutineContext.ensureActive()
                            }
                        } else if (content.isNotBlank()) {
                            LogManager.log(TAG, "Step $step: No tool calls, got content: $content")
                            messages.add(mapOf("role" to "assistant", "content" to content))
                            onOutput?.invoke("💭 $content")
                        } else {
                            LogManager.log(TAG, "Step $step: No tool calls and no content. Continuing...")
                        }
                        
                    } catch (e: Exception) {
                        LogManager.log(TAG, "Error in agent loop: ${e.message}", LogLevel.ERROR)
                        onOutput?.invoke("❌ Error: ${e.message}")
                        
                        AgentErrorTracker.trackAgentError(
                            error = e,
                            agentStep = step,
                            instruction = instruction,
                            context = mapOf(
                                "error_location" to "agent_main_loop",
                                "last_action" to (lastAction ?: "none")
                            )
                        )
                        break
                    }
                }
                
                if (step >= MAX_STEPS) {
                    LogManager.log(TAG, "Reached maximum steps")
                    onOutput?.invoke("⏰ Reached maximum steps, stopping")
                }
                
            } catch (e: Exception) {
                LogManager.log(TAG, "Agent failed", LogLevel.ERROR)
                onOutput?.invoke("❌ Agent failed: ${e.message}")
                
                AgentErrorTracker.trackAgentError(
                    error = e,
                    agentStep = step,
                    instruction = instruction,
                    context = mapOf("error_location" to "agent_outer_loop")
                )
            } finally {
                agentTransaction.finish()
                onAgentStopped?.invoke()
            }
        }
    }
    
    private fun executeToolCall(
        name: String,
        arguments: JSONObject,
        step: Int,
        onOutput: ((String) -> Unit)?
    ): String {
        return try {
            when (name) {
                "simulate_press" -> {
                    val centerX = arguments.getInt("center_x")
                    val centerY = arguments.getInt("center_y")
                    LogManager.log(TAG, "Step $step: Simulating press at ($centerX, $centerY)")
                    Handler(Looper.getMainLooper()).post {
                        AgentActions.simulatePressAt(centerX, centerY)
                    }
                    onOutput?.invoke("👆 Pressed at ($centerX, $centerY)")
                    "Pressed at coordinates ($centerX, $centerY)"
                }
                "set_text" -> {
                    val x = arguments.getInt("x")
                    val y = arguments.getInt("y")
                    val text = arguments.getString("text")
                    LogManager.log(TAG, "Step $step: Setting text at ($x, $y): '$text'")
                    
                    try {
                        Handler(Looper.getMainLooper()).post {
                            AgentActions.setTextAt(x, y, text)
                        }
                        
                        // Wait for the text setting to complete
                        Thread.sleep(800)
                        
                        onOutput?.invoke("⌨️ Set text: '$text' at ($x, $y)")
                        "Set text '$text' at coordinates ($x, $y)"
                        
                    } catch (e: Exception) {
                        LogManager.log(TAG, "Error setting text: ${e.message}", LogLevel.ERROR)
                        onOutput?.invoke("❌ Failed to set text: ${e.message}")
                        "Failed to set text '$text' at coordinates ($x, $y). Error: ${e.message}"
                    }
                }
                "wait_for" -> {
                    val duration = arguments.getLong("duration_ms")
                    LogManager.log(TAG, "Step $step: Waiting for $duration ms")
                    AgentActions.waitFor(duration)
                    onOutput?.invoke("⏱️ Waited ${duration}ms")
                    "Waited for ${duration}ms"
                }
                "wait_for_element" -> {
                    val text = if (arguments.has("text")) arguments.optString("text", null) else null
                    val contentDescription = if (arguments.has("contentDescription")) arguments.optString("contentDescription", null) else null
                    val className = if (arguments.has("className")) arguments.optString("className", null) else null
                    val timeout = if (arguments.has("timeout_ms")) arguments.getLong("timeout_ms") else 5000L
                    LogManager.log(TAG, "Step $step: Waiting for element up to $timeout ms")
                    val found = AgentActions.waitForElement(text, contentDescription, className, timeout)
                    val result = if (found) "Element found" else "Element not found within timeout"
                    onOutput?.invoke("⏳ $result")
                    result
                }
                "go_home" -> {
                    LogManager.log(TAG, "Step $step: Going home")
                    Handler(Looper.getMainLooper()).post {
                        AgentActions.goHome()
                    }
                    onOutput?.invoke("🏠 Went home")
                    "Navigated to home screen"
                }
                "go_back" -> {
                    LogManager.log(TAG, "Step $step: Going back")
                    Handler(Looper.getMainLooper()).post {
                        AgentActions.goBack()
                    }
                    onOutput?.invoke("⬅️ Went back")
                    "Pressed back button"
                }
                "swipe" -> {
                    val startX = arguments.getInt("startX")
                    val startY = arguments.getInt("startY")
                    val endX = arguments.getInt("endX")
                    val endY = arguments.getInt("endY")
                    val duration = if (arguments.has("duration")) arguments.getLong("duration") else 300L
                    LogManager.log(TAG, "Step $step: Swiping from ($startX, $startY) to ($endX, $endY)")
                    Handler(Looper.getMainLooper()).post {
                        AgentActions.swipe(startX, startY, endX, endY, duration)
                    }
                    onOutput?.invoke("👆 Swiped from ($startX, $startY) to ($endX, $endY)")
                    "Swiped from ($startX, $startY) to ($endX, $endY)"
                }
                else -> {
                    LogManager.log(TAG, "Step $step: Unknown tool call: $name")
                    onOutput?.invoke("❓ Unknown action: $name")
                    "Unknown function: $name"
                }
            }
        } catch (e: Exception) {
            LogManager.log(TAG, "Error executing $name: ${e.message}", LogLevel.ERROR)
            val errorMsg = "Error executing $name: ${e.message}"
            onOutput?.invoke("❌ $errorMsg")
            errorMsg
        }
    }

    fun stopAgent() {
        stopping = true
        agentJob?.cancel()
        agentJob = null
    }

    fun pauseAgent() {
        paused = true
    }

    fun resumeAgent() {
        paused = false
    }

    fun isPaused(): Boolean = paused
} 