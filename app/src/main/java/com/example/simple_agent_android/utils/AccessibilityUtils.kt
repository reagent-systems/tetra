package com.example.simple_agent_android.utils

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object AccessibilityUtils {
    
    /**
     * Check if our accessibility service is actually enabled in system settings
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/com.example.simple_agent_android.accessibility.service.BoundingBoxAccessibilityService"
        
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if accessibility services are enabled at all on the device
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        return accessibilityEnabled == 1
    }
} 