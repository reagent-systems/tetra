package com.example.simple_agent_android.agentcore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.simple_agent_android.agentcore.AgentActions
import com.example.simple_agent_android.agentcore.ScreenAnalyzer
import com.example.simple_agent_android.agentcore.TaskContextManager
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
import com.example.simple_agent_android.utils.LogManager
import com.example.simple_agent_android.utils.LogManager.LogLevel
import com.example.simple_agent_android.sentry.AgentErrorTracker
import com.example.simple_agent_android.sentry.SentryManager
import com.example.simple_agent_android.sentry.sentryAgentOperation
import com.example.simple_agent_android.sentry.trackUserAction

object AgentOrchestrator {
    private const val TAG = "AGENT_CORE"
    private var agentJob: Job? = null
    private var paused: Boolean = false
    private var stopping: Boolean = false

    fun runAgent(instruction: String, apiKey: String, context: Context, onAgentStopped: (() -> Unit)? = null, onOutput: ((String) -> Unit)? = null) {
        stopping = false
        
        // Track agent start
        trackUserAction("agent_start", "home", mapOf("instruction_length" to instruction.length))
        val agentTransaction = AgentErrorTracker.startAgentTransaction(instruction)
        val startTime = System.currentTimeMillis()
        
        agentJob = CoroutineScope(Dispatchers.Default).launch {
            var step = 0 // Move step variable outside try block
            try {
                LogManager.log(TAG, "Agent started with instruction: $instruction")
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
                LogManager.log(TAG, "Agent plan: $plan")
                if (plan != null) onOutput?.invoke("Plan: $plan")
                val assistantPlan = if (plan != null) mapOf("role" to "assistant", "content" to plan) else null
                
                // Initialize task context and screen analysis
                val taskContext = TaskContextManager.initializeTask(instruction, plan)
                var previousScreenAnalysis: ScreenAnalysis? = null
                var lastAction: String? = null
                // Start with initial message history
                val baseMessages = mutableListOf<Map<String, Any>>(systemPrompt, userInstruction)
                if (assistantPlan != null) baseMessages.add(assistantPlan)
                var messages = baseMessages.toMutableList()
                
                while (step < 10) {
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
                    TaskContextManager.advanceStep()

                    // Get and analyze current screen
                    var screenJson = AgentActions.getScreenJson()
                    var emptyTries = 0
                    while ((screenJson.trim() == "[]" || screenJson.trim().isEmpty()) && emptyTries < 5) {
                        LogManager.log(TAG, "Screen JSON is empty, waiting for app to load... (try ${emptyTries+1})")
                        onOutput?.invoke("Screen is empty, waiting for app to load... (try ${emptyTries+1})")
                        AgentActions.waitFor(2000) // Wait 2 seconds
                        screenJson = AgentActions.getScreenJson()
                        emptyTries++
                    }
                    
                    val currentScreenAnalysis = ScreenAnalyzer.analyzeScreen(screenJson)
                    
                    // Smart loop detection using both old and new methods
                    val loopAnalysis = LoopDetector.analyzeHistory(messages)
                    val isStuckInLoop = TaskContextManager.isStuckInLoop()
                    
                    if (loopAnalysis.isLooping || isStuckInLoop) {
                        LogManager.log(TAG, "Step $step: Loop detected - traditional: ${loopAnalysis.isLooping}, context-aware: $isStuckInLoop")
                        onOutput?.invoke("Loop detected - analyzing situation...")
                        
                        // Track loop detection
                        AgentErrorTracker.trackLoopDetection(
                            instruction = instruction,
                            loopSteps = listOf(step),
                            loopType = if (loopAnalysis.isLooping) "traditional" else "context_aware",
                            context = mapOf(
                                "step" to step,
                                "loop_analysis" to loopAnalysis.toString(),
                                "stuck_in_loop" to isStuckInLoop
                            )
                        )
                        
                        // Only check task completion if we've made reasonable progress
                        val context = TaskContextManager.getCurrentContext()
                        val hasSignificantProgress = context?.lastSignificantProgress ?: 0 >= 2
                        
                        if (hasSignificantProgress && TaskContextManager.shouldTaskComplete(currentScreenAnalysis, lastAction)) {
                            LogManager.log(TAG, "Step $step: Task appears complete despite loop detection")
                            onOutput?.invoke("Task appears to be complete - stopping")
                            return@launch
                        }
                        
                        // Add intelligent loop breaking prompt with screen context
                        val contextSummary = TaskContextManager.getContextSummary()
                        messages.add(mapOf("role" to "user", "content" to """${Prompts.loopBreakingPrompt}

Current situation analysis:
$contextSummary

Screen Analysis:
- Type: ${currentScreenAnalysis.screenType}
- Loading State: ${currentScreenAnalysis.loadingState}
- Available Elements: ${currentScreenAnalysis.interactableElements.take(5).map { it.displayText }}

You are stuck in a loop. Consider:
1. Are you trying to interact with the wrong element?
2. Is the screen in a different state than expected?
3. Should you try a different approach entirely?
4. Is the task actually complete?

${Prompts.loopBreakingDecisionFormat}"""))

                        // Enhanced stopping decision with context
                        val shouldStopDueToLoop = MetaCognition.shouldStop(messages, apiKey)
                        if (shouldStopDueToLoop) {
                            LogManager.log(TAG, "Step $step: Stopping due to unrecoverable loop")
                            onOutput?.invoke("Stopping due to unrecoverable loop - unable to make progress")
                            
                            // Track task completion failure
                            AgentErrorTracker.trackTaskCompletionError(
                                instruction = instruction,
                                totalSteps = step,
                                lastSuccessfulStep = step - 1,
                                reason = "unrecoverable_loop",
                                context = mapOf("loop_type" to "unrecoverable")
                            )
                            
                            agentTransaction.finish()
                            return@launch
                        }

                        onOutput?.invoke("⚠️ Warning: In a loop - attempting intelligent recovery...")
                    }

                    // Remove any previous screen JSON user message
                    messages = messages.filterNot { it["role"] == "user" && (it["content"] as? String)?.startsWith("Current screen JSON:") == true }.toMutableList()
                    // Remove any trailing tool messages before LLM call
                    while (messages.isNotEmpty() && messages.last()["role"] == "tool") {
                        messages.removeAt(messages.size - 1)
                    }
                    
                    // Advance step and record screen state
                    TaskContextManager.advanceStep()
                    
                    // Add enhanced screen information with analysis
                    val enhancedScreenInfo = """Current screen analysis:
Screen Type: ${currentScreenAnalysis.screenType}
Loading State: ${currentScreenAnalysis.loadingState}
Package: ${currentScreenAnalysis.packageName ?: "Unknown"}
High Priority Elements: ${currentScreenAnalysis.interactableElements.take(10).map { 
    "\"${it.displayText}\" (${it.className.substringAfterLast('.')}) at (${it.bounds.centerX}, ${it.bounds.centerY}) priority:${it.priority}"
}.joinToString(", ")}

Task Context: ${TaskContextManager.getContextSummary()}

Raw screen JSON: $screenJson"""
                    
                    messages.add(mapOf("role" to "user", "content" to enhancedScreenInfo))
                    
                    // Add tool call guidance
                    if (messages.none { it["role"] == "system" && (it["content"] as? String)?.contains("Always respond with a tool call") == true }) {
                        messages.add(0, mapOf("role" to "system", "content" to Prompts.toolCallGuidance))
                    }

                    LogManager.log(TAG, "Step $step: MESSAGES before LLM call: ${messages.map { it.toString() }}", LogLevel.DEBUG)
                    try {
                        if (stopping) {
                            LogManager.log(TAG, "Agent stopping requested before LLM call")
                            break
                        }
                        coroutineContext.ensureActive()
                        val llm = LLMClient(apiKey)
                        LogManager.log(TAG, "Step $step: Calling LLM with tools...")
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
                            LogManager.log(TAG, "Stopping agent due to LLM error.", LogLevel.ERROR)
                            onOutput?.invoke("LLM error: $errorMessage")
                            
                            // Track API error
                            AgentErrorTracker.trackTaskCompletionError(
                                instruction = instruction,
                                totalSteps = step,
                                lastSuccessfulStep = step - 1,
                                reason = "llm_api_error",
                                context = mapOf("error_message" to errorMessage)
                            )
                            
                            agentTransaction.finish()
                            break
                        }
                        val choices = response.optJSONArray("choices") ?: break
                        if (choices.length() == 0) break
                        val choice = choices.getJSONObject(0)
                        val message = choice.getJSONObject("message")
                        val toolCalls = message.optJSONArray("tool_calls")
                        val content = message.optString("content")
                        if (toolCalls != null && toolCalls.length() > 0) {
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
                            
                            // For each tool call, perform the action and add a tool message
                            for (i in 0 until toolCalls.length()) {
                                val toolCall = toolCalls.getJSONObject(i)
                                val function = toolCall.getJSONObject("function")
                                val name = function.getString("name")
                                val arguments = JSONObject(function.getString("arguments"))
                                
                                // Execute the tool call and get the result
                                val toolResult = sentryAgentOperation(
                                    operation = name,
                                    step = step,
                                    instruction = instruction
                                ) {
                                    when (name) {
                                        "simulate_press" -> {
                                            val centerX = arguments.getInt("center_x")
                                            val centerY = arguments.getInt("center_y")
                                            LogManager.log(TAG, "Step $step: Simulating press at center ($centerX, $centerY)")
                                            Handler(Looper.getMainLooper()).post {
                                                AgentActions.simulatePressAt(centerX, centerY)
                                            }
                                            val actionDescription = "Pressed at center ($centerX, $centerY)"
                                        val elementPressed = currentScreenAnalysis.interactableElements.find { 
                                            kotlin.math.abs(it.bounds.centerX - centerX) < 50 && kotlin.math.abs(it.bounds.centerY - centerY) < 50 
                                        }?.displayText
                                        lastAction = actionDescription
                                        onOutput?.invoke("Simulated press at ($centerX, $centerY)")
                                        
                                        // Record action in context manager
                                        TaskContextManager.recordAction(
                                            action = actionDescription,
                                            element = elementPressed,
                                            result = "Press executed",
                                            previousScreenAnalysis = previousScreenAnalysis,
                                            currentScreenAnalysis = currentScreenAnalysis
                                        )
                                        
                                        actionDescription
                                    }
                                    "set_text" -> {
                                        val x = arguments.getInt("x")
                                        val y = arguments.getInt("y")
                                        val text = arguments.getString("text")
                                        LogManager.log(TAG, "Step $step: Setting text at ($x, $y): $text")
                                        Handler(Looper.getMainLooper()).post {
                                            AgentActions.setTextAt(x, y, text)
                                        }
                                        val actionDescription = "Set text at ($x, $y): $text"
                                        val textField = currentScreenAnalysis.textInputs.find { 
                                            kotlin.math.abs(it.bounds.centerX - x) < 50 && kotlin.math.abs(it.bounds.centerY - y) < 50 
                                        }?.displayText
                                        lastAction = actionDescription
                                        onOutput?.invoke("Set text at ($x, $y): $text")
                                        
                                        // Record action in context manager
                                        TaskContextManager.recordAction(
                                            action = actionDescription,
                                            element = textField,
                                            result = "Text input executed",
                                            previousScreenAnalysis = previousScreenAnalysis,
                                            currentScreenAnalysis = currentScreenAnalysis
                                        )
                                        
                                        actionDescription
                                    }
                                    "wait_for" -> {
                                        val duration = arguments.getLong("duration_ms")
                                        LogManager.log(TAG, "Step $step: Waiting for $duration ms")
                                        AgentActions.waitFor(duration)
                                        lastAction = "Waited for $duration ms"
                                        onOutput?.invoke("Waited for $duration ms")
                                        "Waited for $duration ms"
                                    }
                                    "wait_for_element" -> {
                                        val text = if (arguments.has("text")) arguments.optString("text", null) else null
                                        val contentDescription = if (arguments.has("contentDescription")) arguments.optString("contentDescription", null) else null
                                        val className = if (arguments.has("className")) arguments.optString("className", null) else null
                                        val timeout = if (arguments.has("timeout_ms")) arguments.getLong("timeout_ms") else 5000L
                                        LogManager.log(TAG, "Step $step: Waiting for element (text=$text, contentDescription=$contentDescription, className=$className) up to $timeout ms")
                                        val found = AgentActions.waitForElement(text, contentDescription, className, timeout)
                                        lastAction = if (found) "Element appeared" else "Element not found in $timeout ms"
                                        onOutput?.invoke(lastAction!!)
                                        lastAction!!
                                    }
                                    "go_home" -> {
                                        LogManager.log(TAG, "Step $step: Going home")
                                        Handler(Looper.getMainLooper()).post {
                                            AgentActions.goHome()
                                        }
                                        lastAction = "Went home"
                                        onOutput?.invoke("Went home")
                                        "Went home"
                                    }
                                    "go_back" -> {
                                        LogManager.log(TAG, "Step $step: Going back")
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
                                        LogManager.log(TAG, "Step $step: Swiping from ($startX, $startY) to ($endX, $endY) duration $duration ms")
                                        Handler(Looper.getMainLooper()).post {
                                            AgentActions.swipe(startX, startY, endX, endY, duration)
                                        }
                                        lastAction = "Swiped from ($startX, $startY) to ($endX, $endY)"
                                        onOutput?.invoke("Swiped from ($startX, $startY) to ($endX, $endY)")
                                        "Swiped from ($startX, $startY) to ($endX, $endY)"
                                    }
                                        else -> {
                                            LogManager.log(TAG, "Step $step: Unknown tool call: $name")
                                            lastAction = "Unknown tool call: $name"
                                            onOutput?.invoke("Unknown tool call: $name")
                                            "Unknown tool call: $name"
                                        }
                                    }
                                } ?: "Tool execution failed"

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
                                
                                // Update previous screen analysis for next iteration
                                previousScreenAnalysis = currentScreenAnalysis

                                // Do reflection immediately after each action
                                val reflection = MetaCognition.reflectOnStep(messages, apiKey)
                                LogManager.log(TAG, "Step $step: Reflection after action: $reflection")
                                if (reflection != null) {
                                    onOutput?.invoke("Reflection: $reflection")
                                    messages.add(mapOf("role" to "assistant", "content" to reflection))
                                }

                                // Check if we should stop after each action
                                val shouldStop = MetaCognition.shouldStop(messages, apiKey)
                                LogManager.log(TAG, "Step $step: Should stop after action? $shouldStop")
                                onOutput?.invoke("Should stop? $shouldStop")
                                if (shouldStop) {
                                    LogManager.log(TAG, "Step $step: Metacognition decided to stop.")
                                    onOutput?.invoke("Agent decided to stop.")
                                    
                                    // Track successful task completion
                                    val executionTime = System.currentTimeMillis() - startTime
                                    AgentErrorTracker.trackTaskSuccess(
                                        instruction = instruction,
                                        totalSteps = step,
                                        executionTimeMs = executionTime,
                                        context = mapOf("completion_method" to "metacognition_stop")
                                    )
                                    
                                    agentTransaction.finish()
                                    return@launch
                                }
                            }
                            continue
                        } else if (content.isNotBlank()) {
                            LogManager.log(TAG, "Step $step: No tool calls, but got content: $content. Adding to messages and continuing.")
                            messages.add(mapOf("role" to "assistant", "content" to content))
                            onOutput?.invoke(content)
                            delay(200)
                            coroutineContext.ensureActive()
                            continue
                        } else {
                            LogManager.log(TAG, "Step $step: No tool calls and no content. Agent done.")
                            onOutput?.invoke("Agent done.")
                            break
                        }
                    } catch (e: Exception) {
                        LogManager.log(TAG, "Error in agent loop: ${e.message}", LogLevel.ERROR)
                        onOutput?.invoke("Error: ${e.message}")
                        
                        // Track agent execution error
                        AgentErrorTracker.trackAgentError(
                            error = e,
                            agentStep = step,
                            instruction = instruction,
                            context = mapOf(
                                "error_location" to "agent_main_loop",
                                "last_action" to (lastAction ?: "none")
                            )
                        )
                        
                        agentTransaction.throwable = e
                        agentTransaction.finish()
                        break
                    }
                    delay(300)
                    coroutineContext.ensureActive()
                }
            } finally {
                LogManager.log(TAG, "Agent stopped")
                onOutput?.invoke("Agent stopped")
                
                // Track agent stop - step is accessible here since it's defined in the launch scope
                trackUserAction("agent_stop", "home", mapOf(
                    "total_steps" to step,
                    "execution_time_ms" to (System.currentTimeMillis() - startTime)
                ))
                
                // Ensure transaction is finished
                try {
                    if (!agentTransaction.isFinished) {
                        agentTransaction.finish()
                    }
                } catch (e: Exception) {
                    // Ignore transaction finish errors
                }
                
                onAgentStopped?.invoke()
            }
        }
    }

    fun stopAgent() {
        LogManager.log(TAG, "Stopping agent...")
        stopping = true
        agentJob?.cancel()
        agentJob = null
        paused = false
    }

    fun pauseAgent() {
        LogManager.log(TAG, "Agent paused")
        paused = true
    }

    fun resumeAgent() {
        LogManager.log(TAG, "Agent resumed")
        paused = false
    }

    fun isPaused(): Boolean = paused
} 