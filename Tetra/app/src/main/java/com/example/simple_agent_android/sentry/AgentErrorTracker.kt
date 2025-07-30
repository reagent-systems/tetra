package com.example.simple_agent_android.sentry

import io.sentry.SentryLevel
import io.sentry.ITransaction

object AgentErrorTracker {
    
    /**
     * Track agent execution errors
     */
    fun trackAgentError(
        error: Throwable,
        agentStep: Int,
        instruction: String,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Agent execution error at step $agentStep",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "agent_execution",
                "agent_step" to agentStep.toString(),
                "error_stage" to "execution"
            ),
            extras = mapOf(
                "instruction" to instruction.take(200), // Limit instruction length
                "agent_step" to agentStep,
                "execution_context" to context
            )
        )
    }
    
    /**
     * Track agent task completion failures
     */
    fun trackTaskCompletionError(
        instruction: String,
        totalSteps: Int,
        lastSuccessfulStep: Int,
        reason: String,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureMessage(
            message = "Agent failed to complete task: $reason",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "agent_completion",
                "completion_status" to "failed",
                "failure_reason" to reason
            ),
            extras = mapOf(
                "instruction" to instruction.take(200),
                "total_steps" to totalSteps,
                "last_successful_step" to lastSuccessfulStep,
                "completion_percentage" to ((lastSuccessfulStep.toFloat() / totalSteps) * 100).toInt(),
                "context" to context
            )
        )
    }
    
    /**
     * Track successful agent task completion
     */
    fun trackTaskSuccess(
        instruction: String,
        totalSteps: Int,
        executionTimeMs: Long,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.addBreadcrumb(
            message = "Agent task completed successfully",
            level = SentryLevel.INFO,
            category = "agent_success",
            data = mapOf(
                "instruction" to instruction.take(100),
                "total_steps" to totalSteps,
                "execution_time_ms" to executionTimeMs,
                "steps_per_second" to (totalSteps.toFloat() / (executionTimeMs / 1000f)).toString()
            )
        )
        
        // Track performance issues for slow executions
        if (executionTimeMs > 60000) { // 1 minute
            SentryManager.captureMessage(
                message = "Slow agent execution detected",
                level = SentryLevel.WARNING,
                tags = mapOf(
                    "performance_issue" to "slow_execution",
                    "component" to "agent_performance"
                ),
                extras = mapOf(
                    "execution_time_ms" to executionTimeMs,
                    "total_steps" to totalSteps,
                    "instruction" to instruction.take(200)
                )
            )
        }
    }
    
    /**
     * Track accessibility service issues
     */
    fun trackAccessibilityError(
        error: Throwable,
        action: String,
        elementInfo: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Accessibility service error during: $action",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "accessibility_service",
                "action" to action,
                "error_type" to "accessibility"
            ),
            extras = mapOf(
                "action" to action,
                "element_info" to elementInfo,
                "accessibility_enabled" to "unknown" // Could be determined at runtime
            )
        )
    }
    
    /**
     * Track screen analysis failures
     */
    fun trackScreenAnalysisError(
        error: Throwable,
        screenData: String? = null,
        analysisStep: String
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Screen analysis failed at: $analysisStep",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "screen_analysis",
                "analysis_step" to analysisStep,
                "error_type" to "screen_analysis"
            ),
            extras = mapOf(
                "analysis_step" to analysisStep,
                "screen_data_length" to (screenData?.length ?: 0),
                "has_screen_data" to (screenData != null).toString()
            )
        )
    }
    
    /**
     * Track agent loop detection
     */
    fun trackLoopDetection(
        instruction: String,
        loopSteps: List<Int>,
        loopType: String,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureMessage(
            message = "Agent loop detected: $loopType",
            level = SentryLevel.WARNING,
            tags = mapOf(
                "component" to "loop_detection",
                "loop_type" to loopType,
                "issue_type" to "infinite_loop"
            ),
            extras = mapOf(
                "instruction" to instruction.take(200),
                "loop_steps" to loopSteps,
                "loop_length" to loopSteps.size,
                "context" to context
            )
        )
    }
    
    /**
     * Track voice input issues
     */
    fun trackVoiceInputError(
        error: Throwable,
        stage: String, // "initialization", "recording", "processing", "transcription"
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Voice input error at stage: $stage",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "voice_input",
                "voice_stage" to stage,
                "error_type" to "voice_input"
            ),
            extras = mapOf(
                "stage" to stage,
                "context" to context
            )
        )
    }
    
    /**
     * Track floating UI issues
     */
    fun trackFloatingUIError(
        error: Throwable,
        component: String, // "floating_button", "completion_screen", "overlay"
        action: String,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Floating UI error in $component during $action",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "component" to "floating_ui",
                "ui_component" to component,
                "ui_action" to action
            ),
            extras = mapOf(
                "ui_component" to component,
                "action" to action,
                "context" to context
            )
        )
    }
    
    /**
     * Track agent performance metrics
     */
    fun trackPerformanceMetrics(
        instruction: String,
        metrics: Map<String, Any>
    ) {
        SentryManager.addBreadcrumb(
            message = "Agent performance metrics",
            level = SentryLevel.INFO,
            category = "performance",
            data = mapOf(
                "instruction_hash" to instruction.hashCode().toString(),
                "metrics" to metrics
            )
        )
        
        // Check for performance issues
        val executionTime = metrics["execution_time_ms"] as? Long
        val stepCount = metrics["total_steps"] as? Int
        val memoryUsage = metrics["memory_usage_mb"] as? Float
        
        if (executionTime != null && executionTime > 120000) { // 2 minutes
            SentryManager.captureMessage(
                message = "Very slow agent execution",
                level = SentryLevel.ERROR,
                tags = mapOf("performance_issue" to "very_slow_execution"),
                extras = mapOf("execution_time_ms" to executionTime)
            )
        }
        
        if (memoryUsage != null && memoryUsage > 500) { // 500MB
            SentryManager.captureMessage(
                message = "High memory usage detected",
                level = SentryLevel.WARNING,
                tags = mapOf("performance_issue" to "high_memory"),
                extras = mapOf("memory_usage_mb" to memoryUsage)
            )
        }
    }
    
    /**
     * Start an agent execution transaction
     */
    fun startAgentTransaction(instruction: String): ITransaction {
        val transaction = SentryManager.startTransaction("Agent Execution", "agent_task")
        transaction.setData("instruction", instruction.take(100))
        return transaction
    }
    
    /**
     * Track agent step execution
     */
    fun trackAgentStep(
        stepNumber: Int,
        action: String,
        success: Boolean,
        executionTimeMs: Long,
        context: Map<String, Any> = emptyMap()
    ) {
        SentryManager.addBreadcrumb(
            message = "Agent step $stepNumber: $action",
            level = if (success) SentryLevel.INFO else SentryLevel.WARNING,
            category = "agent_step",
            data = mapOf(
                "step_number" to stepNumber,
                "action" to action,
                "success" to success,
                "execution_time_ms" to executionTimeMs,
                "context" to context
            )
        )
    }
} 