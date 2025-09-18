package com.example.simple_agent_android.agentcore.mapping

import android.graphics.Rect

/**
 * Represents a unique fingerprint of a UI element for semantic mapping
 */
data class UIElementFingerprint(
    val semanticId: String,           // Unique semantic identifier (e.g., "gmail_compose_button")
    val text: String?,               // Display text of the element
    val contentDescription: String?, // Accessibility content description
    val className: String,           // Android class name (e.g., "android.widget.Button")
    val bounds: Bounds,              // Position and size information
    val parentContext: String,       // Context of parent screen (e.g., "Gmail main screen")
    val actionType: ActionType,      // Type of action this element supports
    val isClickable: Boolean,        // Whether element can be clicked
    val isEditable: Boolean,         // Whether element can be edited
    val isScrollable: Boolean,       // Whether element can be scrolled
    val priority: Int,               // Priority for interaction (higher = more important)
    val lastSeen: Long = System.currentTimeMillis(), // When this element was last observed
    val confidence: Float = 1.0f     // Confidence in this fingerprint (0.0-1.0)
) {
    data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val centerX: Int get() = (left + right) / 2
        val centerY: Int get() = (top + bottom) / 2
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val area: Int get() = width * height
    }
    
    /**
     * Calculate semantic similarity with another fingerprint
     * Returns a value between 0.0 (completely different) and 1.0 (identical)
     */
    fun calculateSimilarity(other: UIElementFingerprint): Float {
        var similarity = 0.0f
        var weightSum = 0.0f
        
        // Text similarity (high weight)
        if (text != null && other.text != null) {
            val textSim = calculateTextSimilarity(text, other.text)
            similarity += textSim * 0.4f
            weightSum += 0.4f
        }
        
        // Content description similarity (high weight)
        if (contentDescription != null && other.contentDescription != null) {
            val descSim = calculateTextSimilarity(contentDescription, other.contentDescription)
            similarity += descSim * 0.3f
            weightSum += 0.3f
        }
        
        // Class name similarity (medium weight)
        val classSim = if (className == other.className) 1.0f else 0.0f
        similarity += classSim * 0.2f
        weightSum += 0.2f
        
        // Action type similarity (medium weight)
        val actionSim = if (actionType == other.actionType) 1.0f else 0.0f
        similarity += actionSim * 0.1f
        weightSum += 0.1f
        
        return if (weightSum > 0) similarity / weightSum else 0.0f
    }
    
    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        val normalized1 = text1.lowercase().trim()
        val normalized2 = text2.lowercase().trim()
        
        // Exact match
        if (normalized1 == normalized2) return 1.0f
        
        // Substring match
        if (normalized1.contains(normalized2) || normalized2.contains(normalized1)) {
            return 0.8f
        }
        
        // Word overlap
        val words1 = normalized1.split("\\s+".toRegex()).toSet()
        val words2 = normalized2.split("\\s+".toRegex()).toSet()
        val intersection = words1.intersect(words2)
        val union = words1.union(words2)
        
        return if (union.isNotEmpty()) intersection.size.toFloat() / union.size else 0.0f
    }
    
    /**
     * Check if this element is likely the same as another based on position and content
     */
    fun isLikelySameElement(other: UIElementFingerprint, positionThreshold: Int = 50): Boolean {
        // Check if positions are close
        val positionClose = kotlin.math.abs(centerX - other.centerX) < positionThreshold &&
                          kotlin.math.abs(centerY - other.centerY) < positionThreshold
        
        // Check if content is similar
        val contentSimilar = calculateSimilarity(other) > 0.7f
        
        return positionClose && contentSimilar
    }
    
    val centerX: Int get() = bounds.centerX
    val centerY: Int get() = bounds.centerY
}

enum class ActionType {
    CLICK,          // Button, clickable text, etc.
    TEXT_INPUT,     // EditText, input fields
    SWIPE,          // Scrollable areas
    LONG_CLICK,     // Long press actions
    NAVIGATION,     // Back, home, menu buttons
    WAIT,           // Wait for specified duration
    UNKNOWN
}
