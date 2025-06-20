package com.example.simple_agent_android.notification

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.ui.components.VoiceInputButton
import com.example.simple_agent_android.ui.components.VoiceButtonSize
import com.example.simple_agent_android.ui.theme.*
import com.example.simple_agent_android.utils.SharedPrefsUtils
import com.example.simple_agent_android.utils.VoiceInputManager
import com.example.simple_agent_android.utils.VoiceInputState

class VoiceInputActivity : ComponentActivity() {
    private val TAG = "VoiceInputActivity"
    private var voiceInputManager: VoiceInputManager? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "VoiceInputActivity created")
        
        // Set window flags for better user experience
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        // Check if voice recognition is available
        if (!VoiceInputManager.isVoiceRecognitionAvailable(this)) {
            Log.w(TAG, "Voice recognition not available on this device")
            NotificationHelper.showToast(this, "Voice recognition not available")
            finish()
            return
        }
        
        voiceInputManager = VoiceInputManager(this)
        Log.d(TAG, "VoiceInputManager created, available: ${voiceInputManager?.isAvailable()}")
        
        setContent {
            SimpleAgentAndroidTheme {
                VoiceInputDialog(
                    voiceInputManager = voiceInputManager,
                    onDismiss = { 
                        Log.d(TAG, "Dialog dismissed")
                        finish() 
                    },
                    onStartAgent = { instruction ->
                        Log.d(TAG, "Starting agent with instruction: $instruction")
                        startAgentWithInstruction(instruction)
                        finish()
                    }
                )
            }
        }
    }
    
    private fun startAgentWithInstruction(instruction: String) {
        try {
            Log.d(TAG, "Attempting to start agent with: $instruction")
            val apiKey = SharedPrefsUtils.getOpenAIKey(this)
            if (apiKey.isBlank()) {
                Log.w(TAG, "No API key configured")
                NotificationHelper.showToast(this, "Please configure OpenAI API key in settings")
                return
            }
            
            Log.d(TAG, "API key found, starting agent...")
            AgentStateManager.startAgent(
                instruction = instruction,
                apiKey = apiKey,
                appContext = this,
                onOutput = { output ->
                    Log.d(TAG, "Agent output: $output")
                }
            )
            
            NotificationHelper.showToast(this, "Agent started: $instruction")
            Log.d(TAG, "Agent started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting agent", e)
            NotificationHelper.showToast(this, "Failed to start agent: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VoiceInputActivity destroyed")
        voiceInputManager?.cleanup()
    }
}

@Composable
fun VoiceInputDialog(
    voiceInputManager: VoiceInputManager?,
    onDismiss: () -> Unit,
    onStartAgent: (String) -> Unit
) {
    var voiceInputState by remember { mutableStateOf(VoiceInputState.IDLE) }
    var voiceTranscription by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }
    
    // Start voice input automatically when dialog opens
    LaunchedEffect(Unit) {
        Log.d("VoiceInputDialog", "LaunchedEffect triggered")
        if (voiceInputManager != null && voiceInputManager.isAvailable()) {
            Log.d("VoiceInputDialog", "Voice manager available, starting voice input...")
            kotlinx.coroutines.delay(500) // Increased delay for UI to settle
            try {
                voiceInputManager.startListening(
                    onComplete = { transcription ->
                        Log.d("VoiceInputDialog", "Voice input completed: $transcription")
                        voiceTranscription = transcription
                        voiceInputState = VoiceInputState.COMPLETED
                    },
                    onError = { error ->
                        Log.e("VoiceInputDialog", "Voice input error: $error")
                        voiceTranscription = "Error: $error"
                        voiceInputState = VoiceInputState.ERROR
                    },
                    onStateChange = { state ->
                        Log.d("VoiceInputDialog", "Voice state changed to: $state")
                        voiceInputState = state
                        when (state) {
                            VoiceInputState.LISTENING -> voiceTranscription = "Listening..."
                            VoiceInputState.PROCESSING -> voiceTranscription = "Processing..."
                            else -> {}
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("VoiceInputDialog", "Exception starting voice input", e)
                voiceTranscription = "Error: ${e.message}"
                voiceInputState = VoiceInputState.ERROR
            }
        } else {
            Log.w("VoiceInputDialog", "Voice manager not available, showing manual input")
            showManualInput = true
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Voice Input",
                    color = ReagentGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!showManualInput) {
                    VoiceInputButton(
                        voiceInputState = voiceInputState,
                        voiceTranscription = voiceTranscription,
                        voiceInputAvailable = voiceInputManager?.isAvailable() ?: false,
                        onStartVoiceInput = {
                            Log.d("VoiceInputDialog", "Manual voice input start requested")
                            voiceInputManager?.startListening(
                                onComplete = { transcription ->
                                    Log.d("VoiceInputDialog", "Manual voice input completed: $transcription")
                                    voiceTranscription = transcription
                                    voiceInputState = VoiceInputState.COMPLETED
                                },
                                onError = { error ->
                                    Log.e("VoiceInputDialog", "Manual voice input error: $error")
                                    voiceTranscription = "Error: $error"
                                    voiceInputState = VoiceInputState.ERROR
                                },
                                onStateChange = { state ->
                                    Log.d("VoiceInputDialog", "Manual voice state changed to: $state")
                                    voiceInputState = state
                                    when (state) {
                                        VoiceInputState.LISTENING -> voiceTranscription = "Listening..."
                                        VoiceInputState.PROCESSING -> voiceTranscription = "Processing..."
                                        else -> {}
                                    }
                                }
                            )
                        },
                        onStopVoiceInput = {
                            Log.d("VoiceInputDialog", "Voice input stop requested")
                            voiceInputManager?.stopListening()
                            voiceInputState = VoiceInputState.IDLE
                        },
                        onCancelVoiceInput = {
                            Log.d("VoiceInputDialog", "Voice input cancel requested")
                            voiceInputManager?.cancelListening()
                            voiceInputState = VoiceInputState.IDLE
                            voiceTranscription = ""
                        },
                        size = VoiceButtonSize.LARGE,
                        showTranscription = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (voiceInputState == VoiceInputState.COMPLETED && voiceTranscription.isNotBlank() && !voiceTranscription.startsWith("Error:")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ReagentGray)
                            ) {
                                Text("Cancel", color = ReagentBlack)
                            }
                            
                            Button(
                                onClick = { 
                                    Log.d("VoiceInputDialog", "Start agent button clicked with: $voiceTranscription")
                                    onStartAgent(voiceTranscription) 
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen)
                            ) {
                                Text("Start Agent", color = ReagentBlack)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { 
                            Log.d("VoiceInputDialog", "Switching to manual input")
                            showManualInput = true 
                        }
                    ) {
                        Text("Type instead", color = ReagentBlue)
                    }
                } else {
                    // Manual input fallback
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = { Text("Agent Instruction") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ReagentGreen,
                            unfocusedBorderColor = ReagentGray,
                            cursorColor = ReagentGreen
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentGray)
                        ) {
                            Text("Cancel", color = ReagentBlack)
                        }
                        
                        Button(
                            onClick = { 
                                if (manualInput.isNotBlank()) {
                                    Log.d("VoiceInputDialog", "Manual start agent with: $manualInput")
                                    onStartAgent(manualInput)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                            enabled = manualInput.isNotBlank()
                        ) {
                            Text("Start Agent", color = ReagentBlack)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { 
                            Log.d("VoiceInputDialog", "Switching back to voice input")
                            showManualInput = false 
                        }
                    ) {
                        Text("Use voice instead", color = ReagentBlue)
                    }
                }
            }
        }
    }
} 