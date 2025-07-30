package com.example.simple_agent_android.network

import android.util.Log
import com.example.simple_agent_android.data.FeedbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FeedbackApi {
    private val TAG = "FeedbackApi"
    private val feedbackUrl = "https://fb-sa-a.bentlybro.com/feedback"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendFeedback(feedback: FeedbackRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending feedback to: $feedbackUrl")
            
            val url = URL(feedbackUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val jsonString = json.encodeToString(feedback)
            Log.d(TAG, "Feedback JSON: $jsonString")

            // Write the JSON data
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Feedback sent successfully: $response")
                Result.success("Feedback sent successfully!")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "Failed to send feedback. Response code: $responseCode, Error: $errorResponse")
                Result.failure(Exception("Failed to send feedback: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending feedback", e)
            Result.failure(e)
        }
    }
} 