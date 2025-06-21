package com.example.simple_agent_android.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsUtils {
    private const val OVERLAY_PREFS = "overlay_prefs"
    private const val AGENT_PREFS = "agent_prefs"
    private const val ONBOARDING_PREFS = "onboarding_prefs"
    private const val VERTICAL_OFFSET_KEY = "vertical_offset"
    private const val OPENAI_KEY = "openai_key"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

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

    private fun getSharedPrefs(context: Context, prefsName: String): SharedPreferences {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }
} 