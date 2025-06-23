package com.example.simple_agent_android.accessibility.floating

import android.content.Context
import android.util.Log
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.ui.floating.FloatingAgentButton
import com.example.simple_agent_android.ui.floating.FloatingCompletionScreen
import com.example.simple_agent_android.utils.SharedPrefsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FloatingButtonManager(private val context: Context) {
    private val TAG = "FloatingButtonManager"
    private var floatingAgentButton: FloatingAgentButton? = null
    private var floatingCompletionScreen: FloatingCompletionScreen? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private var wasRunning = false

    fun showFloatingButton() {
        try {
            Log.d(TAG, "Showing floating button")
            if (floatingAgentButton == null) {
                Log.d(TAG, "Creating new floating button")
                floatingAgentButton = FloatingAgentButton(context).apply {
                    setOnStartAgentListener { instruction ->
                        Log.d(TAG, "Starting agent with instruction: $instruction")
                        AgentStateManager.startAgent(
                            instruction = instruction,
                            apiKey = context.getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)
                                .getString("openai_key", "") ?: "",
                            appContext = context,
                            onOutput = { output ->
                                Log.d(TAG, "Agent output: $output")
                            }
                        )
                    }
                    setOnPauseAgentListener {
                        val currentlyPaused = AgentStateManager.isPaused()
                        if (currentlyPaused) {
                            AgentStateManager.resumeAgent()
                            floatingAgentButton?.setPaused(false)
                        } else {
                            AgentStateManager.pauseAgent()
                            floatingAgentButton?.setPaused(true)
                        }
                    }
                    setOnStopAgentListener {
                        AgentStateManager.stopAgent()
                    }
                    show()
                }
                
                // Start observing agent state changes
                startObservingAgentState()
                
                // Initialize completion screen
                initializeCompletionScreen()
                
                Log.d(TAG, "Floating button created and shown")
            } else {
                Log.d(TAG, "Floating button already exists")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button", e)
        }
    }

    fun hideFloatingButton() {
        try {
            Log.d(TAG, "Removing floating button")
            stopObservingAgentState()
            floatingAgentButton?.hide()
            floatingAgentButton = null
            floatingCompletionScreen?.dismiss()
            floatingCompletionScreen = null
            Log.d(TAG, "Floating button removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating button", e)
        }
    }

    private fun startObservingAgentState() {
        stateObserverJob?.cancel()
        stateObserverJob = AgentStateManager.agentRunningFlow
            .onEach { isRunning ->
                Log.d(TAG, "Agent state changed: isRunning = $isRunning, wasRunning = $wasRunning")
                floatingAgentButton?.let { button ->
                    if (isRunning) {
                        // Agent started - switch to control state
                        Log.d(TAG, "Agent started - switching to control state")
                        button.switchToControlState()
                        wasRunning = true
                    } else {
                        // Agent stopped - switch back to start state
                        Log.d(TAG, "Agent stopped - switching to start state")
                        button.switchToStartState()
                        
                        // Show completion screen if agent was running and setting is enabled
                        val completionEnabled = SharedPrefsUtils.isCompletionScreenEnabled(context)
                        Log.d(TAG, "Checking completion screen: wasRunning=$wasRunning, completionEnabled=$completionEnabled")
                        
                        if (wasRunning && completionEnabled) {
                            Log.d(TAG, "Conditions met - showing completion screen")
                            showCompletionScreen()
                        } else {
                            Log.d(TAG, "Conditions not met - not showing completion screen")
                        }
                        wasRunning = false
                    }
                }
            }
            .launchIn(scope)
    }

    private fun stopObservingAgentState() {
        stateObserverJob?.cancel()
        stateObserverJob = null
    }

    fun cleanup() {
        hideFloatingButton()
    }
    
    private fun initializeCompletionScreen() {
        floatingCompletionScreen = FloatingCompletionScreen(context).apply {
            setOnNewTaskListener { instruction ->
                Log.d(TAG, "Starting new task from completion screen: $instruction")
                AgentStateManager.startAgent(
                    instruction = instruction,
                    apiKey = context.getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)
                        .getString("openai_key", "") ?: "",
                    appContext = context,
                    onOutput = { output ->
                        Log.d(TAG, "Agent output: $output")
                    }
                )
            }
            setOnDismissListener {
                Log.d(TAG, "Completion screen dismissed")
            }
        }
    }
    
    private fun showCompletionScreen() {
        try {
            Log.d(TAG, "Showing completion screen")
            floatingCompletionScreen?.show("Task completed successfully! What would you like to do next?")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing completion screen", e)
        }
    }
    
    // Test method to manually show completion screen
    fun testShowCompletionScreen() {
        Log.d(TAG, "TEST: Manually showing completion screen")
        initializeCompletionScreen()
        showCompletionScreen()
    }
} 