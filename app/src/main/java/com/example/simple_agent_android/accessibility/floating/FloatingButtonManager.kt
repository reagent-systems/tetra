package com.example.simple_agent_android.accessibility.floating

import android.content.Context
import android.util.Log
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.ui.floating.FloatingAgentButton

class FloatingButtonManager(private val context: Context) {
    private val TAG = "FloatingButtonManager"
    private var floatingAgentButton: FloatingAgentButton? = null

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
            floatingAgentButton?.hide()
            floatingAgentButton = null
            Log.d(TAG, "Floating button removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating button", e)
        }
    }

    fun cleanup() {
        hideFloatingButton()
    }
} 