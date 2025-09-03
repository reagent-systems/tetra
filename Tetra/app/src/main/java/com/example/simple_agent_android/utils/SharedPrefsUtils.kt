package com.example.simple_agent_android.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsUtils {
    private const val OVERLAY_PREFS = "overlay_prefs"
    private const val AGENT_PREFS = "agent_prefs"
    private const val ONBOARDING_PREFS = "onboarding_prefs"
    private const val VERTICAL_OFFSET_KEY = "vertical_offset"
    private const val OPENAI_KEY = "openai_key"
    private const val OPENAI_BASE_URL = "openai_base_url"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private const val COMPLETION_SCREEN_ENABLED_KEY = "completion_screen_enabled"

    fun getVerticalOffset(context: Context): Int {
        return getSharedPrefs(context, OVERLAY_PREFS)
            .getInt(VERTICAL_OFFSET_KEY, 0)
    }

    fun setVerticalOffset(context: Context, offset: Int) {
        getSharedPrefs(context, OVERLAY_PREFS)
            .edit()
            .putInt(VERTICAL_OFFSET_KEY, offset)
            .apply()
    }

    fun getOpenAIKey(context: Context): String {
        return getSharedPrefs(context, AGENT_PREFS)
            .getString(OPENAI_KEY, "") ?: ""
    }

    fun setOpenAIKey(context: Context, key: String) {
        getSharedPrefs(context, AGENT_PREFS)
            .edit()
            .putString(OPENAI_KEY, key)
            .apply()
    }

    fun getOpenAIBaseUrl(context: Context): String {
        return getSharedPrefs(context, AGENT_PREFS)
            .getString(OPENAI_BASE_URL, "https://api.openai.com") ?: "https://api.openai.com"
    }

    fun setOpenAIBaseUrl(context: Context, baseUrl: String) {
        val cleanUrl = if (baseUrl.isBlank()) {
            "https://api.openai.com"
        } else {
            baseUrl.trimEnd('/')
        }
        getSharedPrefs(context, AGENT_PREFS)
            .edit()
            .putString(OPENAI_BASE_URL, cleanUrl)
            .apply()
    }

    fun hasCompletedOnboarding(context: Context): Boolean {
        return getSharedPrefs(context, ONBOARDING_PREFS)
            .getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getSharedPrefs(context, ONBOARDING_PREFS)
            .edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, completed)
            .apply()
    }

    fun isCompletionScreenEnabled(context: Context): Boolean {
        return getSharedPrefs(context, AGENT_PREFS)
            .getBoolean(COMPLETION_SCREEN_ENABLED_KEY, true) // Default to enabled
    }

    fun setCompletionScreenEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context, AGENT_PREFS)
            .edit()
            .putBoolean(COMPLETION_SCREEN_ENABLED_KEY, enabled)
            .apply()
    }

    private fun getSharedPrefs(context: Context, prefsName: String): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
} 