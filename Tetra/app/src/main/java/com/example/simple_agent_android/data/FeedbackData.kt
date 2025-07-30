package com.example.simple_agent_android.data

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    val user_name: String,
    val feedback: String,
    val category: String,
    val app_version: String,
    val device_info: String,
    val screenshots: List<String> = emptyList()
)

enum class FeedbackCategory(val displayName: String) {
    BUG("Bug Report"),
    FEATURE("Feature Request"),
    UI_UX("UI/UX Issue"),
    PERFORMANCE("Performance Issue"),
    ACCESSIBILITY("Accessibility Issue"),
    OTHER("Other")
} 