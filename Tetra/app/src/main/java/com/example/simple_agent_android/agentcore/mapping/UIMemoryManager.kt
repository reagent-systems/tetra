package com.example.simple_agent_android.agentcore.mapping

import android.content.Context
import android.util.Log
import com.example.simple_agent_android.utils.SharedPrefsUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages semantic UI mapping and navigation memory
 * This is the central component that stores and retrieves UI state information
 */
object UIMemoryManager {
    private const val TAG = "UIMemoryManager"
    private const val SIMILARITY_THRESHOLD = 0.8f
    private const val LOOP_DETECTION_THRESHOLD = 0.7f
    private const val MAX_RECENT_ACTIONS = 10
    
    private val navigationTree = NavigationTree()
    private val elementFingerprints = ConcurrentHashMap<String, UIElementFingerprint>()
    private val recentActions = mutableListOf<Action>()
    private val mutex = Mutex()
    
    // Current state
    private var currentScreenId: String? = null
    private var currentScreenState: ScreenState? = null
    
    /**
     * Record a screen state and its elements
     */
    suspend fun recordScreenState(
        screenJson: String,
        actions: List<Action> = emptyList(),
        appPackage: String? = null
    ): String {
        return mutex.withLock {
            try {
                val elements = parseScreenJson(screenJson)
                val screenState = createScreenState(elements, appPackage)
                
                // Find similar existing screen
                val similarNode = navigationTree.findSimilarScreenState(screenState, SIMILARITY_THRESHOLD)
                
                val screenId = if (similarNode != null) {
                    // Update existing screen
                    val updatedScreenState = similarNode.screenState!!.updateWithNewInfo(
                        newElements = elements,
                        newPaths = extractNavigationPaths(actions)
                    )
                    similarNode.screenState = updatedScreenState
                    similarNode.id
                } else {
                    // Create new screen
                    navigationTree.addScreenState(screenState, currentScreenId)
                }
                
                // Update current state
                currentScreenId = screenId
                currentScreenState = screenState
                
                // Record recent actions
                recordRecentActions(actions)
                
                // Update element fingerprints
                updateElementFingerprints(elements)
                
                Log.d(TAG, "Recorded screen state: $screenId")
                screenId
                
            } catch (e: Exception) {
                Log.e(TAG, "Error recording screen state", e)
                "unknown_screen"
            }
        }
    }
    
    /**
     * Find a similar screen state
     */
    suspend fun findSimilarScreen(screenJson: String): ScreenState? {
        return mutex.withLock {
            val elements = parseScreenJson(screenJson)
            val screenState = createScreenState(elements)
            navigationTree.findSimilarScreenState(screenState, SIMILARITY_THRESHOLD)?.screenState
        }
    }
    
    /**
     * Get navigation path from current screen to target
     */
    suspend fun getNavigationPath(targetScreenId: String): List<NavigationPath>? {
        return mutex.withLock {
            currentScreenId?.let { fromId ->
                navigationTree.getPath(fromId, targetScreenId)
            }
        }
    }
    
    /**
     * Get the next action to perform based on current state and goal
     */
    suspend fun getNextAction(goal: String): Action? {
        return mutex.withLock {
            // First, try to find a known path to the goal
            val knownPath = findKnownPathToGoal(goal)
            if (knownPath != null) {
                return@withLock knownPath
            }
            
            // If no known path, let the AI decide
            null
        }
    }
    
    /**
     * Detect if we're in a loop using semantic analysis
     */
    suspend fun detectLoop(): LoopAnalysis {
        return mutex.withLock {
            val analysis = LoopAnalysis()
            
            // Check for repeated screen states
            val recentScreenStates = getRecentScreenStates(5)
            if (recentScreenStates.size >= 3) {
                val similarity = calculateScreenSequenceSimilarity(recentScreenStates)
                if (similarity > LOOP_DETECTION_THRESHOLD) {
                    analysis.isLooping = true
                    analysis.loopType = "screen_repetition"
                    analysis.severity = (similarity * 10).toInt()
                }
            }
            
            // Check for repeated action sequences
            val actionSimilarity = calculateActionSequenceSimilarity(recentActions.takeLast(5))
            if (actionSimilarity > LOOP_DETECTION_THRESHOLD) {
                analysis.isLooping = true
                analysis.loopType = "action_repetition"
                analysis.severity = (actionSimilarity * 10).toInt()
            }
            
            // Check for stuck in same app
            val appStuck = isStuckInSameApp()
            if (appStuck) {
                analysis.isLooping = true
                analysis.loopType = "app_stuck"
                analysis.severity = 5
            }
            
            analysis
        }
    }
    
