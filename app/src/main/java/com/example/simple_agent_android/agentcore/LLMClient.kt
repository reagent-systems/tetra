package com.example.simple_agent_android.agentcore

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LLMClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val model = "gpt-4o"

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
        )
    ))

    fun sendWithTools(messages: List<Map<String, String>>): JSONObject? {
        try {
            val json = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray(messages.map { JSONObject(it) }))
                put("tools", tools)
                put("tool_choice", "auto")
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            android.util.Log.i("LLMClient", "Request payload: ${json.toString().take(1000)}")
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                android.util.Log.i("LLMClient", "HTTP status: ${response.code}")
                android.util.Log.i("LLMClient", "Response body: ${responseBody?.take(1000)}")
                if (!response.isSuccessful) {
                    android.util.Log.e("LLMClient", "Unsuccessful response: ${response.code} ${responseBody}")
                    val errorJson = JSONObject()
                    errorJson.put("error", "HTTP ${response.code}")
                    errorJson.put("body", responseBody ?: "null")
                    return errorJson
                }
                if (responseBody == null) {
                    android.util.Log.e("LLMClient", "Null response body")
                    val errorJson = JSONObject()
                    errorJson.put("error", "Null response body")
                    return errorJson
                }
                return JSONObject(responseBody)
            }
        } catch (e: Exception) {
            android.util.Log.e("LLMClient", "Exception in sendWithTools", e)
            val errorJson = JSONObject()
            errorJson.put("error", e.toString())
            return errorJson
        }
    }
} 