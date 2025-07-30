package com.example.simple_agent_android.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingStep(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val targetScreen: String,
    val actionText: String,
    val isCompleted: Boolean = false,
    val canSkip: Boolean = false,
    val validationCheck: (() -> Boolean)? = null
)

enum class OnboardingState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

object OnboardingSteps {
    const val ENABLE_ACCESSIBILITY = "enable_accessibility"
    const val NAVIGATE_DEBUG = "navigate_debug"
    const val ENABLE_OVERLAY = "enable_overlay"
    const val ADJUST_OFFSET = "adjust_offset"
    const val DISABLE_OVERLAY = "disable_overlay"
    const val SET_API_KEY = "set_api_key"
    const val COMPLETE_SETUP = "complete_setup"
    
    fun getAllSteps(): List<OnboardingStep> = listOf(
        OnboardingStep(
            id = ENABLE_ACCESSIBILITY,
            title = "Enable Accessibility Service",
            description = "Allow Simple Agent to interact with other apps by enabling the accessibility service. This is required for the agent to work.",
            icon = Icons.Default.Accessibility,
            targetScreen = "home",
            actionText = "Enable Accessibility"
        ),
        OnboardingStep(
            id = ADJUST_OFFSET,
            title = "Calibrate Overlay System",
            description = "We'll show you the overlay system and let you adjust the alignment. The red boxes help the agent understand your screen layout.",
            icon = Icons.Default.Tune,
            targetScreen = "onboarding",
            actionText = "Calibrate Overlay",
            canSkip = true
        ),
        OnboardingStep(
            id = SET_API_KEY,
            title = "Set OpenAI API Key",
            description = "Enter your OpenAI API key to enable the AI functionality. You can get one from platform.openai.com.",
            icon = Icons.Default.Key,
            targetScreen = "settings",
            actionText = "Enter API Key"
        ),
        OnboardingStep(
            id = COMPLETE_SETUP,
            title = "Setup Complete!",
            description = "Congratulations! Your Simple Agent is now ready to use. You can start giving it commands from the home screen.",
            icon = Icons.Default.CheckCircle,
            targetScreen = "home",
            actionText = "Start Using Agent"
        )
    )
} 