    /**
     * Update navigation tree when UI changes are detected
     */
    suspend fun updateNavigationTree(changes: UIChanges) {
        mutex.withLock {
            when (changes.type) {
                UIChangeType.ELEMENT_MOVED -> {
                    // Update element positions
                    changes.elementId?.let { elementId ->
                        elementFingerprints[elementId]?.let { fingerprint ->
                            val updatedFingerprint = fingerprint.copy(
                                bounds = fingerprint.bounds.copy(
                                    left = changes.newPosition?.x ?: fingerprint.bounds.left,
                                    top = changes.newPosition?.y ?: fingerprint.bounds.top
                                )
                            )
                            elementFingerprints[elementId] = updatedFingerprint
                        }
                    }
                }
                
                UIChangeType.ELEMENT_REMOVED -> {
                    // Mark element as obsolete
                    changes.elementId?.let { elementId ->
                        elementFingerprints.remove(elementId)
                    }
                }
                
                UIChangeType.SCREEN_CHANGED -> {
                    // Mark screen as obsolete
                    changes.screenId?.let { screenId ->
                        navigationTree.markScreenObsolete(screenId)
                    }
                }
                
                UIChangeType.APP_UPDATED -> {
                    // Mark all screens in app as obsolete
                    changes.appPackage?.let { packageName ->
                        navigationTree.getScreensInApp(packageName).forEach { screenState ->
                            navigationTree.markScreenObsolete(screenState.screenId)
                        }
                    }
                }
            }
            
            // Clean up obsolete data
            navigationTree.cleanupObsoleteScreens()
        }
    }
    
    /**
     * Get statistics about the UI memory
     */
    suspend fun getStatistics(): UIMemoryStatistics {
        return mutex.withLock {
            val treeStats = navigationTree.getStatistics()
            UIMemoryStatistics(
                totalScreens = treeStats.screenNodes,
                totalElements = elementFingerprints.size,
                recentActions = recentActions.size,
                maxTreeDepth = treeStats.maxDepth,
                obsoleteScreens = treeStats.obsoleteNodes
            )
        }
    }
    
    // Private helper methods
    
