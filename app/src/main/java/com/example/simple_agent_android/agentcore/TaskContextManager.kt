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
    var confidence: Double = 0.0,
    val taskType: TaskType = TaskType.GENERAL,
    var currentPhase: TaskPhase = TaskPhase.NAVIGATION
)

data class ActionRecord(
    val step: Int,
    val action: String,
    val element: String?,
    val result: String,
    val screenChanged: Boolean,
    val progressMade: Boolean,
    val phaseAdvancement: Boolean = false
)

data class ScreenState(
    val step: Int,
    val screenType: ScreenType,
    val loadingState: LoadingState,
    val keyElements: List<String>,
    val hash: String, // Simple hash to detect screen changes
    val packageName: String? = null
)

enum class TaskType {
    MESSAGING, CALLING, EMAIL, NAVIGATION, SETTINGS, MEDIA, GENERAL
}

enum class TaskPhase {
    NAVIGATION,    // Finding and opening the right app
    APP_LOADING,   // Waiting for app to load
    INTERACTION,   // Main task interactions (typing, selecting)
    COMPLETION,    // Final actions (sending, saving)
    VERIFICATION   // Confirming task success
}

object TaskContextManager {
    private const val TAG = "TaskContextManager"
    private var currentContext: TaskContext? = null
    
    fun initializeTask(instruction: String, plan: String?): TaskContext {
        Log.d(TAG, "Initializing task: $instruction")
        
        // Parse plan if available and determine task type
        val (objective, criteria, estimatedSteps) = parsePlan(plan, instruction)
        val taskType = determineTaskType(instruction)
        
        val context = TaskContext(
            originalInstruction = instruction,
            primaryObjective = objective,
            currentStep = 0,
            totalSteps = estimatedSteps,
            successCriteria = criteria,
            taskType = taskType,
            currentPhase = TaskPhase.NAVIGATION
        )
        
        currentContext = context
        Log.d(TAG, "Task initialized - Type: $taskType, Phase: ${context.currentPhase}")
        return context
    }
    
    private fun determineTaskType(instruction: String): TaskType {
        val lowerInstruction = instruction.lowercase()
        return when {
            lowerInstruction.contains("message") || lowerInstruction.contains("text") || lowerInstruction.contains("sms") -> TaskType.MESSAGING
            lowerInstruction.contains("call") || lowerInstruction.contains("phone") -> TaskType.CALLING
            lowerInstruction.contains("email") || lowerInstruction.contains("mail") -> TaskType.EMAIL
            lowerInstruction.contains("navigate") || lowerInstruction.contains("go to") || lowerInstruction.contains("open") -> TaskType.NAVIGATION
            lowerInstruction.contains("setting") || lowerInstruction.contains("config") -> TaskType.SETTINGS
            lowerInstruction.contains("play") || lowerInstruction.contains("music") || lowerInstruction.contains("video") -> TaskType.MEDIA
            else -> TaskType.GENERAL
        }
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
        val phaseAdvancement = detectPhaseAdvancement(action, result, currentScreenAnalysis, element)
        
        val actionRecord = ActionRecord(
            step = context.currentStep,
            action = action,
            element = element,
            result = result,
            screenChanged = screenChanged,
            progressMade = progressMade,
            phaseAdvancement = phaseAdvancement
        )
        
        context.actionHistory.add(actionRecord)
        
        // Update last significant progress
        if (progressMade) {
            context.lastSignificantProgress = context.currentStep
        }
        
        // Update task phase if advancement detected
        if (phaseAdvancement) {
            updateTaskPhase(currentScreenAnalysis)
        }
        
        // Record screen state
        recordScreenState(currentScreenAnalysis)
        
        Log.d(TAG, "Recorded action: $action, progress: $progressMade, phase advancement: $phaseAdvancement, current phase: ${context.currentPhase}")
    }
    
    private fun detectPhaseAdvancement(action: String, result: String, analysis: ScreenAnalysis, element: String? = null): Boolean {
        val context = currentContext ?: return false
        
        return when (context.currentPhase) {
            TaskPhase.NAVIGATION -> {
                // Advanced if we opened the target app
                when (context.taskType) {
                    TaskType.MESSAGING -> analysis.packageName?.contains("message") == true || 
                                         analysis.interactableElements.any { it.displayText.contains("compose", true) || it.displayText.contains("new message", true) }
                    TaskType.CALLING -> analysis.packageName?.contains("phone") == true || analysis.packageName?.contains("dialer") == true
                    else -> action.contains("press", true) && result.contains("executed")
                }
            }
            TaskPhase.APP_LOADING -> {
                // Advanced if app is fully loaded with interactive elements
                analysis.loadingState == LoadingState.LOADED && analysis.interactableElements.isNotEmpty()
            }
            TaskPhase.INTERACTION -> {
                // Advanced if we performed meaningful interactions (text input, selections)
                action.contains("text", true) || (action.contains("press", true) && analysis.interactableElements.any { 
                    it.displayText.contains("send", true) || it.displayText.contains("call", true) 
                })
            }
            TaskPhase.COMPLETION -> {
                // Advanced if we executed final action (send, call, etc.)
                action.contains("press", true) && (element?.contains("send", true) == true || element?.contains("call", true) == true)
            }
            TaskPhase.VERIFICATION -> false // Final phase
        }
    }
    
