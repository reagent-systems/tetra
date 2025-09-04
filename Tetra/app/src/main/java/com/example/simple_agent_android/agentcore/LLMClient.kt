package com.example.simple_agent_android.agentcore

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.example.simple_agent_android.sentry.ApiErrorTracker
import com.example.simple_agent_android.sentry.sentryApiCall

class LLMClient(private val apiKey: String, private val baseUrl: String = "https://api.openai.com", private val model: String = "gpt-4o") {
    private val client = OkHttpClient()
    private val apiUrl = "$baseUrl/v1/chat/completions"

    // Tool definitions for OpenAI function calling
    private val tools = JSONArray(listOf(
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "simulate_press",
                "description" to "Simulate a press at the center of the given screen element. Use center_x and center_y from the JSON.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "center_x" to mapOf("type" to "integer", "description" to "Center X coordinate of the element"),
                        "center_y" to mapOf("type" to "integer", "description" to "Center Y coordinate of the element")
                    ),
                    "required" to listOf("center_x", "center_y")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "set_text",
                "description" to "Set text at the given screen coordinates.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "x" to mapOf("type" to "integer", "description" to "X coordinate"),
                        "y" to mapOf("type" to "integer", "description" to "Y coordinate"),
                        "text" to mapOf("type" to "string", "description" to "Text to set")
                    ),
                    "required" to listOf("x", "y", "text")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "go_home",
                "description" to "Go to the home screen (simulate pressing the home button).",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf<String, Any>(),
                    "required" to listOf<String>()
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "go_back",
                "description" to "Simulate pressing the back button.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf<String, Any>(),
                    "required" to listOf<String>()
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "swipe",
                "description" to "Simulate a swipe gesture from (startX, startY) to (endX, endY) over a duration in ms.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "startX" to mapOf("type" to "integer", "description" to "Start X coordinate"),
                        "startY" to mapOf("type" to "integer", "description" to "Start Y coordinate"),
                        "endX" to mapOf("type" to "integer", "description" to "End X coordinate"),
                        "endY" to mapOf("type" to "integer", "description" to "End Y coordinate"),
                        "duration" to mapOf("type" to "integer", "description" to "Duration in ms (default 300)")
                    ),
                    "required" to listOf("startX", "startY", "endX", "endY")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "wait_for",
                "description" to "Wait for a specified number of milliseconds before continuing.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "duration_ms" to mapOf("type" to "integer", "description" to "Duration to wait in milliseconds")
                    ),
                    "required" to listOf("duration_ms")
                )
            )
        ),
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to "wait_for_element",
                "description" to "Wait until a UI element matching the given criteria appears, or until a timeout is reached.",
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "text" to mapOf("type" to "string", "description" to "Text of the element (optional)"),
                        "contentDescription" to mapOf("type" to "string", "description" to "Content description of the element (optional)"),
                        "className" to mapOf("type" to "string", "description" to "Class name of the element (optional)"),
                        "timeout_ms" to mapOf("type" to "integer", "description" to "Timeout in milliseconds (default 5000)")
                    ),
                    "required" to listOf("timeout_ms")
                )
            )
        )
    ))

    fun sendWithTools(messages: List<Map<String, Any>>): JSONObject? {
        return sentryApiCall(
            endpoint = apiUrl,
            operation = "chat_completion_with_tools"
        ) {
            val startTime = System.currentTimeMillis()
            
            val json = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray(messages.map { JSONObject(it) }))
                put("tools", tools)
                put("tool_choice", "auto")
            }
            
            val requestBody = json.toString()
            val body = requestBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
                
            android.util.Log.i("LLMClient", "Request payload: ${requestBody.take(1000)}")
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                val duration = System.currentTimeMillis() - startTime
                
                android.util.Log.i("LLMClient", "HTTP status: ${response.code}")
                android.util.Log.i("LLMClient", "Response body: ${responseBody?.take(1000)}")
                
                if (!response.isSuccessful) {
                    android.util.Log.e("LLMClient", "Unsuccessful response: ${response.code} ${responseBody}")
                    
                    // Track specific API errors
                    when (response.code) {
                        401 -> ApiErrorTracker.trackAuthError(
                            endpoint = apiUrl,
                            errorMessage = responseBody,
                            responseCode = response.code
                        )
                        429 -> ApiErrorTracker.trackRateLimit(
                            endpoint = apiUrl,
                            retryAfter = response.header("Retry-After")
                        )
                        400 -> {
                            // Check for message sequencing errors first
                            if (responseBody?.contains("tool' role message must follow") == true ||
                                responseBody?.contains("tool_calls") == true) {
                                ApiErrorTracker.trackMessageSequencingError(
                                    endpoint = apiUrl,
                                    errorMessage = responseBody,
                                    messageHistory = messages.map { it.mapValues { entry -> entry.value.toString() } }
                                )
                            } else {
                                // Handle other 400 errors
                                ApiErrorTracker.trackStructuredApiError(
                                    endpoint = apiUrl,
                                    responseCode = response.code,
                                    responseBody = responseBody ?: "Unknown error",
                                    requestContext = mapOf(
                                        "model" to model,
                                        "message_count" to messages.size,
                                        "has_tools" to true
                                    )
                                )
                            }
                        }
                        402, 403 -> {
                            // Parse structured error if possible
                            try {
                                if (responseBody != null) {
                                    ApiErrorTracker.trackStructuredApiError(
                                        endpoint = apiUrl,
                                        responseCode = response.code,
                                        responseBody = responseBody,
                                        requestContext = mapOf(
                                            "model" to model,
                                            "message_count" to messages.size,
                                            "has_tools" to true
                                        )
                                    )
                                } else {
                                    ApiErrorTracker.trackQuotaError(
                                        endpoint = apiUrl,
                                        errorMessage = "HTTP ${response.code}",
                                        quotaType = "billing"
                                    )
                                }
                            } catch (e: Exception) {
                                ApiErrorTracker.trackQuotaError(
                                    endpoint = apiUrl,
                                    errorMessage = "HTTP ${response.code}",
                                    quotaType = "billing"
                                )
                            }
                        }
                        405 -> {
                            val errorReason = "Method Not Allowed - The API endpoint doesn't support POST requests. Check if the base URL is correct."
                            ApiErrorTracker.trackOpenAIError(
                                error = Exception("$errorReason (HTTP ${response.code})"),
                                endpoint = apiUrl,
                                requestBody = requestBody.take(500),
                                responseCode = response.code,
                                responseBody = responseBody,
                                apiKey = apiKey
                            )
                        }
                        404 -> {
                            val errorReason = "Not Found - The API endpoint doesn't exist. Check if the base URL and model are correct."
                            ApiErrorTracker.trackOpenAIError(
                                error = Exception("$errorReason (HTTP ${response.code})"),
                                endpoint = apiUrl,
                                requestBody = requestBody.take(500),
                                responseCode = response.code,
                                responseBody = responseBody,
                                apiKey = apiKey
                            )
                        }
                        else -> {
                            val commonErrors = mapOf(
                                500 to "Internal Server Error - The API service is experiencing issues",
                                502 to "Bad Gateway - Connection issues between client and server",
                                503 to "Service Unavailable - The API service is temporarily down",
                                504 to "Gateway Timeout - The API service is not responding"
                            )
                            val errorReason = commonErrors[response.code] ?: "Unknown error"
                            
                            ApiErrorTracker.trackOpenAIError(
                                error = Exception("$errorReason (HTTP ${response.code})"),
                                endpoint = apiUrl,
                                requestBody = requestBody.take(500),
                                responseCode = response.code,
                                responseBody = responseBody,
                                apiKey = apiKey
                            )
                        }
                    }
                    
                    val errorJson = JSONObject()
                    
                    // Provide more descriptive error messages
                    val errorReason = when (response.code) {
                        401 -> "Authentication Failed - Check your OpenAI API key"
                        402 -> "Payment Required - Check your OpenAI account billing"
                        403 -> "Forbidden - API key lacks necessary permissions"
                        404 -> "Not Found - Check if the base URL and endpoint are correct"
                        405 -> "Method Not Allowed - The API endpoint doesn't support POST requests"
                        429 -> "Rate Limit Exceeded - Too many requests, please wait"
                        500 -> "Internal Server Error - API service is experiencing issues"
                        502 -> "Bad Gateway - Connection issues between client and server"
                        503 -> "Service Unavailable - API service is temporarily down"
                        504 -> "Gateway Timeout - API service is not responding"
                        else -> "HTTP ${response.code}"
                    }
                    
                    errorJson.put("error", errorReason)
                    errorJson.put("http_code", response.code)
                    errorJson.put("body", responseBody ?: "null")
                    return@sentryApiCall errorJson
                }
                
                if (responseBody == null) {
                    android.util.Log.e("LLMClient", "Null response body")
                    
                    ApiErrorTracker.trackOpenAIError(
                        error = Exception("Null response body"),
                        endpoint = apiUrl,
                        requestBody = requestBody.take(500),
                        responseCode = response.code,
                        apiKey = apiKey
                    )
                    
                    val errorJson = JSONObject()
                    errorJson.put("error", "Null response body")
                    return@sentryApiCall errorJson
                }
                
                // Track successful API call
                ApiErrorTracker.trackApiSuccess(
                    endpoint = apiUrl,
                    responseCode = response.code,
                    responseTimeMs = duration,
                    requestSize = requestBody.length,
                    responseSize = responseBody.length
                )
                
                return@sentryApiCall JSONObject(responseBody)
            }
        }
    }
} 