    private fun parseScreenJson(screenJson: String): List<UIElementFingerprint> {
        val elements = mutableListOf<UIElementFingerprint>()
        
        try {
            val jsonArray = JSONArray(screenJson)
            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.getJSONObject(i)
                val fingerprint = createElementFingerprint(element)
                elements.add(fingerprint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing screen JSON", e)
        }
        
        return elements
    }
    
    private fun createElementFingerprint(element: JSONObject): UIElementFingerprint {
        val text = element.optString("text").takeIf { it.isNotEmpty() }
        val contentDescription = element.optString("contentDescription").takeIf { it.isNotEmpty() }
        val className = element.optString("className", "unknown")
        val bounds = UIElementFingerprint.Bounds(
            left = element.optInt("x", 0),
            top = element.optInt("y", 0),
            right = element.optInt("x", 0) + element.optInt("width", 0),
            bottom = element.optInt("y", 0) + element.optInt("height", 0)
        )
        
        val semanticId = generateSemanticId(text, contentDescription, className)
        val actionType = determineActionType(className, element.optBoolean("clickable", false))
        val priority = calculatePriority(text, contentDescription, className, bounds)
        
        return UIElementFingerprint(
            semanticId = semanticId,
            text = text,
            contentDescription = contentDescription,
            className = className,
            bounds = bounds,
            parentContext = currentScreenState?.screenTitle ?: "unknown",
            actionType = actionType,
            isClickable = element.optBoolean("clickable", false),
            isEditable = className.contains("EditText"),
            isScrollable = element.optBoolean("scrollable", false),
            priority = priority
        )
    }
    
    private fun createScreenState(elements: List<UIElementFingerprint>, appPackage: String? = null): ScreenState {
        val packageName = appPackage ?: elements.firstOrNull()?.let { 
            // Extract package from element context
            "unknown"
        } ?: "unknown"
        
        val screenTitle = determineScreenTitle(elements)
        val screenType = determineScreenType(elements)
        val screenId = generateScreenId(packageName, screenTitle)
        
        return ScreenState(
            screenId = screenId,
            appPackage = packageName,
            screenTitle = screenTitle,
            elements = elements,
            navigationPaths = emptyList(),
            screenType = screenType
        )
    }
    
    private fun generateSemanticId(text: String?, contentDescription: String?, className: String): String {
        val baseText = text ?: contentDescription ?: className
        return baseText.lowercase()
            .replace("\\s+".toRegex(), "_")
            .replace("[^a-z0-9_]".toRegex(), "")
            .take(50)
    }
    
    private fun determineActionType(className: String, isClickable: Boolean): ActionType {
        return when {
            className.contains("EditText") -> ActionType.TEXT_INPUT
            className.contains("Button") -> ActionType.CLICK
            className.contains("ScrollView") -> ActionType.SWIPE
            isClickable -> ActionType.CLICK
            else -> ActionType.UNKNOWN
        }
    }
    
    private fun calculatePriority(text: String?, contentDescription: String?, className: String, bounds: UIElementFingerprint.Bounds): Int {
        var priority = 0
        
        // Base priority for interactive elements
        if (className.contains("Button") || className.contains("EditText")) priority += 50
        
        // Priority based on text content
        val displayText = (text ?: contentDescription ?: "").lowercase()
        when {
            displayText.contains("submit") || displayText.contains("send") -> priority += 30
            displayText.contains("next") || displayText.contains("continue") -> priority += 25
            displayText.contains("search") -> priority += 20
            displayText.contains("login") || displayText.contains("sign in") -> priority += 20
        }
        
        // Size-based priority
        val area = bounds.area
        when {
            area in 5000..50000 -> priority += 10
            area in 1000..5000 -> priority += 5
        }
        
        return priority
    }
    
    private fun determineScreenTitle(elements: List<UIElementFingerprint>): String? {
        // Look for title elements
        val titleElement = elements.find { 
            it.className.contains("Title") || 
            it.text?.contains("Settings") == true ||
            it.text?.contains("Home") == true
        }
        return titleElement?.text ?: "Unknown Screen"
    }
    
    private fun determineScreenType(elements: List<UIElementFingerprint>): ScreenType {
        val textInputCount = elements.count { it.actionType == ActionType.TEXT_INPUT }
        val buttonCount = elements.count { it.actionType == ActionType.CLICK }
        
        return when {
            elements.isEmpty() -> ScreenType.UNKNOWN
            textInputCount >= 2 -> ScreenType.APP_SUBSCREEN
            buttonCount >= 5 -> ScreenType.APP_MAIN
            elements.any { it.text?.contains("Settings") == true } -> ScreenType.SETTINGS
            else -> ScreenType.UNKNOWN
        }
    }
    
    private fun generateScreenId(packageName: String, screenTitle: String?): String {
        val baseId = "${packageName}_${screenTitle ?: "unknown"}"
        return baseId.lowercase().replace("\\s+".toRegex(), "_")
    }
    
    private fun extractNavigationPaths(actions: List<Action>): List<NavigationPath> {
        // This would extract navigation paths from actions
        // For now, return empty list
        return emptyList()
    }
    
    private fun recordRecentActions(actions: List<Action>) {
        recentActions.addAll(actions)
        if (recentActions.size > MAX_RECENT_ACTIONS) {
            recentActions.removeAt(0)
        }
    }
    
    private fun updateElementFingerprints(elements: List<UIElementFingerprint>) {
        elements.forEach { element ->
            elementFingerprints[element.semanticId] = element
        }
    }
    
    private fun findKnownPathToGoal(goal: String): Action? {
        // Look for elements that match the goal
        val matchingElements = currentScreenState?.findElementsByText(goal)
        return matchingElements?.firstOrNull()?.let { element ->
            Action.click(element.semanticId, element.centerX, element.centerY)
        }
    }
    
    private fun getRecentScreenStates(count: Int): List<ScreenState> {
        // This would return recent screen states
        // For now, return empty list
        return emptyList()
    }
    
    private fun calculateScreenSequenceSimilarity(screens: List<ScreenState>): Float {
        if (screens.size < 2) return 0.0f
        
        var totalSimilarity = 0.0f
        for (i in 0 until screens.size - 1) {
            totalSimilarity += screens[i].calculateSimilarity(screens[i + 1])
        }
        
        return totalSimilarity / (screens.size - 1)
    }
    
    private fun calculateActionSequenceSimilarity(actions: List<Action>): Float {
        if (actions.size < 2) return 0.0f
        
        var totalSimilarity = 0.0f
        for (i in 0 until actions.size - 1) {
            val similarity = if (actions[i].type == actions[i + 1].type) 1.0f else 0.0f
            totalSimilarity += similarity
        }
        
        return totalSimilarity / (actions.size - 1)
    }
    
    private fun isStuckInSameApp(): Boolean {
        // Check if we've been in the same app for too long without progress
        return false // Simplified for now
    }
}

data class LoopAnalysis(
    var isLooping: Boolean = false,
    var loopType: String = "none",
    var severity: Int = 0,
    var confidence: Float = 0.0f
)

data class UIMemoryStatistics(
    val totalScreens: Int,
    val totalElements: Int,
    val recentActions: Int,
    val maxTreeDepth: Int,
    val obsoleteScreens: Int
)

enum class UIChangeType {
    ELEMENT_MOVED,
    ELEMENT_REMOVED,
    SCREEN_CHANGED,
    APP_UPDATED
}

data class UIChanges(
    val type: UIChangeType,
    val elementId: String? = null,
    val screenId: String? = null,
    val appPackage: String? = null,
    val newPosition: UIElementFingerprint.Bounds? = null
)
