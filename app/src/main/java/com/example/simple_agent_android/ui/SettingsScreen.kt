package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simple_agent_android.ui.theme.*

@Composable
fun SettingsScreen(
    openAiKey: String,
    onOpenAiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    saved: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Settings",
                    color = ReagentGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = onOpenAiKeyChange,
                    label = { Text("OpenAI API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentGreen,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentGreen
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = ReagentBlack, fontWeight = FontWeight.Bold)
                }
                if (saved) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saved!", color = ReagentGreen)
                }
            }
        }
    }
} 