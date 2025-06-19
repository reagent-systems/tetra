package com.example.simple_agent_android.agentcore

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.simple_agent_android.utils.NotificationUtils

object AgentStateManager {
    private var agentRunningState = mutableStateOf(false)
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    fun isAgentRunning(): Boolean = agentRunningState.value

    fun getAgentRunningState(): MutableState<Boolean> = agentRunningState

    fun startAgent(instruction: String, apiKey: String, appContext: Context, onOutput: ((String) -> Unit)? = null) {
        context = appContext.applicationContext
        agentRunningState.value = true
        NotificationUtils.showAgentStartedNotification(appContext)
        
        AgentOrchestrator.runAgent(
            instruction = instruction,
            apiKey = apiKey,
            context = appContext,
            onAgentStopped = {
                stopAgent()
            },
            onOutput = onOutput
        )
    }

    fun stopAgent() {
        AgentOrchestrator.stopAgent()
        agentRunningState.value = false
        context?.let { NotificationUtils.showAgentStoppedNotification(it) }
    }

    fun pauseAgent() {
        AgentOrchestrator.pauseAgent()
    }

    fun resumeAgent() {
        AgentOrchestrator.resumeAgent()
    }

    fun isPaused(): Boolean = AgentOrchestrator.isPaused()
} 