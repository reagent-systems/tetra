package com.example.simple_agent_android.agentcore.mapping

/**
 * Represents a complete state of a screen with all its UI elements and context
 */
data class ScreenState(
    val screenId: String,                    // Unique identifier for this screen state
    val appPackage: String,                  // Package name of the current app
    val screenTitle: String?,                // Human-readable title of the screen
    val elements: List<UIElementFingerprint>, // All interactive elements on screen
    val navigationPaths: List<NavigationPath>, // Available navigation paths from this screen
    val lastVisited: Long = System.currentTimeMillis(), // When this screen was last visited
    val visitCount: Int = 1,                 // How many times this screen has been visited
    val confidence: Float = 1.0f,            // Confidence in this screen state (0.0-1.0)
    val screenType: ScreenType = ScreenType.UNKNOWN, // Type of screen (home, app, dialog, etc.)
    val isStable: Boolean = true,            // Whether this screen state is stable (not loading)
    val metadata: Map<String, Any> = emptyMap() // Additional metadata about the screen
) {
    /**
     * Find elements by semantic ID
     */
    fun findElementBySemanticId(semanticId: String): UIElementFingerprint? {
        return elements.find { it.semanticId == semanticId }
    }
    
    /**
     * Find elements by text content
     */
    fun findElementsByText(text: String): List<UIElementFingerprint> {
        return elements.filter { 
            it.text?.contains(text, ignoreCase = true) == true ||
            it.contentDescription?.contains(text, ignoreCase = true) == true
        }
    }
    
    /**
     * Find elements by action type
     */
    fun findElementsByActionType(actionType: ActionType): List<UIElementFingerprint> {
        return elements.filter { it.actionType == actionType }
    }
    
    /**
     * Get the most important elements (highest priority)
     */
    fun getMostImportantElements(limit: Int = 5): List<UIElementFingerprint> {
        return elements.sortedByDescending { it.priority }.take(limit)
    }
    
    /**
     * Calculate similarity with another screen state
     */
    fun calculateSimilarity(other: ScreenState): Float {
        // Check if same app
        if (appPackage != other.appPackage) return 0.0f
        
        // Check if same screen type
        if (screenType != other.screenType) return 0.3f
        
        // Calculate element similarity
        val elementSimilarity = calculateElementSimilarity(other.elements)
        
        // Calculate navigation path similarity
        val pathSimilarity = calculatePathSimilarity(other.navigationPaths)
        
        // Weighted combination
        return elementSimilarity * 0.7f + pathSimilarity * 0.3f
    }
    
    private fun calculateElementSimilarity(otherElements: List<UIElementFingerprint>): Float {
        if (elements.isEmpty() && otherElements.isEmpty()) return 1.0f
        if (elements.isEmpty() || otherElements.isEmpty()) return 0.0f
        
        var totalSimilarity = 0.0f
        var matchCount = 0
        
        for (element in elements) {
            val bestMatch = otherElements.maxByOrNull { element.calculateSimilarity(it) }
            if (bestMatch != null && element.calculateSimilarity(bestMatch) > 0.5f) {
                totalSimilarity += element.calculateSimilarity(bestMatch)
                matchCount++
            }
        }
        
        return if (matchCount > 0) totalSimilarity / matchCount else 0.0f
    }
    
    private fun calculatePathSimilarity(otherPaths: List<NavigationPath>): Float {
        if (navigationPaths.isEmpty() && otherPaths.isEmpty()) return 1.0f
        if (navigationPaths.isEmpty() || otherPaths.isEmpty()) return 0.0f
        
        val commonPaths = navigationPaths.intersect(otherPaths.toSet()).size
        val totalPaths = navigationPaths.union(otherPaths.toSet()).size
        
        return if (totalPaths > 0) commonPaths.toFloat() / totalPaths else 0.0f
    }
    
    /**
     * Check if this screen state is likely the same as another
     */
    fun isLikelySameScreen(other: ScreenState, similarityThreshold: Float = 0.8f): Boolean {
        return calculateSimilarity(other) >= similarityThreshold
    }
    
    /**
     * Update this screen state with new information
     */
    fun updateWithNewInfo(
        newElements: List<UIElementFingerprint>,
        newPaths: List<NavigationPath> = emptyList(),
        newMetadata: Map<String, Any> = emptyMap()
    ): ScreenState {
        return copy(
            elements = newElements,
            navigationPaths = navigationPaths + newPaths,
            lastVisited = System.currentTimeMillis(),
            visitCount = visitCount + 1,
            metadata = metadata + newMetadata
        )
    }
}

enum class ScreenType {
    HOME_SCREEN,        // Android home screen
    APP_DRAWER,         // App drawer/launcher
    APP_MAIN,           // Main screen of an app
    APP_SUBSCREEN,      // Sub-screen within an app
    DIALOG,             // Dialog or popup
    SETTINGS,           // Settings screen
    NOTIFICATION,       // Notification panel
    KEYBOARD,           // On-screen keyboard
    LOADING,            // Loading screen
    ERROR,              // Error screen
    UNKNOWN             // Unknown screen type
}
