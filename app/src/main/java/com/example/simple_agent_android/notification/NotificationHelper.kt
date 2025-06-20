package com.example.simple_agent_android.notification

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object NotificationHelper {
    
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        // Ensure we're on the main thread for Toast
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, duration).show()
        }
    }
} 