    private fun updateTaskPhase(analysis: ScreenAnalysis) {
        val context = currentContext ?: return
        
        val newPhase = when (context.currentPhase) {
            TaskPhase.NAVIGATION -> TaskPhase.APP_LOADING
            TaskPhase.APP_LOADING -> TaskPhase.INTERACTION
            TaskPhase.INTERACTION -> TaskPhase.COMPLETION
            TaskPhase.COMPLETION -> TaskPhase.VERIFICATION
            TaskPhase.VERIFICATION -> TaskPhase.VERIFICATION // Stay in final phase
        }
        
        if (newPhase != context.currentPhase) {
            Log.d(TAG, "Phase advancement: ${context.currentPhase} -> $newPhase")
            context.currentPhase = newPhase
        }
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
            hash = hash,
            packageName = analysis.packageName
        )
        
        context.screenHistory.add(screenState)
        
        // Keep only last 10 screen states to prevent memory issues
        if (context.screenHistory.size > 10) {
            context.screenHistory.removeAt(0)
        }
    }
    
    private fun generateScreenHash(analysis: ScreenAnalysis): String {
        // Simple hash based on key elements and screen type
        val keyInfo = "${analysis.screenType}_${analysis.loadingState}_${analysis.packageName}_" +
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
        val context = currentContext ?: return false
        
        // Consider progress made if:
        // 1. Screen changed meaningfully
        // 2. Successfully performed a significant action
        // 3. Moved closer to goal state
        // 4. Advanced to new app or interface
        
        return when {
            screenChanged && currentAnalysis.loadingState == LoadingState.LOADED -> true
            action.contains("text") && result.contains("executed") -> true
            action.contains("press") && screenChanged -> true
            action.contains("swipe") && screenChanged -> true
            result.contains("success", ignoreCase = true) -> true
            // App-specific progress indicators
            context.taskType == TaskType.MESSAGING && currentAnalysis.packageName?.contains("message") == true -> true
            context.taskType == TaskType.CALLING && currentAnalysis.packageName?.contains("phone") == true -> true
            else -> false
        }
    }
    
    fun isStuckInLoop(): Boolean {
        val context = currentContext ?: return false
        
        // More lenient loop detection - allow more steps for complex tasks
        val maxStepsWithoutProgress = when (context.taskType) {
            TaskType.MESSAGING, TaskType.EMAIL -> 5 // Messaging tasks can be complex
            TaskType.CALLING -> 4
            else -> 3
        }
        
        // Check if no progress in last N steps
        val stepsSinceProgress = context.currentStep - context.lastSignificantProgress
        if (stepsSinceProgress > maxStepsWithoutProgress) return true
        
        // Check for repeated actions (more lenient)
        val recentActions = context.actionHistory.takeLast(4)
        if (recentActions.size >= 4) {
            val actionTypes = recentActions.map { it.action.split(" ").first() }
            if (actionTypes.all { it == actionTypes.first() }) return true
        }
        
        // Check for repeated screen states (more lenient)
        val recentScreens = context.screenHistory.takeLast(4)
        if (recentScreens.size >= 4) {
            val hashes = recentScreens.map { it.hash }
            if (hashes.all { it == hashes.first() }) return true
        }
        
        return false
    }
    
    fun shouldTaskComplete(currentScreenAnalysis: ScreenAnalysis? = null, lastAction: String? = null): Boolean {
        val context = currentContext ?: return false
        
        // Use the new TaskCompletionDetector if we have screen analysis
        if (currentScreenAnalysis != null) {
            val isComplete = TaskCompletionDetector.isTaskComplete(context, currentScreenAnalysis, lastAction)
            val confidence = TaskCompletionDetector.getCompletionConfidence(context, currentScreenAnalysis, lastAction)
            
            // Update context confidence
            context.confidence = confidence
            
            Log.d(TAG, "Task completion check via detector - Type: ${context.taskType}, Phase: ${context.currentPhase}, Complete: $isComplete, Confidence: $confidence")
            
            return isComplete
        }
        
        // Fallback to conservative detection without screen analysis
        val stepsSinceProgress = context.currentStep - context.lastSignificantProgress
        val hasSignificantProgress = context.lastSignificantProgress >= 3 // Require more progress
        
        val fallbackCompletion = hasSignificantProgress && 
                                context.currentPhase >= TaskPhase.COMPLETION &&
                                stepsSinceProgress <= 1 &&
                                context.confidence > 0.7
        
        Log.d(TAG, "Task completion check (fallback) - Complete: $fallbackCompletion")
        
        return fallbackCompletion
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
            Type: ${context.taskType}
            Phase: ${context.currentPhase}
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
            // Create smart default criteria based on task type
            val taskType = determineTaskType(instruction)
            val defaultCriteria = when (taskType) {
                TaskType.MESSAGING -> listOf("Open messaging app", "Find or create conversation", "Type message", "Send message")
                TaskType.CALLING -> listOf("Open phone app", "Find contact or enter number", "Initiate call")
                TaskType.EMAIL -> listOf("Open email app", "Compose new email", "Enter recipient", "Type message", "Send email")
                else -> listOf("Navigate to target", "Perform main action", "Complete task")
            }
            return Triple(instruction, defaultCriteria, defaultCriteria.size + 2)
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
            val taskType = determineTaskType(instruction)
            val defaultCriteria = when (taskType) {
                TaskType.MESSAGING -> listOf("Open messaging app", "Find or create conversation", "Type message", "Send message")
                TaskType.CALLING -> listOf("Open phone app", "Find contact or enter number", "Initiate call")
                else -> listOf("Complete the task")
            }
            return Triple(instruction, defaultCriteria, defaultCriteria.size + 2)
        }
    }
} 