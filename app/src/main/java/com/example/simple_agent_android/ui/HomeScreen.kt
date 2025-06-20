package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.R
import com.example.simple_agent_android.ui.theme.*
import com.example.simple_agent_android.ui.components.VoiceInputRow
import com.example.simple_agent_android.utils.VoiceInputState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun HomeScreen(
    agentRunning: Boolean,
    onStartAgent: () -> Unit,
    onStopAgent: () -> Unit,
    agentInput: String,
    onAgentInputChange: (String) -> Unit,
    onEnableAccessibility: () -> Unit,
    agentOutput: String,
    floatingUiEnabled: Boolean,
    onToggleFloatingUi: () -> Unit,
    // Voice Input Parameters
    voiceInputState: VoiceInputState,
    voiceTranscription: String,
    voiceInputAvailable: Boolean,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    // Status Parameters
    accessibilityServiceEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header Section
        Text(
            text = "Simple Agent",
            style = MaterialTheme.typography.headlineMedium,
            color = ReagentWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "AI-Powered Android Automation",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Accessibility Service Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (accessibilityServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (accessibilityServiceEnabled) ReagentGreen else ReagentStatusOffline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Service",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReagentWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (accessibilityServiceEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (accessibilityServiceEnabled) ReagentGreen else ReagentGray
                        )
                    }
                }
                
                if (!accessibilityServiceEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onEnableAccessibility,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Enable Accessibility Service",
                            color = ReagentWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Quick Actions Section
        if (accessibilityServiceEnabled) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = ReagentWhite,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            
            Button(
                onClick = onToggleFloatingUi,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (floatingUiEnabled) ReagentGreen else ReagentBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (floatingUiEnabled) "Disable Floating UI" else "Enable Floating UI",
                    color = ReagentWhite,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // Agent Control Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Agent Status Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (agentRunning) ReagentStatusOnline else ReagentStatusIdle,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(if (agentRunning) R.string.agent_status_running else R.string.agent_status_stopped),
                        color = if (agentRunning) ReagentGreen else ReagentGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                // Input Section
                OutlinedTextField(
                    value = agentInput,
                    onValueChange = onAgentInputChange,
                    label = { 
                        Text(
                            "What would you like the agent to do?",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentGreen,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentGreen,
                        focusedLabelColor = ReagentGreen,
                        unfocusedLabelColor = ReagentGray
                    ),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Voice Input Section
                VoiceInputRow(
                    voiceInputState = voiceInputState,
                    voiceTranscription = voiceTranscription,
                    voiceInputAvailable = voiceInputAvailable,
                    onStartVoiceInput = onStartVoiceInput,
                    onStopVoiceInput = onStopVoiceInput,
                    onCancelVoiceInput = onCancelVoiceInput,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Agent Control Button
                FuturisticAgentButton(
                    running = agentRunning,
                    onStart = onStartAgent,
                    onStop = onStopAgent,
                    enabled = accessibilityServiceEnabled && agentInput.isNotBlank()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Output Section
                if (agentOutput.isNotBlank()) {
                    Text(
                        text = "Agent Output",
                        style = MaterialTheme.typography.titleSmall,
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Text(
                                text = agentOutput,
                                color = ReagentGreen,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun FuturisticAgentButton(
    running: Boolean, 
    onStart: () -> Unit, 
    onStop: () -> Unit,
    enabled: Boolean = true
) {
    val buttonColor = when {
        !enabled -> ReagentGray
        running -> ReagentStatusOffline
        else -> ReagentGreen
    }
    val buttonText = if (running) stringResource(id = R.string.stop_agent) else stringResource(id = R.string.start_agent)
    val onClick = if (running) onStop else onStart
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 4.dp else 0.dp, RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = ReagentGray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = buttonText,
            color = if (enabled) ReagentBlack else ReagentGray,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp
        )
    }
} 