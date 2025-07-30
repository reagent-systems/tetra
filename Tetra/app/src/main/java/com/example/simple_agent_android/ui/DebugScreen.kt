package com.example.simple_agent_android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Article
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
    debugCursorEnabled: Boolean,
    onToggleDebugCursor: () -> Unit,
    onExportJson: () -> Unit,
    jsonOutput: String?,
    onCloseJson: () -> Unit
) {
    val context = LocalContext.current
    val logLineCount = remember { mutableStateOf(0) }

    // Update log line count every 2 seconds (less frequent to reduce lag)
    LaunchedEffect(Unit) {
        while (true) {
            val fullLog = LogManager.getFullLog()
            logLineCount.value = if (fullLog.isBlank()) 0 else fullLog.lines().size
            kotlinx.coroutines.delay(2000)
        }
    }

    fun exportLogsToFile() {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "simple_agent_logs_$timestamp.txt"
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            val logContent = LogManager.getFullLog()
            FileWriter(file).use { writer ->
                writer.write("Simple Agent Debug Logs\n")
                writer.write("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("=".repeat(50) + "\n\n")
                writer.write(logContent)
            }
            
            Toast.makeText(context, "Logs exported to Downloads/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_LONG).show()
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Debug Cursor Toggle
                Button(
                    onClick = onToggleDebugCursor,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (debugCursorEnabled) ReagentStatusOnline else ReagentGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (debugCursorEnabled) "Hide Debug Cursor" else "Show Debug Cursor",
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
                    valueRange = -200f..200f,
                    steps = 400,
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

        // Test Controls Card
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
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = ReagentStatusOffline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Test Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Button(
                    onClick = { 
                        try {
                            com.example.simple_agent_android.accessibility.service.BoundingBoxAccessibilityService.testCompletionScreen()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentStatusOffline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Test Completion Screen",
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Logs Card
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
                        imageVector = Icons.Default.Article,
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
                
                // Log Statistics
                Card(
                            modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ReagentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log entries: ${logLineCount.value}",
                            color = ReagentWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Button(
                            onClick = { LogManager.clearLog() },
                        modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentStatusOffline),
                        shape = RoundedCornerShape(12.dp)
                        ) {
                        Text("Clear Logs", color = ReagentWhite, fontWeight = FontWeight.Medium)
                        }
                    
                        Button(
                        onClick = { exportLogsToFile() },
                        modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                        shape = RoundedCornerShape(12.dp)
                        ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = ReagentWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = ReagentWhite, fontWeight = FontWeight.Medium)
                        }
                    }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                    Text(
                    text = "Logs are stored in memory and exported to Downloads folder when needed. This improves performance by avoiding UI lag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
} 