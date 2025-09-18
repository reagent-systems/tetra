package com.example.simple_agent_android.agentcore.mapping

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Example demonstrating how to use the semantic mapping system
 */
object SemanticMappingExample {
    private const val TAG = "SemanticMappingExample"
    
    /**
     * Example: How to use the enhanced agent with semantic mapping
     */
    fun demonstrateEnhancedAgent(context: Context, instruction: String, apiKey: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load existing UI memory
                val memoryLoaded = UIMemoryPersistence.loadUIMemory(context)
                Log.d(TAG, "UI memory loaded: $memoryLoaded")
                
                // Run the enhanced agent
                com.example.simple_agent_android.agentcore.EnhancedAgentOrchestrator.runAgent(
                    instruction = instruction,
                    apiKey = apiKey,
                    context = context,
                    onAgentStopped = {
                        Log.d(TAG, "Enhanced agent stopped")
                        // Save UI memory when agent stops
                        UIMemoryPersistence.saveUIMemory(context)
                    },
                    onOutput = { output ->
                        Log.d(TAG, "Agent output: $output")
                        // Handle real-time output
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error running enhanced agent", e)
            }
        }
    }
    
    /**
     * Example: How to query the semantic memory
     */
    suspend fun demonstrateMemoryQuery() {
        try {
            // Get memory statistics
            val stats = UIMemoryManager.getStatistics()
            Log.d(TAG, "Memory statistics: $stats")
            
            // Check for loops
            val loopAnalysis = UIMemoryManager.detectLoop()
            if (loopAnalysis.isLooping) {
                Log.d(TAG, "Loop detected: ${loopAnalysis.loopType} (severity: ${loopAnalysis.severity})")
            }
            
            // Get navigation path (if we had a target screen)
            val navigationPath = UIMemoryManager.getNavigationPath("target_screen_id")
            if (navigationPath != null) {
                Log.d(TAG, "Found navigation path with ${navigationPath.size} steps")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error querying memory", e)
        }
    }
    
    /**
     * Example: How to manually record a screen state
     */
    suspend fun demonstrateManualRecording(screenJson: String, appPackage: String) {
        try {
            val screenId = UIMemoryManager.recordScreenState(screenJson, emptyList(), appPackage)
            Log.d(TAG, "Recorded screen state: $screenId")
            
            // Find similar screen
            val similarScreen = UIMemoryManager.findSimilarScreen(screenJson)
            if (similarScreen != null) {
                Log.d(TAG, "Found similar screen: ${similarScreen.screenId}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recording screen state", e)
        }
    }
    
    /**
     * Example: How to handle UI changes
     */
    suspend fun demonstrateUIChangeHandling() {
        try {
            // Simulate an element being moved
            val elementMoved = UIChanges(
                type = UIChangeType.ELEMENT_MOVED,
                elementId = "gmail_compose_button",
                newPosition = UIElementFingerprint.Bounds(100, 200, 200, 250)
            )
            UIMemoryManager.updateNavigationTree(elementMoved)
            
            // Simulate an app update
            val appUpdated = UIChanges(
                type = UIChangeType.APP_UPDATED,
                appPackage = "com.google.android.gm"
            )
            UIMemoryManager.updateNavigationTree(appUpdated)
            
            Log.d(TAG, "UI changes processed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling UI changes", e)
        }
    }
    
    /**
     * Example: How to get the navigation tree structure
     */
    suspend fun demonstrateTreeStructure() {
        try {
            // This would need to be implemented in UIMemoryManager
            // val treeString = UIMemoryManager.getNavigationTreeString()
            // Log.d(TAG, "Navigation tree:\n$treeString")
            
            val stats = UIMemoryManager.getStatistics()
            Log.d(TAG, "Tree statistics: $stats")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tree structure", e)
        }
    }
}

/**
 * Example usage in MainActivity or ViewModel
 */
class ExampleUsage {
    
    fun startEnhancedAgent(context: Context, instruction: String, apiKey: String) {
        // Use the enhanced agent instead of the regular one
        SemanticMappingExample.demonstrateEnhancedAgent(context, instruction, apiKey)
    }
    
    suspend fun explorePhoneUI(context: Context, screenJson: String) {
        // Manually record screen states as the user navigates
        SemanticMappingExample.demonstrateManualRecording(screenJson, "com.example.app")
    }
    
    suspend fun checkForLoops() {
        // Check if we're in a loop
        SemanticMappingExample.demonstrateMemoryQuery()
    }
    
    suspend fun handleAppUpdate(context: Context, packageName: String) {
        // Handle UI changes when apps are updated
        val appUpdated = UIChanges(
            type = UIChangeType.APP_UPDATED,
            appPackage = packageName
        )
        UIMemoryManager.updateNavigationTree(appUpdated)
        
        // Save updated memory
        UIMemoryPersistence.saveUIMemory(context)
    }
}
