package com.example.simple_agent_android.sentry

import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Extension functions to easily add Sentry tracking to existing code
 */

/**
 * Execute a block of code with automatic error tracking
 */
inline fun <T> sentryTrack(
    operation: String,
    component: String = "unknown",
    tags: Map<String, String> = emptyMap(),
    crossinline block: () -> T
): T? {
    return try {
        SentryManager.addBreadcrumb(
            message = "Starting operation: $operation",
            category = component
        )
        
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        SentryManager.addBreadcrumb(
            message = "Operation completed: $operation",
            category = component,
            data = mapOf("duration_ms" to duration)
        )
        
        result
    } catch (e: Exception) {
        SentryManager.captureException(
            throwable = e,
            message = "Error in operation: $operation",
            tags = tags + mapOf("component" to component, "operation" to operation)
        )
        null
    }
}

/**
 * Execute a suspending block with automatic error tracking
 */
suspend inline fun <T> sentryTrackSuspend(
    operation: String,
    component: String = "unknown",
    tags: Map<String, String> = emptyMap(),
    crossinline block: suspend () -> T
): T? {
    return try {
        SentryManager.addBreadcrumb(
            message = "Starting async operation: $operation",
            category = component
        )
        
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        SentryManager.addBreadcrumb(
            message = "Async operation completed: $operation",
            category = component,
            data = mapOf("duration_ms" to duration)
        )
        
        result
    } catch (e: Exception) {
        SentryManager.captureException(
            throwable = e,
            message = "Error in async operation: $operation",
            tags = tags + mapOf("component" to component, "operation" to operation)
        )
        null
    }
}

/**
 * Execute a block and track performance metrics
 */
inline fun <T> sentryPerformanceTrack(
    operation: String,
    component: String = "performance",
    warningThresholdMs: Long = 1000,
    errorThresholdMs: Long = 5000,
    crossinline block: () -> T
): T? {
    return try {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        when {
            duration > errorThresholdMs -> {
                SentryManager.captureMessage(
                    message = "Very slow operation detected: $operation",
                    level = SentryLevel.ERROR,
                    tags = mapOf(
                        "performance_issue" to "very_slow",
                        "component" to component,
                        "operation" to operation
                    ),
                    extras = mapOf(
                        "duration_ms" to duration,
                        "threshold_ms" to errorThresholdMs
                    )
                )
            }
            duration > warningThresholdMs -> {
                SentryManager.captureMessage(
                    message = "Slow operation detected: $operation",
                    level = SentryLevel.WARNING,
                    tags = mapOf(
                        "performance_issue" to "slow",
                        "component" to component,
                        "operation" to operation
                    ),
                    extras = mapOf(
                        "duration_ms" to duration,
                        "threshold_ms" to warningThresholdMs
                    )
                )
            }
            else -> {
                SentryManager.addBreadcrumb(
                    message = "Performance tracked: $operation",
                    category = "performance",
                    data = mapOf(
                        "duration_ms" to duration,
                        "operation" to operation
                    )
                )
            }
        }
        
        result
    } catch (e: Exception) {
        SentryManager.captureException(
            throwable = e,
            message = "Error in performance tracked operation: $operation",
            tags = mapOf("component" to component, "operation" to operation)
        )
        null
    }
}

/**
 * Safely execute a coroutine with error tracking
 */
fun CoroutineScope.launchWithSentry(
    operation: String,
    component: String = "coroutine",
    tags: Map<String, String> = emptyMap(),
    block: suspend CoroutineScope.() -> Unit
) {
    launch {
        try {
            SentryManager.addBreadcrumb(
                message = "Starting coroutine: $operation",
                category = component
            )
            
            block()
            
            SentryManager.addBreadcrumb(
                message = "Coroutine completed: $operation",
                category = component
            )
        } catch (e: Exception) {
            SentryManager.captureException(
                throwable = e,
                message = "Error in coroutine: $operation",
                tags = tags + mapOf("component" to component, "operation" to operation)
            )
        }
    }
}

/**
 * Wrap API calls with automatic error tracking
 */
inline fun <T> sentryApiCall(
    endpoint: String,
    operation: String = "api_call",
    crossinline block: () -> T
): T? {
    val transaction = ApiErrorTracker.startApiTransaction(endpoint, operation)
    return try {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        // Track successful API call
        ApiErrorTracker.trackApiSuccess(
            endpoint = endpoint,
            responseCode = 200, // Assume success if no exception
            responseTimeMs = duration
        )
        
        transaction.finish()
        result
    } catch (e: Exception) {
        // Track API error
        ApiErrorTracker.trackOpenAIError(
            error = e,
            endpoint = endpoint
        )
        
        transaction.throwable = e
        transaction.finish()
        null
    }
}

/**
 * Wrap agent operations with automatic tracking
 */
inline fun <T> sentryAgentOperation(
    operation: String,
    step: Int,
    instruction: String,
    crossinline block: () -> T
): T? {
    return try {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        AgentErrorTracker.trackAgentStep(
            stepNumber = step,
            action = operation,
            success = true,
            executionTimeMs = duration
        )
        
        result
    } catch (e: Exception) {
        AgentErrorTracker.trackAgentError(
            error = e,
            agentStep = step,
            instruction = instruction,
            context = mapOf("operation" to operation)
        )
        null
    }
}

/**
 * Extension function for Exception to easily send to Sentry
 */
fun Throwable.sendToSentry(
    message: String? = null,
    component: String = "unknown",
    tags: Map<String, String> = emptyMap(),
    extras: Map<String, Any> = emptyMap()
) {
    SentryManager.captureException(
        throwable = this,
        message = message ?: this.message,
        tags = tags + mapOf("component" to component),
        extras = extras
    )
}

/**
 * Easy breadcrumb tracking
 */
fun trackUserAction(
    action: String,
    screen: String = "unknown",
    data: Map<String, Any> = emptyMap()
) {
    SentryManager.addBreadcrumb(
        message = "User action: $action",
        category = "user_interaction",
        data = data + mapOf(
            "action" to action,
            "screen" to screen
        )
    )
}

/**
 * Track screen navigation
 */
fun trackScreenNavigation(
    fromScreen: String,
    toScreen: String,
    navigationMethod: String = "unknown"
) {
    SentryManager.addBreadcrumb(
        message = "Screen navigation: $fromScreen -> $toScreen",
        category = "navigation",
        data = mapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen,
            "navigation_method" to navigationMethod
        )
    )
}

/**
 * Track feature usage
 */
fun trackFeatureUsage(
    feature: String,
    action: String,
    success: Boolean = true,
    metadata: Map<String, Any> = emptyMap()
) {
    SentryManager.addBreadcrumb(
        message = "Feature usage: $feature - $action",
        level = if (success) SentryLevel.INFO else SentryLevel.WARNING,
        category = "feature_usage",
        data = mapOf(
            "feature" to feature,
            "action" to action,
            "success" to success
        ) + metadata
    )
} 