package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.ui.theme.*

@Composable
fun AboutScreen() {
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
            text = "About Simple Agent",
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
        
        // Main Description Card
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
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "What is Simple Agent?",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Simple Agent is designed with the belief that AI agents don't need to be complex to be useful. By focusing on a small set of core operations and using function calling for all interactions, Simple Agent remains easy to understand, modify, and extend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReagentWhite,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "This Android version provides advanced features like dynamic tool loading, intelligent loop detection, and smart execution management - all running 100% on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReagentGray,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Features Card
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
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Key Features",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                FeatureItem(
                    title = "Accessibility Integration",
                    description = "Uses Android's accessibility service to interact with any app"
                )
                
                FeatureItem(
                    title = "Voice Control",
                    description = "Speak your commands naturally with built-in voice recognition"
                )
                
                FeatureItem(
                    title = "Smart Loop Detection",
                    description = "Prevents infinite loops with intelligent behavior analysis"
                )
                
                FeatureItem(
                    title = "Floating UI",
                    description = "Control the agent from anywhere with a floating interface"
                )
                
                FeatureItem(
                    title = "Persistent Notifications",
                    description = "Stay connected with notification-based controls"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Privacy & Security Card
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "• All processing happens locally on your device\n• Your API keys are stored securely and never shared\n• No data is transmitted to external servers\n• Open source and transparent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReagentWhite,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Version Info Card
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ReagentGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Version Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Simple Agent Android\nVersion 1.0.0\nBuild: f38083d\nBuilt with Jetpack Compose & Material Design 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReagentGray,
                    lineHeight = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentWhite,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "  $description",
            style = MaterialTheme.typography.bodySmall,
            color = ReagentGray,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
} 