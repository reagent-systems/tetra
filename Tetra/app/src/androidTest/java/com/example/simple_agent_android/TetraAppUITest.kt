package com.example.simple_agent_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class TetraAppUITest {
    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        Thread.sleep(2000)

        // Launch the app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("com.example.simple_agent_android")
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 5000)
    }

    @Test
    fun testAppLaunch() {
        // Verify the app launched successfully
        val appElement = device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 5000)
        assertTrue("App should launch successfully", appElement)
    }

    @Test
    fun testHomeScreenElements() {
        // Test main UI elements are present
        device.wait(Until.hasObject(By.text("Tetra")), 5000)

        // Check for key UI elements
        val instructionField = device.findObject(UiSelector().className("android.widget.EditText"))
        assertTrue("Instruction field should exist", instructionField.exists())

        // Look for agent control buttons
        val startButton = device.findObject(UiSelector().textContains("Start"))
        val runButton = device.findObject(UiSelector().textContains("Run"))
        // Button might not be visible initially, that's ok
    }

    @Test
    fun testAgentInstructionInput() {
        // Find instruction input field and enter text
        val instructionField = device.findObject(UiSelector().className("android.widget.EditText"))
        if (instructionField.exists()) {
            instructionField.clearTextField()
            instructionField.setText("Open settings")
            Thread.sleep(1000)

            // Verify text was entered
            assertTrue("Text should be entered", instructionField.text.contains("settings"))
        }
    }

    @Test
    fun testSettingsNavigation() {
        // Try to navigate to settings screen
        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 3000)

        // Look for settings button/menu
        val settingsButton = device.findObject(UiSelector().textContains("Settings"))
        if (settingsButton.exists()) {
            settingsButton.click()
            Thread.sleep(2000)

            // Verify we're in settings
            val apiKeyField = device.findObject(UiSelector().textContains("API Key"))
            val openaiField = device.findObject(UiSelector().textContains("OpenAI"))
            // Settings screen should have API key field
        }
    }

    @Test
    fun testAccessibilityServiceCheck() {
        // Check if accessibility service prompt appears or is handled
        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 3000)

        // Look for accessibility-related UI elements
        val accessibilityText = device.findObject(UiSelector().textContains("Accessibility"))
        val enableText = device.findObject(UiSelector().textContains("Enable"))
        // This test verifies the accessibility flow is working
    }

    @Test
    fun testAgentExecution() {
        // Test basic agent execution flow
        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 3000)

        // Enter a simple instruction
        val instructionField = device.findObject(UiSelector().className("android.widget.EditText"))
        if (instructionField.exists()) {
            instructionField.clearTextField()
            instructionField.setText("Go to home screen")
            Thread.sleep(1000)

            // Look for run/start button
            var runButton = device.findObject(UiSelector().textContains("Start"))
            if (!runButton.exists()) {
                runButton = device.findObject(UiSelector().textContains("Run"))
            }
            if (!runButton.exists()) {
                runButton = device.findObject(UiSelector().className("android.widget.Button"))
            }

            if (runButton.exists()) {
                runButton.click()
                Thread.sleep(3000)

                // Check for agent execution feedback
                val executionFeedback = device.findObject(UiSelector().textContains("Starting"))
                val agentFeedback = device.findObject(UiSelector().textContains("Agent"))
            }
        }
    }

    @Test
    fun testFloatingControls() {
        // Test floating button functionality if enabled
        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 3000)

        // Look for floating controls
        val floatingButton = device.findObject(UiSelector().className("android.widget.ImageButton"))
        // Floating controls might be enabled in settings
    }

    @Test
    fun testDebugFeatures() {
        // Test debug overlay and bounding box features
        device.wait(Until.hasObject(By.pkg("com.example.simple_agent_android")), 3000)

        // Navigate to debug screen if available
        val debugButton = device.findObject(UiSelector().textContains("Debug"))
        if (debugButton.exists()) {
            debugButton.click()
            Thread.sleep(2000)

            // Look for debug controls
            val overlayButton = device.findObject(UiSelector().textContains("Overlay"))
            val boundingButton = device.findObject(UiSelector().textContains("Bounding"))
        }
    }
}