package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.simple_agent_android.R
import com.example.simple_agent_android.ui.theme.*
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onEnableAccessibility,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Enable Accessibility Service", color = ReagentWhite)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (agentRunning) ReagentStatusOnline else ReagentStatusIdle,
                                shape = RoundedCornerShape(7.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (agentRunning) R.string.agent_status_running else R.string.agent_status_stopped),
                        color = if (agentRunning) ReagentGreen else ReagentGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = agentInput,
                    onValueChange = onAgentInputChange,
                    label = { Text("Agent Instruction") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentGreen,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentGreen
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                FuturisticAgentButton(
                    running = agentRunning,
                    onStart = onStartAgent,
                    onStop = onStopAgent
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = ReagentDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = agentOutput,
                            color = ReagentGreen,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuturisticAgentButton(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val buttonColor = if (running) ReagentStatusOffline else ReagentGreen
    val buttonText = if (running) stringResource(id = R.string.stop_agent) else stringResource(id = R.string.start_agent)
    val onClick = if (running) onStop else onStart
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = buttonText,
            color = ReagentBlack,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
} 