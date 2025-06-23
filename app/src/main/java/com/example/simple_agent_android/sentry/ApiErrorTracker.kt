package com.example.simple_agent_android.sentry

import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection

object ApiErrorTracker {
    
    /**
     * Track OpenAI API errors with detailed context
     */
    fun trackOpenAIError(
        error: Throwable,
        endpoint: String,
        requestBody: String? = null,
        responseCode: Int? = null,
        responseBody: String? = null,
        apiKey: String? = null
    ) {
        val sanitizedApiKey = apiKey?.let { "***${it.takeLast(4)}" } ?: "not_provided"
        
        SentryManager.captureException(
            throwable = error,
            message = "OpenAI API Error: ${error.message}",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "api_provider" to "openai",
                "endpoint" to endpoint,
                "response_code" to (responseCode?.toString() ?: "unknown"),
                "has_api_key" to (apiKey != null).toString()
            ),
            extras = mapOf(
                "endpoint" to endpoint,
                "request_body" to (requestBody?.take(500) ?: "not_provided"), // Limit size
                "response_code" to (responseCode ?: "unknown"),
                "response_body" to (responseBody?.take(1000) ?: "not_provided"), // Limit size
                "api_key_suffix" to sanitizedApiKey,
                "error_type" to error.javaClass.simpleName
            )
        )
        
        // Add breadcrumb for API call
        SentryManager.addBreadcrumb(
            message = "OpenAI API call failed",
            level = SentryLevel.ERROR,
            category = "api_error",
            data = mapOf(
                "endpoint" to endpoint,
                "response_code" to (responseCode ?: "unknown"),
                "error_message" to (error.message ?: "unknown")
            )
        )
    }
    
    /**
     * Track successful API calls for monitoring
     */
    fun trackApiSuccess(
        endpoint: String,
        responseCode: Int,
        responseTimeMs: Long,
        requestSize: Int? = null,
        responseSize: Int? = null
    ) {
        SentryManager.addBreadcrumb(
            message = "API call successful",
            level = SentryLevel.INFO,
            category = "api_success",
            data = mapOf(
                "endpoint" to endpoint,
                "response_code" to responseCode,
                "response_time_ms" to responseTimeMs,
                "request_size_bytes" to (requestSize ?: 0),
                "response_size_bytes" to (responseSize ?: 0)
            )
        )
        
        // Track performance if response time is slow
        if (responseTimeMs > 5000) { // 5 seconds
            SentryManager.captureMessage(
                message = "Slow API response detected",
                level = SentryLevel.WARNING,
                tags = mapOf(
                    "performance_issue" to "slow_api",
                    "endpoint" to endpoint
                ),
                extras = mapOf(
                    "response_time_ms" to responseTimeMs,
                    "threshold_ms" to 5000
                )
            )
        }
    }
    
    /**
     * Track network connectivity issues
     */
    fun trackNetworkError(
        error: Throwable,
        endpoint: String? = null,
        attemptNumber: Int = 1
    ) {
        SentryManager.captureException(
            throwable = error,
            message = "Network connectivity error",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "error_type" to "network",
                "endpoint" to (endpoint ?: "unknown"),
                "attempt_number" to attemptNumber.toString()
            ),
            extras = mapOf(
                "network_error_type" to error.javaClass.simpleName,
                "is_retry" to (attemptNumber > 1).toString()
            )
        )
    }
    
    /**
     * Track API rate limiting
     */
    fun trackRateLimit(
        endpoint: String,
        retryAfter: String? = null,
        remainingRequests: Int? = null
    ) {
        SentryManager.captureMessage(
            message = "API rate limit exceeded",
            level = SentryLevel.WARNING,
            tags = mapOf(
                "api_issue" to "rate_limit",
                "endpoint" to endpoint
            ),
            extras = mapOf(
                "retry_after" to (retryAfter ?: "unknown"),
                "remaining_requests" to (remainingRequests ?: "unknown")
            )
        )
    }
    
    /**
     * Track authentication failures
     */
    fun trackAuthError(
        endpoint: String,
        errorMessage: String? = null,
        responseCode: Int? = null
    ) {
        SentryManager.captureMessage(
            message = "API authentication failed",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "api_issue" to "authentication",
                "endpoint" to endpoint,
                "response_code" to (responseCode?.toString() ?: "unknown")
            ),
            extras = mapOf(
                "error_message" to (errorMessage ?: "unknown"),
                "suggests_invalid_key" to (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED).toString()
            )
        )
    }
    
    /**
     * Track API quota/billing issues
     */
    fun trackQuotaError(
        endpoint: String,
        errorMessage: String? = null,
        quotaType: String? = null
    ) {
        SentryManager.captureMessage(
            message = "API quota exceeded",
            level = SentryLevel.ERROR,
            tags = mapOf(
                "api_issue" to "quota_exceeded",
                "endpoint" to endpoint,
                "quota_type" to (quotaType ?: "unknown")
            ),
            extras = mapOf(
                "error_message" to (errorMessage ?: "unknown"),
                "requires_billing_check" to "true"
            )
        )
    }
    
    /**
     * Parse and track structured API errors
     */
    fun trackStructuredApiError(
        endpoint: String,
        responseCode: Int,
        responseBody: String,
        requestContext: Map<String, Any> = emptyMap()
    ) {
        try {
            val jsonResponse = JSONObject(responseBody)
            val errorObj = jsonResponse.optJSONObject("error")
            
            val errorType = errorObj?.optString("type") ?: "unknown"
            val errorMessage = errorObj?.optString("message") ?: "unknown"
            val errorCode = errorObj?.optString("code") ?: "unknown"
            
            when (errorType) {
                "insufficient_quota" -> trackQuotaError(endpoint, errorMessage, "insufficient_quota")
                "invalid_api_key" -> trackAuthError(endpoint, errorMessage, responseCode)
                "rate_limit_exceeded" -> trackRateLimit(endpoint)
                else -> {
                    SentryManager.captureMessage(
                        message = "Structured API error: $errorMessage",
                        level = SentryLevel.ERROR,
                        tags = mapOf(
                            "api_error_type" to errorType,
                            "api_error_code" to errorCode,
                            "endpoint" to endpoint,
                            "response_code" to responseCode.toString()
                        ),
                        extras = requestContext + mapOf(
                            "error_message" to errorMessage,
                            "full_response" to responseBody.take(1000)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // If we can't parse the error response, track it as a generic error
            SentryManager.captureException(
                throwable = e,
                message = "Failed to parse API error response",
                tags = mapOf(
                    "endpoint" to endpoint,
                    "response_code" to responseCode.toString()
                ),
                extras = mapOf(
                    "raw_response" to responseBody.take(1000)
                )
            )
        }
    }
    
    /**
     * Start tracking an API call transaction
     */
    fun startApiTransaction(endpoint: String, operation: String = "api_call") =
        SentryManager.startTransaction("API: $endpoint", operation)
} 