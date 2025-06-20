package com.example.simple_agent_android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.simple_agent_android.utils.LogManager

@Composable
fun DebugScreen(
    showBoxes: Boolean,
    onToggleBoxes: () -> Unit,
    overlayActive: Boolean,
    onToggleOverlay: () -> Unit,
    verticalOffset: Int,
    onVerticalOffsetChange: (Int) -> Unit,
    onExportJson: () -> Unit,
    jsonOutput: String?,
    onCloseJson: () -> Unit
) {
    val context = LocalContext.current
    val logText = remember { mutableStateOf(LogManager.getFullLog()) }
    val scrollState = rememberScrollState()

    // Update log text every second
    LaunchedEffect(Unit) {
        while (true) {
            logText.value = LogManager.getFullLog()
            kotlinx.coroutines.delay(1000)
        }
    }

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
            text = "Debug Tools",
            style = MaterialTheme.typography.headlineMedium,
            color = ReagentWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Development & Testing Controls",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Overlay Controls Card
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
                        imageVector = if (overlayActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Overlay Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Button(
                    onClick = onToggleOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (overlayActive) ReagentStatusOffline else ReagentBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (overlayActive) "Hide Overlay" else "Show Overlay",
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Vertical Offset",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReagentWhite,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$verticalOffset px",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Slider(
                    value = verticalOffset.toFloat(),
                    onValueChange = { onVerticalOffsetChange(it.toInt()) },
                    valueRange = -100f..100f,
                    steps = 200,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = ReagentBlue,
                        activeTrackColor = ReagentBlue,
                        inactiveTrackColor = ReagentGray.copy(alpha = 0.3f)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Data Export Card
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
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Data Export",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Button(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Export Interactive Elements",
                        color = ReagentBlack,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // JSON Output Dialog
        if (jsonOutput != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ReagentDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Interactive Elements JSON",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = jsonOutput,
                            color = ReagentGray,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCloseJson,
                        colors = ButtonDefaults.buttonColors(containerColor = ReagentStatusOffline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = ReagentWhite, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        // System Logs Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ReagentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "System Logs",
                            style = MaterialTheme.typography.titleMedium,
                            color = ReagentWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { LogManager.clearLog() },
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentStatusOffline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear", color = ReagentWhite, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Agent Logs", logText.value)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy", color = ReagentWhite, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (logText.value.isBlank()) "No logs available" else logText.value,
                            color = if (logText.value.isBlank()) ReagentGray.copy(alpha = 0.6f) else ReagentGray,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
} 