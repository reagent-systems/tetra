package com.example.simple_agent_android.agentcore

import android.util.Log

object TaskCompletionDetector {
    private const val TAG = "TaskCompletionDetector"
    
    fun isTaskComplete(
        context: TaskContext,
        currentScreenAnalysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        Log.d(TAG, "Checking task completion for ${context.taskType} in phase ${context.currentPhase}")
        
        return when (context.taskType) {
            TaskType.MESSAGING -> isMessagingTaskComplete(context, currentScreenAnalysis, lastAction)
            TaskType.CALLING -> isCallingTaskComplete(context, currentScreenAnalysis, lastAction)
            TaskType.EMAIL -> isEmailTaskComplete(context, currentScreenAnalysis, lastAction)
            TaskType.NAVIGATION -> isNavigationTaskComplete(context, currentScreenAnalysis, lastAction)
            else -> isGeneralTaskComplete(context, currentScreenAnalysis, lastAction)
        }
    }
    
    private fun isMessagingTaskComplete(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        // Messaging task requires specific sequence completion
        val hasOpenedMessagingApp = context.actionHistory.any { 
            it.action.contains("press", true) && 
            (it.element?.contains("message", true) == true || analysis.packageName?.contains("message") == true)
        }
        
        val hasTypedMessage = context.actionHistory.any {
            it.action.contains("text", true) && 
            it.result.contains("executed", true)
        }
        
        val hasSentMessage = context.actionHistory.any {
            it.action.contains("press", true) && 
            (it.element?.contains("send", true) == true || 
             it.element?.contains("submit", true) == true ||
             lastAction?.contains("send", true) == true)
        }
        
        // Check for confirmation screens or return to conversation list
        val isInCompletionState = analysis.screenType == ScreenType.LIST_VIEW || 
                                 analysis.interactableElements.any { 
                                     it.displayText.contains("delivered", true) || 
                                     it.displayText.contains("sent", true) ||
                                     it.displayText.contains("conversation", true)
                                 }
        
        val isComplete = hasOpenedMessagingApp && hasTypedMessage && hasSentMessage && isInCompletionState
        
        Log.d(TAG, "Messaging completion check - App: $hasOpenedMessagingApp, Typed: $hasTypedMessage, Sent: $hasSentMessage, Completion state: $isInCompletionState -> Complete: $isComplete")
        
        return isComplete
    }
    
    private fun isCallingTaskComplete(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        val hasOpenedPhoneApp = context.actionHistory.any {
            it.action.contains("press", true) &&
            (it.element?.contains("phone", true) == true || analysis.packageName?.contains("phone") == true)
        }
        
        val hasInitiatedCall = context.actionHistory.any {
            it.action.contains("press", true) &&
            (it.element?.contains("call", true) == true || it.element?.contains("dial", true) == true)
        }
        
        val isInCallState = analysis.interactableElements.any {
            it.displayText.contains("end call", true) ||
            it.displayText.contains("hang up", true) ||
            it.displayText.contains("calling", true)
        }
        
        return hasOpenedPhoneApp && hasInitiatedCall && isInCallState
    }
    
    private fun isEmailTaskComplete(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        // Similar to messaging but for email apps
        val hasOpenedEmailApp = context.actionHistory.any {
            it.action.contains("press", true) &&
            (it.element?.contains("mail", true) == true || analysis.packageName?.contains("mail") == true)
        }
        
        val hasComposedEmail = context.actionHistory.any {
            it.action.contains("text", true) && it.result.contains("executed", true)
        }
        
        val hasSentEmail = context.actionHistory.any {
            it.action.contains("press", true) &&
            (it.element?.contains("send", true) == true || it.element?.contains("submit", true) == true)
        }
        
        return hasOpenedEmailApp && hasComposedEmail && hasSentEmail
    }
    
    private fun isNavigationTaskComplete(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        // Navigation tasks are complete when we reach the target app/screen
        val targetReached = context.actionHistory.any { it.progressMade } &&
                           analysis.loadingState == LoadingState.LOADED &&
                           analysis.interactableElements.isNotEmpty()
        
        return targetReached
    }
    
    private fun isGeneralTaskComplete(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Boolean {
        // Conservative general completion detection
        val hasSignificantProgress = context.lastSignificantProgress > 0
        val isStableState = analysis.loadingState == LoadingState.LOADED
        val stepsSinceProgress = context.currentStep - context.lastSignificantProgress
        val hasCompletedMostCriteria = context.completedCriteria.size >= (context.successCriteria.size * 0.7).toInt()
        
        return hasSignificantProgress && isStableState && stepsSinceProgress <= 2 && hasCompletedMostCriteria
    }
    
    fun getCompletionConfidence(
        context: TaskContext,
        analysis: ScreenAnalysis,
        lastAction: String?
    ): Double {
        // Return confidence level (0.0 to 1.0) for task completion
        val baseConfidence = when (context.taskType) {
            TaskType.MESSAGING -> getMessagingConfidence(context, analysis)
            TaskType.CALLING -> getCallingConfidence(context, analysis)
            TaskType.EMAIL -> getEmailConfidence(context, analysis)
            else -> 0.5
        }
        
        // Adjust based on phase progression
        val phaseBonus = when (context.currentPhase) {
            TaskPhase.VERIFICATION -> 0.3
            TaskPhase.COMPLETION -> 0.2
            TaskPhase.INTERACTION -> 0.1
            else -> 0.0
        }
        
        return (baseConfidence + phaseBonus).coerceIn(0.0, 1.0)
    }
    
    private fun getMessagingConfidence(context: TaskContext, analysis: ScreenAnalysis): Double {
        var confidence = 0.0
        
        if (analysis.packageName?.contains("message") == true) confidence += 0.3
        if (context.actionHistory.any { it.action.contains("text", true) }) confidence += 0.3
        if (context.actionHistory.any { it.element?.contains("send", true) == true }) confidence += 0.4
        
        return confidence
    }
    
    private fun getCallingConfidence(context: TaskContext, analysis: ScreenAnalysis): Double {
        var confidence = 0.0
        
        if (analysis.packageName?.contains("phone") == true) confidence += 0.4
        if (context.actionHistory.any { it.element?.contains("call", true) == true }) confidence += 0.6
        
        return confidence
    }
    
    private fun getEmailConfidence(context: TaskContext, analysis: ScreenAnalysis): Double {
        var confidence = 0.0
        
        if (analysis.packageName?.contains("mail") == true) confidence += 0.3
        if (context.actionHistory.any { it.action.contains("text", true) }) confidence += 0.3
        if (context.actionHistory.any { it.element?.contains("send", true) == true }) confidence += 0.4
        
        return confidence
    }
} 