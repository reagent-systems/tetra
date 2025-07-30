package com.example.simple_agent_android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.utils.SharedPrefsUtils
import com.example.simple_agent_android.utils.VoiceInputManager

class NotificationActionReceiver : BroadcastReceiver() {
    private val TAG = "NotificationActionReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")
        
        when (intent.action) {
            AgentNotificationManager.ACTION_START_AGENT -> {
                handleStartAgent(context, intent)
            }
            AgentNotificationManager.ACTION_STOP_AGENT -> {
                handleStopAgent(context)
            }
            AgentNotificationManager.ACTION_VOICE_INPUT -> {
                handleVoiceInput(context)
            }
        }
    }
    
    private fun handleStartAgent(context: Context, intent: Intent) {
        try {
            // Get text input from RemoteInput
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            val instruction = remoteInput?.getCharSequence(AgentNotificationManager.KEY_TEXT_REPLY)?.toString()
            
            if (!instruction.isNullOrBlank()) {
                Log.d(TAG, "Starting agent with instruction: $instruction")
                
                val apiKey = SharedPrefsUtils.getOpenAIKey(context)
                if (apiKey.isBlank()) {
                    Log.w(TAG, "No API key configured")
                    NotificationHelper.showToast(context, "Please configure OpenAI API key in settings")
                    return
                }
                
                AgentStateManager.startAgent(
                    instruction = instruction,
                    apiKey = apiKey,
                    appContext = context,
                    onOutput = { output ->
                        Log.d(TAG, "Agent output: $output")
                        // Could show output in a notification or store it
                    }
                )
                
                NotificationHelper.showToast(context, "Agent started: $instruction")
            } else {
                Log.w(TAG, "No instruction provided")
                NotificationHelper.showToast(context, "Please provide an instruction")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting agent from notification", e)
            NotificationHelper.showToast(context, "Failed to start agent")
        }
    }
    
    private fun handleStopAgent(context: Context) {
        try {
            Log.d(TAG, "Stopping agent")
            AgentStateManager.stopAgent()
            NotificationHelper.showToast(context, "Agent stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping agent from notification", e)
            NotificationHelper.showToast(context, "Failed to stop agent")
        }
    }
    
    private fun handleVoiceInput(context: Context) {
        try {
            Log.d(TAG, "Starting voice input from notification")
            
            // Create a voice input activity intent
            val voiceIntent = Intent(context, VoiceInputActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(voiceIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice input from notification", e)
            NotificationHelper.showToast(context, "Failed to start voice input")
        }
    }
} 