package com.example.simple_agent_android

import android.app.Application
import com.example.simple_agent_android.agentcore.metacognition.Prompts

class SimpleAgentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prompts.initialize(this)
    }
} 