package com.example.simple_agent_android.agentcore

import android.util.Log
import org.json.JSONObject

data class TaskContext(
    val originalInstruction: String,
    val primaryObjective: String,
    var currentStep: Int,
    val totalSteps: Int,
    val successCriteria: List<String>,
    val completedCriteria: MutableList<String> = mutableListOf(),
    val actionHistory: MutableList<ActionRecord> = mutableListOf(),
    val screenHistory: MutableList<ScreenState> = mutableListOf(),
    var lastSignificantProgress: Int = 0, // Step number of last meaningful progress
    var confidence: Double = 0.0
)

data class ActionRecord(
    val step: Int,
    val action: String,
    val element: String?,
    val result: String,
    val screenChanged: Boolean,
    val progressMade: Boolean
)

data class ScreenState(
    val step: Int,
    val screenType: ScreenType,
    val loadingState: LoadingState,
    val keyElements: List<String>,
    val hash: String // Simple hash to detect screen changes
)

object TaskContextManager {
    private const val TAG = "TaskContextManager"
    private var currentContext: TaskContext? = null
    
    fun initializeTask(instruction: String, plan: String?): TaskContext {
        Log.d(TAG, "Initializing task: $instruction")
        
        // Parse plan if available
        val (objective, criteria, estimatedSteps) = parsePlan(plan, instruction)
        
        val context = TaskContext(
            originalInstruction = instruction,
            primaryObjective = objective,
            currentStep = 0,
            totalSteps = estimatedSteps,
            successCriteria = criteria
        )
        
        currentContext = context
        return context
    }
    
    fun recordAction(
        action: String,
        element: String?,
        result: String,
        previousScreenAnalysis: ScreenAnalysis?,
        currentScreenAnalysis: ScreenAnalysis
    ) {
        val context = currentContext ?: return
        
        val screenChanged = detectScreenChange(previousScreenAnalysis, currentScreenAnalysis)
        val progressMade = evaluateProgress(action, result, screenChanged, currentScreenAnalysis)
        
        val actionRecord = ActionRecord(
            step = context.currentStep,
            action = action,
            element = element,
            result = result,
            screenChanged = screenChanged,
            progressMade = progressMade
        )
        
        context.actionHistory.add(actionRecord)
        
        // Update last significant progress
        if (progressMade) {
            context.lastSignificantProgress = context.currentStep
        }
        
        // Record screen state
        recordScreenState(currentScreenAnalysis)
        
        Log.d(TAG, "Recorded action: $action, progress: $progressMade, screen changed: $screenChanged")
    }
    
    private fun recordScreenState(analysis: ScreenAnalysis) {
        val context = currentContext ?: return
        
        val keyElements = analysis.interactableElements.take(5).map { it.displayText }
        val hash = generateScreenHash(analysis)
        
        val screenState = ScreenState(
            step = context.currentStep,
            screenType = analysis.screenType,
            loadingState = analysis.loadingState,
            keyElements = keyElements,
            hash = hash
        )
        
        context.screenHistory.add(screenState)
        
        // Keep only last 10 screen states to prevent memory issues
        if (context.screenHistory.size > 10) {
            context.screenHistory.removeAt(0)
        }
    }
    
    private fun generateScreenHash(analysis: ScreenAnalysis): String {
        // Simple hash based on key elements and screen type
        val keyInfo = "${analysis.screenType}_${analysis.loadingState}_" +
                analysis.interactableElements.take(10).joinToString("_") { 
                    "${it.className}_${it.displayText}_${it.bounds.centerX}_${it.bounds.centerY}"
                }
        return keyInfo.hashCode().toString()
    }
    
    private fun detectScreenChange(previous: ScreenAnalysis?, current: ScreenAnalysis): Boolean {
        if (previous == null) return true
        
        val prevHash = generateScreenHash(previous)
        val currentHash = generateScreenHash(current)
        
        return prevHash != currentHash
    }
    
