package com.example.simple_agent_android.agentcore.mapping

/**
 * Represents a path from one screen to another with the actions required
 */
data class NavigationPath(
    val fromScreenId: String,        // Source screen identifier
    val toScreenId: String,          // Destination screen identifier
    val actions: List<Action>,       // Sequence of actions to perform
    val successRate: Float = 1.0f,   // Success rate of this path (0.0-1.0)
    val lastUsed: Long = System.currentTimeMillis(), // When this path was last used
    val useCount: Int = 0,           // How many times this path has been used
    val averageTimeMs: Long = 0L,    // Average time to complete this path
    val confidence: Float = 1.0f,    // Confidence in this path (0.0-1.0)
    val isObsolete: Boolean = false, // Whether this path is outdated
    val metadata: Map<String, Any> = emptyMap() // Additional metadata
) {
    /**
     * Mark this path as used and update statistics
     */
    fun markAsUsed(executionTimeMs: Long): NavigationPath {
        val newSuccessRate = if (successRate > 0) {
            // Simple moving average for success rate
            (successRate * useCount + 1.0f) / (useCount + 1)
        } else {
            1.0f
        }
        
        val newAverageTime = if (averageTimeMs > 0) {
            (averageTimeMs * useCount + executionTimeMs) / (useCount + 1)
        } else {
            executionTimeMs
        }
        
        return copy(
            successRate = newSuccessRate,
            lastUsed = System.currentTimeMillis(),
            useCount = useCount + 1,
            averageTimeMs = newAverageTime
        )
    }
    
    /**
     * Mark this path as failed and update statistics
     */
    fun markAsFailed(): NavigationPath {
        val newSuccessRate = if (useCount > 0) {
            (successRate * useCount) / (useCount + 1)
        } else {
            0.0f
        }
        
        return copy(
            successRate = newSuccessRate,
            lastUsed = System.currentTimeMillis(),
            useCount = useCount + 1
        )
    }
    
    /**
     * Mark this path as obsolete (UI has changed)
     */
    fun markAsObsolete(): NavigationPath {
        return copy(isObsolete = true)
    }
    
    /**
     * Check if this path is reliable (high success rate, recent use)
     */
    fun isReliable(): Boolean {
        val recentUse = System.currentTimeMillis() - lastUsed < 24 * 60 * 60 * 1000L // 24 hours
        return !isObsolete && successRate > 0.7f && recentUse
    }
    
    /**
     * Get the estimated time to complete this path
     */
    fun getEstimatedTimeMs(): Long {
        return if (averageTimeMs > 0) averageTimeMs else {
            // Estimate based on action count (rough estimate)
            actions.size * 1000L // 1 second per action
        }
    }
}

/**
 * Represents a single action in a navigation path
 */
data class Action(
    val type: ActionType,            // Type of action
    val targetElement: String?,      // Semantic ID of target element (if applicable)
    val coordinates: Coordinates?,   // Screen coordinates (if applicable)
    val text: String?,               // Text to input (if applicable)
    val duration: Long = 0L,         // Duration for wait actions
    val parameters: Map<String, Any> = emptyMap() // Additional parameters
) {
    data class Coordinates(
        val x: Int,
        val y: Int
    )
    
    /**
     * Create a click action
     */
    companion object {
        fun click(elementId: String, x: Int, y: Int): Action {
            return Action(
                type = ActionType.CLICK,
                targetElement = elementId,
                coordinates = Coordinates(x, y),
                text = null
            )
        }
        
        fun textInput(elementId: String, x: Int, y: Int, text: String): Action {
            return Action(
                type = ActionType.TEXT_INPUT,
                targetElement = elementId,
                coordinates = Coordinates(x, y),
                text = text
            )
        }
        
        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = 300L): Action {
            return Action(
                type = ActionType.SWIPE,
                targetElement = null,
                coordinates = Coordinates(startX, startY),
                text = null,
                duration = duration,
                parameters = mapOf(
                    "endX" to endX,
                    "endY" to endY
                )
            )
        }
        
        fun wait(durationMs: Long): Action {
            return Action(
                type = ActionType.WAIT,
                targetElement = null,
                coordinates = null,
                text = null,
                duration = durationMs
            )
        }
        
        fun goHome(): Action {
            return Action(
                type = ActionType.NAVIGATION,
                targetElement = null,
                coordinates = null,
                text = null,
                parameters = mapOf("action" to "home")
            )
        }
        
        fun goBack(): Action {
            return Action(
                type = ActionType.NAVIGATION,
                targetElement = null,
                coordinates = null,
                text = null,
                parameters = mapOf("action" to "back")
            )
        }
    }
}
