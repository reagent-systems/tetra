package com.example.simple_agent_android.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.simple_agent_android.MainActivity
import com.example.simple_agent_android.R

class AgentNotificationManager(private val context: Context) {
    private val TAG = "AgentNotificationManager"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "agent_control_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_AGENT = "com.example.simple_agent_android.START_AGENT"
        const val ACTION_STOP_AGENT = "com.example.simple_agent_android.STOP_AGENT"
        const val ACTION_VOICE_INPUT = "com.example.simple_agent_android.VOICE_INPUT"
        const val ACTION_OPEN_APP = "com.example.simple_agent_android.OPEN_APP"
        const val EXTRA_VOICE_REPLY = "voice_reply"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agent Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for agent control"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    fun showPersistentNotification(agentRunning: Boolean = false) {
        try {
            val notification = buildNotification(agentRunning)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Persistent notification shown, agentRunning: $agentRunning")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing persistent notification", e)
        }
    }
    
    fun updateNotification(agentRunning: Boolean) {
        showPersistentNotification(agentRunning)
    }
    
    fun hidePersistentNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Persistent notification hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding persistent notification", e)
        }
    }
    
    private fun buildNotification(agentRunning: Boolean): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Simple Agent")
            .setContentText(if (agentRunning) "Agent is running" else "Agent ready")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setColor(context.getColor(R.color.reagent_green))
        
        if (agentRunning) {
            // Agent is running - show stop action
            val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_STOP_AGENT
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                R.drawable.ic_stop,
                "Stop Agent",
                stopPendingIntent
            )
        } else {
            // Agent is not running - show input actions
            
            // Text input action with RemoteInput
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Enter agent instruction...")
                .build()
            
            val textInputIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_START_AGENT
            }
            val textInputPendingIntent = PendingIntent.getBroadcast(
                context, 2, textInputIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            
            val textInputAction = NotificationCompat.Action.Builder(
                R.drawable.ic_edit,
                "Type Message",
                textInputPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()
            
            // Voice input action
            val voiceInputIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_VOICE_INPUT
            }
            val voiceInputPendingIntent = PendingIntent.getBroadcast(
                context, 3, voiceInputIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                R.drawable.ic_mic,
                "Voice Input",
                voiceInputPendingIntent
            )
            builder.addAction(textInputAction)
        }
        
        return builder.build()
    }
    
    fun isNotificationActive(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNotifications = notificationManager.activeNotifications
                activeNotifications.any { it.id == NOTIFICATION_ID }
            } else {
                // For older versions, we can't easily check, so assume it's active if we think it should be
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification status", e)
            false
        }
    }
} 