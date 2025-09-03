package com.example.simple_agent_android.agentcore

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.simple_agent_android.utils.NotificationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgentStateManager {
    private val _agentRunning = mutableStateOf(false)
    val agentRunning: State<Boolean> = _agentRunning
    
    private val _agentRunningFlow = MutableStateFlow(false)
    val agentRunningFlow: StateFlow<Boolean> = _agentRunningFlow.asStateFlow()
    
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    fun isAgentRunning(): Boolean = _agentRunning.value

    fun startAgent(instruction: String, apiKey: String, appContext: Context, baseUrl: String = "https://api.openai.com", onOutput: ((String) -> Unit)? = null) {
        if (_agentRunning.value) {
            // Agent is already running, don't start again
            return
        }
        
        context = appContext.applicationContext
        _agentRunning.value = true
        _agentRunningFlow.value = true
        NotificationUtils.showAgentStartedNotification(appContext)
        
        AgentOrchestrator.runAgent(
            instruction = instruction,
            apiKey = apiKey,
            context = appContext,
            baseUrl = baseUrl,
            onAgentStopped = {
                stopAgent()
            },
            onOutput = onOutput
        )
    }

    fun stopAgent() {
        if (!_agentRunning.value) {
            // Agent is already stopped
            return
        }
        
        AgentOrchestrator.stopAgent()
        _agentRunning.value = false
        _agentRunningFlow.value = false
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