    private fun evaluateProgress(
        action: String,
        result: String,
        screenChanged: Boolean,
        currentAnalysis: ScreenAnalysis
    ): Boolean {
        // Consider progress made if:
        // 1. Screen changed meaningfully
        // 2. Successfully performed a significant action
        // 3. Moved closer to goal state
        
        return when {
            screenChanged && currentAnalysis.loadingState == LoadingState.LOADED -> true
            action.contains("text") && result.contains("Set text") -> true
            action.contains("press") && screenChanged -> true
            action.contains("swipe") && screenChanged -> true
            result.contains("success", ignoreCase = true) -> true
            else -> false
        }
    }
    
    fun isStuckInLoop(): Boolean {
        val context = currentContext ?: return false
        
        // Check if no progress in last 3 steps
        val stepsSinceProgress = context.currentStep - context.lastSignificantProgress
        if (stepsSinceProgress > 3) return true
        
        // Check for repeated actions
        val recentActions = context.actionHistory.takeLast(3)
        if (recentActions.size >= 3) {
            val actionTypes = recentActions.map { it.action.split(" ").first() }
            if (actionTypes.all { it == actionTypes.first() }) return true
        }
        
        // Check for repeated screen states
        val recentScreens = context.screenHistory.takeLast(3)
        if (recentScreens.size >= 3) {
            val hashes = recentScreens.map { it.hash }
            if (hashes.all { it == hashes.first() }) return true
        }
        
        return false
    }
    
    fun shouldTaskComplete(): Boolean {
        val context = currentContext ?: return false
        
        // Check if we've made progress recently
        val stepsSinceProgress = context.currentStep - context.lastSignificantProgress
        
        // Check if we're in a stable state that might indicate completion
        val recentScreens = context.screenHistory.takeLast(2)
        val isStableScreen = recentScreens.size >= 2 && 
                recentScreens.all { it.loadingState == LoadingState.LOADED }
        
        // Simple heuristics for completion
        return when {
            // Made significant progress and now in stable state
            context.lastSignificantProgress > 0 && isStableScreen && stepsSinceProgress <= 1 -> true
            // Completed multiple criteria
            context.completedCriteria.size >= context.successCriteria.size / 2 -> true
            // High confidence from previous evaluations
            context.confidence > 0.8 -> true
            else -> false
        }
    }
    
    fun getContextSummary(): String {
        val context = currentContext ?: return "No active task"
        
        val progressPercent = if (context.totalSteps > 0) {
            (context.currentStep.toDouble() / context.totalSteps * 100).toInt()
        } else {
            (context.lastSignificantProgress.toDouble() / context.currentStep.coerceAtLeast(1) * 100).toInt()
        }
        
        return """
            Task: ${context.primaryObjective}
            Progress: ${context.currentStep}/${context.totalSteps} steps ($progressPercent%)
            Last Progress: Step ${context.lastSignificantProgress}
            Completed Criteria: ${context.completedCriteria.size}/${context.successCriteria.size}
            Recent Actions: ${context.actionHistory.takeLast(3).joinToString(", ") { it.action }}
        """.trimIndent()
    }
    
    fun advanceStep() {
        currentContext?.let { it.currentStep++ }
    }
    
    fun getCurrentContext(): TaskContext? = currentContext
    
    fun updateConfidence(confidence: Double) {
        currentContext?.let { it.confidence = confidence }
    }
    
    private fun parsePlan(plan: String?, instruction: String): Triple<String, List<String>, Int> {
        if (plan == null) {
            return Triple(instruction, listOf("Complete the task"), 5)
        }
        
        try {
            val json = JSONObject(plan)
            val objective = json.optString("primary_objective", instruction)
            val criteriaArray = json.optJSONArray("success_criteria")
            val criteria = mutableListOf<String>()
            
            if (criteriaArray != null) {
                for (i in 0 until criteriaArray.length()) {
                    criteria.add(criteriaArray.getString(i))
                }
            }
            
            val stepsArray = json.optJSONArray("estimated_steps")
            val estimatedSteps = stepsArray?.length() ?: 5
            
            return Triple(objective, criteria, estimatedSteps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse plan JSON", e)
            return Triple(instruction, listOf("Complete the task"), 5)
        }
    }
} 