package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun SettingsScreen(
    openAiKey: String,
    onOpenAiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    saved: Boolean,
    onCheckForUpdates: () -> Unit,
    onRedoOnboarding: () -> Unit = {},
    completionScreenEnabled: Boolean = true,
    onCompletionScreenToggle: (Boolean) -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = ReagentWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Configure your AI agent settings",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // API Configuration Card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "API Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Enter your OpenAI API key to enable agent functionality",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = onOpenAiKeyChange,
                    label = { 
                        Text(
                            "OpenAI API Key",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    placeholder = {
                        Text(
                            "sk-...",
                            color = ReagentGray.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide API Key" else "Show API Key",
                                tint = ReagentGreen
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentGreen,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentGreen,
                        focusedLabelColor = ReagentGreen,
                        unfocusedLabelColor = ReagentGray
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Save Configuration", 
                        color = ReagentBlack, 
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (saved) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ReagentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Configuration saved successfully!",
                            color = ReagentGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // User Experience Card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "User Experience",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Customize how the agent interacts with you",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Completion Screen Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Task Completion Screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ReagentWhite,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Show suggestions when tasks complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = ReagentGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Switch(
                        checked = completionScreenEnabled,
                        onCheckedChange = onCompletionScreenToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ReagentWhite,
                            checkedTrackColor = ReagentGreen,
                            uncheckedThumbColor = ReagentGray,
                            uncheckedTrackColor = ReagentGray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // App Management Card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "App Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Keep your app up to date and manage your setup experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = onCheckForUpdates,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Check for Updates", 
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onRedoOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ReagentBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "App Onboarding", 
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Information Card
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ReagentGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "About API Keys",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Your API key is stored locally on your device and is never transmitted to our servers. It's only used to communicate directly with OpenAI's services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "To get an API key, visit platform.openai.com and create an account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
} 