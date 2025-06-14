package com.example.simple_agent_android.agentcore.metacognition

import android.content.Context
import com.example.simple_agent_android.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Prompts {
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private fun getString(resId: Int): String {
        return applicationContext?.getString(resId) ?: ""
    }

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return applicationContext?.getString(resId, *formatArgs) ?: ""
    }

    val systemPrompt: String
        get() = getString(R.string.agent_system_prompt)

    val planningPrompt: String
        get() = getString(R.string.agent_planning_prompt)

    val planningJsonFormat: String
        get() = getString(R.string.agent_planning_json_format)

    val reflectionPrompt: String
        get() = getString(R.string.agent_reflection_prompt)

    val reflectionJsonFormat: String
        get() = getString(R.string.agent_reflection_json_format)

    val stoppingPrompt: String
        get() = getString(R.string.agent_stopping_prompt)

    val stoppingJsonFormat: String
        get() = getString(R.string.agent_stopping_json_format)

    val loopBreakingPrompt: String
        get() = getString(R.string.agent_loop_breaking_prompt)

    val loopBreakingDecisionFormat: String
        get() = getString(R.string.agent_loop_breaking_decision_format)

    val autoContinueInfinite: String
        get() = getString(R.string.agent_auto_continue_infinite)

    val autoContinueLimited: String
        get() = getString(R.string.agent_auto_continue_limited)

    val manualMode: String
        get() = getString(R.string.agent_manual_mode)

    val toolCallGuidance: String
        get() = getString(R.string.agent_tool_call_guidance)

    fun getDateReminder(): String {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getString(R.string.agent_date_reminder, currentDateTime)
    }

    fun getDateCorrection(): String {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val currentYear = LocalDateTime.now().year
        return getString(R.string.agent_date_correction, currentDateTime, currentYear)
    }
} 