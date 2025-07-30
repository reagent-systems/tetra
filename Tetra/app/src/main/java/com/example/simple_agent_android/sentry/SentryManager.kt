package com.example.simple_agent_android.sentry

import android.content.Context
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.User
import io.sentry.Breadcrumb
import io.sentry.SentryEvent
import io.sentry.Hint
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import java.util.Date

object SentryManager {
    private const val TAG = "SentryManager"
    
    /**
     * Initialize Sentry with custom configuration
     */
    fun initialize(context: Context) {
        SentryAndroid.init(context) { options ->
            // DSN is automatically read from AndroidManifest.xml metadata
            // Do not override options.dsn - let it use the manifest value
            options.isDebug = true // Set to false in production
            options.environment = if (isDebugBuild()) "development" else "production"
            options.release = getAppVersion(context)
            
            // Performance monitoring
            options.tracesSampleRate = 1.0 // Capture 100% of transactions for development
            
            // Session tracking
            options.isEnableAutoSessionTracking = true
            
            // Add custom before send callback
            options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                // Add custom logic here if needed
                addCustomTags(event)
                event
            }
        }
        
        // Set user context
        setUserContext()
        
        addBreadcrumb("SentryManager initialized", SentryLevel.INFO)
    }
    
    /**
     * Set user context for better error tracking
     */
    private fun setUserContext() {
        val user = User()
        user.id = "user_${System.currentTimeMillis()}" // Generate unique user ID
        user.username = "android_user"
        Sentry.setUser(user)
    }
    
    /**
     * Add custom tags to events
     */
    private fun addCustomTags(event: SentryEvent) {
        event.setTag("component", "simple_agent")
        event.setTag("platform", "android")
    }
    
    /**
     * Log general exceptions with context
     */
    fun captureException(
        throwable: Throwable,
        message: String? = null,
        level: SentryLevel = SentryLevel.ERROR,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any> = emptyMap()
    ) {
        Sentry.withScope { scope ->
            // Add custom message
            message?.let { scope.setExtra("custom_message", it) }
            
            // Add tags
            tags.forEach { (key, value) ->
                scope.setTag(key, value)
            }
            
            // Add extra data
            extras.forEach { (key, value) ->
                scope.setExtra(key, value.toString())
            }
            
            // Set level
            scope.level = level
            
            // Capture the exception
            Sentry.captureException(throwable)
        }
    }
    
    /**
     * Log custom messages without exceptions
     */
    fun captureMessage(
        message: String,
        level: SentryLevel = SentryLevel.INFO,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any> = emptyMap()
    ) {
        Sentry.withScope { scope ->
            // Add tags
            tags.forEach { (key, value) ->
                scope.setTag(key, value)
            }
            
            // Add extra data
            extras.forEach { (key, value) ->
                scope.setExtra(key, value.toString())
            }
            
            // Set level
            scope.level = level
            
            // Capture the message
            Sentry.captureMessage(message)
        }
    }
    
    /**
     * Add breadcrumb for tracking user actions
     */
    fun addBreadcrumb(
        message: String,
        level: SentryLevel = SentryLevel.INFO,
        category: String = "user_action",
        data: Map<String, Any> = emptyMap()
    ) {
        val breadcrumb = Breadcrumb()
        breadcrumb.message = message
        breadcrumb.level = level
        breadcrumb.category = category
        
        Sentry.addBreadcrumb(breadcrumb)
    }
    
    /**
     * Start a transaction for performance monitoring
     */
    fun startTransaction(name: String, operation: String) = Sentry.startTransaction(name, operation)
    
    /**
     * Set custom context for the current scope
     */
    fun setContext(key: String, value: Any) {
        Sentry.setExtra(key, value.toString())
    }
    
    /**
     * Set custom tag for the current scope
     */
    fun setTag(key: String, value: String) {
        Sentry.setTag(key, value)
    }
    
    /**
     * Clear current scope
     */
    fun clearScope() {
        Sentry.popScope()
    }
    
    // Utility methods
    private fun isDebugBuild(): Boolean {
        return try {
            Class.forName("com.example.simple_agent_android.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getAppVersionName(): String {
        return try {
            Class.forName("com.example.simple_agent_android.BuildConfig")
                .getField("VERSION_NAME")
                .get(null) as String
        } catch (e: Exception) {
            "unknown"
        }
    }
} 