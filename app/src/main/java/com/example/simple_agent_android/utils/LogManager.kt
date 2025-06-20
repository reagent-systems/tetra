package com.example.simple_agent_android.utils

import androidx.compose.runtime.mutableStateOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LogManager {
    private val logBuffer = mutableStateOf("")
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = LocalDateTime.now().format(formatter)
        val logEntry = "[$timestamp] $level/$tag: $message\n"
        logBuffer.value += logEntry
    }

    fun getFullLog(): String = logBuffer.value

    fun clearLog() {
        logBuffer.value = ""
    }

    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }
} 