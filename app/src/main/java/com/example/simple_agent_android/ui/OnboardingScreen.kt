package com.example.simple_agent_android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.simple_agent_android.ui.theme.*
import com.example.simple_agent_android.data.OnboardingStep
import com.example.simple_agent_android.data.OnboardingSteps
import com.example.simple_agent_android.viewmodel.MainViewModel

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    currentStep: OnboardingStep,
    currentStepIndex: Int,
    totalSteps: Int,
    onNextStep: () -> Unit,
    onSkipStep: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    onNavigateToScreen: (String) -> Unit,
    onDismiss: () -> Unit,
    onToggleOverlay: () -> Unit = {},
    onEnableAccessibility: () -> Unit = {},
    onUpdateVerticalOffset: (Int) -> Unit = {},
    onSaveApiKey: () -> Unit = {}
) {
    val progress = (currentStepIndex + 1).toFloat() / totalSteps.toFloat()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header with progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = ReagentWhite,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ReagentGray
                )
            }
        }
        
        Text(
            text = "Step ${currentStepIndex + 1} of $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = ReagentBlue,
            trackColor = ReagentGray.copy(alpha = 0.3f),
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Step Card
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = ReagentDark),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Step Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                ReagentBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(40.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = ReagentBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Step Title
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ReagentWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Step Description
                    Text(
                        text = currentStep.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ReagentGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Action Buttons
                    when (currentStep.id) {
                        OnboardingSteps.ENABLE_ACCESSIBILITY -> {
                            AccessibilityStepContent(
                                viewModel = viewModel,
                                onEnableAccessibility = onEnableAccessibility,
                                onNextStep = onNextStep
                            )
                        }
                        OnboardingSteps.ADJUST_OFFSET -> {
                            OverlayCalibrationStepContent(
                                viewModel = viewModel,
                                onNextStep = onNextStep,
                                onSkipStep = onSkipStep,
                                onToggleOverlay = onToggleOverlay,
                                onUpdateVerticalOffset = onUpdateVerticalOffset
                            )
                        }
                        OnboardingSteps.SET_API_KEY -> {
                            ApiKeyStepContent(
                                viewModel = viewModel,
                                onNextStep = onNextStep,
                                onSaveApiKey = onSaveApiKey
                            )
                        }
                        OnboardingSteps.COMPLETE_SETUP -> {
                            CompletionStepContent(
                                onCompleteOnboarding = onCompleteOnboarding
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
private fun AccessibilityStepContent(
    viewModel: MainViewModel,
    onEnableAccessibility: () -> Unit,
    onNextStep: () -> Unit
) {
    val accessibilityEnabled = viewModel.accessibilityServiceEnabled.value
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (accessibilityEnabled) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = ReagentGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "✓ Accessibility Service Enabled!",
                color = ReagentGreen,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue", color = ReagentBlack, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = ReagentBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Button(
                onClick = onEnableAccessibility,
                colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Accessibility", color = ReagentWhite, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OverlayCalibrationStepContent(
    viewModel: MainViewModel,
    onNextStep: () -> Unit,
    onSkipStep: () -> Unit,
    onToggleOverlay: () -> Unit,
    onUpdateVerticalOffset: (Int) -> Unit
) {
    val overlayActive = viewModel.overlayActive.value
    val verticalOffset = viewModel.verticalOffset.value
    var localOffset by remember { mutableStateOf(verticalOffset.toFloat()) }
    var hasStarted by remember { mutableStateOf(false) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!hasStarted) {
            // Initial state - show overlay button
            Text(
                text = "We'll show you red boxes around interactive elements to help you understand how the agent sees your screen.",
                color = ReagentGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Button(
                onClick = {
                    onToggleOverlay()
                    hasStarted = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Overlay", color = ReagentWhite, fontWeight = FontWeight.Medium)
            }
        } else if (overlayActive) {
            // Overlay is active - show calibration controls
            Text(
                text = "Adjust the vertical offset so the red boxes align perfectly with the buttons and interactive elements.",
                color = ReagentGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Vertical Offset Slider
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Vertical Offset: ${localOffset.roundToInt()}px",
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Slider(
                        value = localOffset,
                        onValueChange = { newValue ->
                            localOffset = newValue
                            onUpdateVerticalOffset(newValue.roundToInt())
                        },
                        valueRange = -200f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = ReagentBlue,
                            activeTrackColor = ReagentBlue,
                            inactiveTrackColor = ReagentGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "-200px",
                            color = ReagentGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "+200px",
                            color = ReagentGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkipStep,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ReagentGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip", fontWeight = FontWeight.Medium)
                }
                
                Button(
                    onClick = {
                        // Hide overlay and proceed
                        onToggleOverlay()
                        onNextStep()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = ReagentWhite, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            // Overlay was disabled - show completion
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = ReagentGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "✓ Overlay Calibrated!",
                color = ReagentGreen,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue", color = ReagentBlack, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = ReagentBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ApiKeyStepContent(
    viewModel: MainViewModel,
    onNextStep: () -> Unit,
    onSaveApiKey: () -> Unit
) {
    val hasApiKey = viewModel.openAiKey.value.isNotBlank()
    var localApiKey by remember { mutableStateOf(viewModel.openAiKey.value) }
    var isValid by remember { mutableStateOf(hasApiKey) }
    
    // Update validation when key changes
    LaunchedEffect(localApiKey) {
        isValid = localApiKey.isNotBlank() && localApiKey.startsWith("sk-")
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (hasApiKey && localApiKey == viewModel.openAiKey.value) {
            // Already configured - show success state
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = ReagentGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "✓ API Key Configured!",
                color = ReagentGreen,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextStep,
                colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue", color = ReagentBlack, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = ReagentBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // Show input form
            Text(
                text = "Get your API key from platform.openai.com and paste it below:",
                color = ReagentGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
                label = { Text("OpenAI API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValid) ReagentGreen else ReagentBlue,
                    unfocusedBorderColor = ReagentGray,
                    cursorColor = ReagentBlue,
                    focusedLabelColor = if (isValid) ReagentGreen else ReagentBlue,
                    unfocusedLabelColor = ReagentGray
                ),
                visualTransformation = if (localApiKey.isNotBlank()) {
                    // Mask the API key with asterisks
                    PasswordVisualTransformation('*')
                } else {
                    VisualTransformation.None
                },
                singleLine = true
            )
            
            if (localApiKey.isNotBlank() && !isValid) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "API key should start with 'sk-'",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    // Save the API key and proceed
                    viewModel.updateOpenAiKey(localApiKey)
                    onSaveApiKey()
                    onNextStep()
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isValid) ReagentGreen else ReagentGray,
                    disabledContainerColor = ReagentGray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Save & Continue",
                    color = if (isValid) ReagentBlack else ReagentGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CompletionStepContent(
    onCompleteOnboarding: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "🎉",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "You're all set! Your Simple Agent is ready to help you automate tasks across your apps.",
            color = ReagentGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = onCompleteOnboarding,
            colors = ButtonDefaults.buttonColors(containerColor = ReagentGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Using Simple Agent", color = ReagentBlack, fontWeight = FontWeight.Medium)
        }
    }
} 