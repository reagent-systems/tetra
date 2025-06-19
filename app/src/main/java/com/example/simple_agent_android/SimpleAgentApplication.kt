package com.example.simple_agent_android

import android.app.Application
import com.example.simple_agent_android.agentcore.metacognition.Prompts
import com.example.simple_agent_android.utils.NotificationUtils
import com.example.simple_agent_android.agentcore.AgentStateManager

class SimpleAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prompts.initialize(this)
        NotificationUtils.createNotificationChannel(this)
        AgentStateManager.initialize(this)
    }
} 