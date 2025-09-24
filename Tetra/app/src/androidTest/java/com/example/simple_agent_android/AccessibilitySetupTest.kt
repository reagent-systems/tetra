package com.example.simple_agent_android

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilitySetupTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()

        // Wake up the device
        device.wakeUp()
        device.pressHome()
    }

    @Test
    fun testEnableAccessibilityService() {
        // Open accessibility settings
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        // Wait for settings to load
        device.waitForIdle()
        Thread.sleep(2000)

        try {
            // Look for our accessibility service
            val accessibilityService = device.findObject(
                UiSelector()
                    .textContains("Simple Agent")
                    .className("android.widget.TextView")
            )

            if (accessibilityService.exists()) {
                accessibilityService.click()
                device.waitForIdle()

                // Look for toggle switch
                val toggle = device.findObject(
                    UiSelector()
                        .className("android.widget.Switch")
                        .instance(0)
                )

                if (toggle.exists() && !toggle.isChecked) {
                    toggle.click()
                    device.waitForIdle()

                    // Confirm enabling if dialog appears
                    val okButton = device.findObject(
                        UiSelector()
                            .textMatches("OK|Allow|Enable")
                            .className("android.widget.Button")
                    )

                    if (okButton.exists()) {
                        okButton.click()
                        device.waitForIdle()
                    }
                }
            }
        } catch (e: UiObjectNotFoundException) {
            // Service might already be enabled or not found
            println("Accessibility service not found or already enabled: ${e.message}")
        }

        // Go back to home
        device.pressHome()
    }

    @Test
    fun testOverlayPermission() {
        // Open overlay permission settings
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)

        device.waitForIdle()
        Thread.sleep(2000)

        try {
            // Look for our app in the list
            val appName = device.findObject(
                UiSelector()
                    .textContains("Simple Agent")
                    .className("android.widget.TextView")
            )

            if (appName.exists()) {
                appName.click()
                device.waitForIdle()

                // Look for toggle switch
                val toggle = device.findObject(
                    UiSelector()
                        .className("android.widget.Switch")
                        .instance(0)
                )

                if (toggle.exists() && !toggle.isChecked) {
                    toggle.click()
                    device.waitForIdle()
                }
            }
        } catch (e: UiObjectNotFoundException) {
            println("Overlay permission not found: ${e.message}")
        }

        device.pressHome()
    }
}