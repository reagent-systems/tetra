package com.example.simple_agent_android

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidWorldBenchmarkTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    companion object {
        private const val PACKAGE_NAME = "com.example.simple_agent_android"
        private const val TIMEOUT = 10000L

        // Sample AndroidWorld benchmark commands
        private val BENCHMARK_COMMANDS = listOf(
            "Open the calculator app and calculate 2+2",
            "Open settings and enable airplane mode",
            "Open contacts and add a new contact named John Doe",
            "Open camera and take a photo",
            "Open calendar and create an event for tomorrow"
        )
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        device.wakeUp()
        device.pressHome()
    }

    private fun launchApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.wait(Until.hasObject(UiSelector().packageName(PACKAGE_NAME)), TIMEOUT)
    }

    private fun inputApiKey(apiKey: String) {
        try {
            // Look for API key input field
            val apiKeyField = device.findObject(
                UiSelector()
                    .className("android.widget.EditText")
                    .textContains("API Key")
            )

            if (apiKeyField.exists()) {
                apiKeyField.click()
                device.waitForIdle()
                apiKeyField.clearTextField()
                apiKeyField.text = apiKey
                device.waitForIdle()
            } else {
                // Alternative: look for settings button
                val settingsButton = device.findObject(
                    UiSelector()
                        .descriptionContains("Settings")
                        .or(UiSelector().textContains("Settings"))
                )

                if (settingsButton.exists()) {
                    settingsButton.click()
                    device.waitForIdle()

                    val apiField = device.findObject(
                        UiSelector()
                            .className("android.widget.EditText")
                            .instance(0)
                    )

                    if (apiField.exists()) {
                        apiField.click()
                        apiField.clearTextField()
                        apiField.text = apiKey
                    }
                }
            }
        } catch (e: UiObjectNotFoundException) {
            println("Could not find API key input field: ${e.message}")
        }
    }

    private fun executeCommand(command: String) {
        try {
            // Look for command input field
            val commandField = device.findObject(
                UiSelector()
                    .className("android.widget.EditText")
                    .descriptionContains("command")
                    .or(UiSelector().className("android.widget.EditText").textContains("Enter"))
            )

            if (commandField.exists()) {
                commandField.click()
                device.waitForIdle()
                commandField.clearTextField()
                commandField.text = command
                device.waitForIdle()

                // Look for execute/run button
                val executeButton = device.findObject(
                    UiSelector()
                        .textMatches("Run|Execute|Start|Go")
                        .className("android.widget.Button")
                        .or(UiSelector().descriptionMatches("Run|Execute|Start|Go"))
                )

                if (executeButton.exists()) {
                    executeButton.click()
                    device.waitForIdle()
                }
            }
        } catch (e: UiObjectNotFoundException) {
            println("Could not execute command: ${e.message}")
        }
    }

    private fun waitForCompletion(): Boolean {
        // Wait for agent to complete task (max 60 seconds)
        val startTime = System.currentTimeMillis()
        val maxWaitTime = 60000L

        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                // Look for completion indicators
                val completionIndicator = device.findObject(
                    UiSelector()
                        .textMatches(".*[Cc]ompleted?.*|.*[Ff]inished.*|.*[Dd]one.*|.*[Ss]uccess.*")
                        .or(UiSelector().descriptionMatches(".*[Cc]ompleted?.*|.*[Ff]inished.*|.*[Dd]one.*|.*[Ss]uccess.*"))
                )

                if (completionIndicator.exists()) {
                    return true
                }

                // Check if agent is still running
                val runningIndicator = device.findObject(
                    UiSelector()
                        .textMatches(".*[Rr]unning.*|.*[Ee]xecuting.*|.*[Pp]rocessing.*")
                )

                if (!runningIndicator.exists()) {
                    // Agent might have stopped - check for error or completion
                    Thread.sleep(2000)
                    break
                }

                Thread.sleep(1000)
            } catch (e: Exception) {
                println("Error waiting for completion: ${e.message}")
                break
            }
        }

        return false
    }

    @Test
    fun testBenchmarkExecution() {
        // Get API key from instrumentation arguments or environment
        val apiKey = InstrumentationRegistry.getArguments().getString("api_key")
            ?: System.getenv("OPENAI_API_KEY")
            ?: "your_api_key_here"

        // Launch the app
        launchApp()
        Thread.sleep(3000)

        // Input API key
        inputApiKey(apiKey)
        Thread.sleep(2000)

        // Execute first benchmark command
        val testCommand = BENCHMARK_COMMANDS.first()
        executeCommand(testCommand)

        // Wait for completion
        val completed = waitForCompletion()

        if (completed) {
            println("Benchmark test completed successfully: $testCommand")
        } else {
            println("Benchmark test may have failed or timed out: $testCommand")
        }

        // Take screenshot for debugging
        device.takeScreenshot(java.io.File("/sdcard/benchmark_result.png"))
    }

    @Test
    fun testMultipleBenchmarks() {
        val apiKey = InstrumentationRegistry.getArguments().getString("api_key")
            ?: System.getenv("OPENAI_API_KEY")
            ?: "your_api_key_here"

        launchApp()
        Thread.sleep(3000)
        inputApiKey(apiKey)

        // Test multiple benchmark commands
        for ((index, command) in BENCHMARK_COMMANDS.take(3).withIndex()) {
            println("Executing benchmark ${index + 1}: $command")

            executeCommand(command)
            val completed = waitForCompletion()

            if (completed) {
                println("Benchmark ${index + 1} completed successfully")
            } else {
                println("Benchmark ${index + 1} failed or timed out")
            }

            // Reset for next command
            device.pressHome()
            launchApp()
            Thread.sleep(2000)
        }
    }

    @Test
    fun testSpecificCoordinateInteraction() {
        launchApp()
        Thread.sleep(3000)

        // Test coordinate-based interaction
        // These coordinates should be adjusted based on your UI layout
        val centerX = device.displayWidth / 2
        val centerY = device.displayHeight / 2

        // Click at center of screen
        device.click(centerX, centerY)
        device.waitForIdle()

        // Click at specific coordinates for command input
        // You'll need to adjust these based on your UI
        device.click(centerX, centerY + 200)
        device.waitForIdle()

        println("Coordinate-based interaction test completed")
    }
}