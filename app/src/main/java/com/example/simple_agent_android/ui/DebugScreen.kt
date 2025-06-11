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
import androidx.compose.ui.unit.dp
import com.example.simple_agent_android.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
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
                            .background(ReagentGreen, shape = RoundedCornerShape(7.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Debug Tools",
                        color = ReagentGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onToggleOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (overlayActive) ReagentStatusOffline else ReagentGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (overlayActive) "Hide Overlay" else "Show Overlay", color = ReagentBlack)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Vertical Offset: $verticalOffset px", color = ReagentGreen)
                Slider(
                    value = verticalOffset.toFloat(),
                    onValueChange = { onVerticalOffsetChange(it.toInt()) },
                    valueRange = -100f..100f,
                    steps = 200,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = ReagentGreen,
                        activeTrackColor = ReagentBlue,
                        inactiveTrackColor = ReagentGray
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Export Interactive Elements as JSON", color = ReagentWhite)
                }
            }
        }
        if (jsonOutput != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ReagentDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Interactive Elements JSON", color = ReagentGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = jsonOutput, color = ReagentGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCloseJson, colors = ButtonDefaults.buttonColors(containerColor = ReagentStatusOffline)) {
                        Text("Close", color = ReagentWhite)
                    }
                }
            }
        }
    }
} 