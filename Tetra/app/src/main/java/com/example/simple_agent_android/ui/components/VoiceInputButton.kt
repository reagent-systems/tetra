package com.example.simple_agent_android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.ui.theme.*
import com.example.simple_agent_android.utils.VoiceInputState

@Composable
fun VoiceInputButton(
    voiceInputState: VoiceInputState,
    voiceTranscription: String,
    voiceInputAvailable: Boolean,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    modifier: Modifier = Modifier,
    size: VoiceButtonSize = VoiceButtonSize.MEDIUM,
    showTranscription: Boolean = true
) {
    val isListening = voiceInputState == VoiceInputState.LISTENING
    val isProcessing = voiceInputState == VoiceInputState.PROCESSING
    val isActive = isListening || isProcessing
    
    // Animation for pulsing effect when listening
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Voice Input Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size.buttonSize)
                .scale(if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    color = when {
                        !voiceInputAvailable -> ReagentGray
                        isListening -> ReagentGreen
                        isProcessing -> ReagentBlue
                        voiceInputState == VoiceInputState.ERROR -> Color.Red
                        else -> ReagentBlue
                    }
                )
                .clickable(enabled = voiceInputAvailable) {
                    when (voiceInputState) {
                        VoiceInputState.IDLE, VoiceInputState.COMPLETED, VoiceInputState.ERROR -> {
                            onStartVoiceInput()
                        }
                        VoiceInputState.LISTENING -> {
                            onStopVoiceInput()
                        }
                        VoiceInputState.PROCESSING -> {
                            onCancelVoiceInput()
                        }
                    }
                }
        ) {
            Icon(
                imageVector = when {
                    !voiceInputAvailable -> Icons.Default.MicOff
                    isActive -> Icons.Default.Stop
                    else -> Icons.Default.Mic
                },
                contentDescription = when {
                    !voiceInputAvailable -> "Voice input not available"
                    isListening -> "Stop listening"
                    isProcessing -> "Cancel processing"
                    else -> "Start voice input"
                },
                tint = ReagentWhite,
                modifier = Modifier.size(size.iconSize)
            )
        }
        
        if (showTranscription && size != VoiceButtonSize.SMALL) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status/Transcription Text
            Card(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .heightIn(min = 40.dp),
                colors = CardDefaults.cardColors(containerColor = ReagentDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        !voiceInputAvailable -> "Voice input\nnot available"
                        voiceTranscription.isNotEmpty() -> voiceTranscription
                        isListening -> "Listening..."
                        isProcessing -> "Processing..."
                        voiceInputState == VoiceInputState.ERROR -> "Error occurred"
                        else -> "Tap to speak"
                    },
                    color = when {
                        !voiceInputAvailable -> ReagentGray
                        voiceInputState == VoiceInputState.ERROR -> Color.Red
                        isActive -> ReagentGreen
                        voiceInputState == VoiceInputState.COMPLETED -> ReagentWhite
                        else -> ReagentGray
                    },
                    fontSize = size.textSize,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

enum class VoiceButtonSize(
    val buttonSize: androidx.compose.ui.unit.Dp,
    val iconSize: androidx.compose.ui.unit.Dp,
    val textSize: androidx.compose.ui.unit.TextUnit
) {
    SMALL(32.dp, 16.dp, 10.sp),
    MEDIUM(56.dp, 24.dp, 12.sp),
    LARGE(72.dp, 32.dp, 14.sp)
}

@Composable
fun VoiceInputRow(
    voiceInputState: VoiceInputState,
    voiceTranscription: String,
    voiceInputAvailable: Boolean,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onCancelVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VoiceInputButton(
            voiceInputState = voiceInputState,
            voiceTranscription = voiceTranscription,
            voiceInputAvailable = voiceInputAvailable,
            onStartVoiceInput = onStartVoiceInput,
            onStopVoiceInput = onStopVoiceInput,
            onCancelVoiceInput = onCancelVoiceInput,
            size = VoiceButtonSize.MEDIUM,
            showTranscription = false
        )
        
        Text(
            text = when {
                !voiceInputAvailable -> "Voice input not available on this device"
                voiceTranscription.isNotEmpty() && voiceInputState == VoiceInputState.COMPLETED -> 
                    "\"$voiceTranscription\""
                voiceInputState == VoiceInputState.LISTENING -> "Listening for your voice..."
                voiceInputState == VoiceInputState.PROCESSING -> "Processing speech..."
                voiceInputState == VoiceInputState.ERROR -> "Error: ${voiceTranscription.removePrefix("Error: ")}"
                else -> "Tap microphone to speak your instruction"
            },
            color = when {
                !voiceInputAvailable -> ReagentGray
                voiceInputState == VoiceInputState.ERROR -> Color.Red
                voiceInputState == VoiceInputState.LISTENING || voiceInputState == VoiceInputState.PROCESSING -> ReagentGreen
                voiceInputState == VoiceInputState.COMPLETED -> ReagentWhite
                else -> ReagentGray
            },
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
} 