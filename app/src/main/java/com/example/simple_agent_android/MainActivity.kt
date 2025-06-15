package com.example.simple_agent_android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.simple_agent_android.ui.theme.SimpleAgentAndroidTheme
import android.os.Build
import android.content.Context
import android.net.Uri
import com.example.simple_agent_android.agentcore.AgentOrchestrator
import androidx.compose.foundation.layout.Box
import com.example.simple_agent_android.ui.HomeScreen
import com.example.simple_agent_android.ui.DebugScreen
import com.example.simple_agent_android.ui.SidebarDrawer
import com.example.simple_agent_android.ui.SettingsScreen
import com.example.simple_agent_android.ui.AboutScreen
import android.util.Log

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()
        setContent {
            SimpleAgentAndroidTheme {
                var drawerOpen by remember { mutableStateOf(false) }
                var selectedScreen by remember { mutableStateOf("home") }
                var agentRunning by remember { mutableStateOf(false) }
                var debugMenuExpanded by remember { mutableStateOf(false) }
                var overlayActive by remember { mutableStateOf(BoundingBoxAccessibilityService.isOverlayActive()) }
                var showBoxes by remember { mutableStateOf(true) }
                var verticalOffset by remember { mutableStateOf(BoundingBoxAccessibilityService.getVerticalOffset(this@MainActivity)) }
                val prefs = getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)
                var openAiKey by remember { mutableStateOf(prefs.getString("openai_key", "") ?: "") }
                var keyVisible by remember { mutableStateOf(false) }
                var agentInput by remember { mutableStateOf("") }
                var showJsonDialog by remember { mutableStateOf(false) }
                var jsonOutput by remember { mutableStateOf("") }
                var settingsSaved by remember { mutableStateOf(false) }
                var agentOutput by remember { mutableStateOf("") }
                var floatingUiEnabled by remember { mutableStateOf(false) }

                SidebarDrawer(
                    drawerOpen = drawerOpen,
                    onDrawerOpen = { drawerOpen = true },
                    onDrawerClose = { drawerOpen = false },
                    selectedScreen = selectedScreen,
                    onSelectScreen = {
                        selectedScreen = it
                        drawerOpen = false
                        if (it != "settings") settingsSaved = false
                    },
                ) {
                    when (selectedScreen) {
                        "home" -> HomeScreen(
                            agentRunning = agentRunning,
                            onStartAgent = {
                                if (!hasOverlayPermission(this@MainActivity)) {
                                    requestOverlayPermission(this@MainActivity)
                                } else {
                                    if (!BoundingBoxAccessibilityService.isOverlayActive()) {
                                        BoundingBoxAccessibilityService.startOverlay(false)
                                    } else {
                                        BoundingBoxAccessibilityService.setOverlayEnabled(false)
                                    }
                                    agentRunning = true
                                    agentOutput = ""
                                    AgentOrchestrator.runAgent(
                                        instruction = agentInput,
                                        apiKey = openAiKey,
                                        context = this@MainActivity,
                                        onAgentStopped = { agentRunning = false },
                                        onOutput = { output ->
                                            agentOutput += output + "\n"
                                        }
                                    )
                                }
                            },
                            onStopAgent = {
                                AgentOrchestrator.stopAgent()
                                agentRunning = false
                            },
                            agentInput = agentInput,
                            onAgentInputChange = { agentInput = it },
                            onEnableAccessibility = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            },
                            agentOutput = agentOutput,
                            floatingUiEnabled = floatingUiEnabled,
                            onToggleFloatingUi = {
                                Log.d(TAG, "Toggle floating UI requested, current state: $floatingUiEnabled")
                                if (!hasOverlayPermission(this@MainActivity)) {
                                    Log.d(TAG, "No overlay permission, requesting...")
                                    requestOverlayPermission(this@MainActivity)
                                } else {
                                    try {
                                        floatingUiEnabled = !floatingUiEnabled
                                        Log.d(TAG, "New floating UI state: $floatingUiEnabled")
                                        if (floatingUiEnabled) {
                                            Log.d(TAG, "Showing floating button")
                                            BoundingBoxAccessibilityService.showFloatingButton()
                                        } else {
                                            Log.d(TAG, "Hiding floating button")
                                            BoundingBoxAccessibilityService.hideFloatingButton()
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error toggling floating UI", e)
                                        floatingUiEnabled = false
                                    }
                                }
                            }
                        )
                        "debug" -> DebugScreen(
                            showBoxes = showBoxes,
                            onToggleBoxes = {
                                showBoxes = !showBoxes
                                BoundingBoxAccessibilityService.setOverlayEnabled(showBoxes)
                            },
                            overlayActive = overlayActive,
                            onToggleOverlay = {
                                if (!hasOverlayPermission(this@MainActivity)) {
                                    requestOverlayPermission(this@MainActivity)
                                } else {
                                    if (BoundingBoxAccessibilityService.isOverlayActive()) {
                                        BoundingBoxAccessibilityService.stopOverlay()
                                        overlayActive = false
                                    } else {
                                        BoundingBoxAccessibilityService.startOverlay(showBoxes)
                                        overlayActive = true
                                    }
                                }
                            },
                            verticalOffset = verticalOffset,
                            onVerticalOffsetChange = {
                                verticalOffset = it
                                BoundingBoxAccessibilityService.setVerticalOffset(this@MainActivity, verticalOffset)
                            },
                            onExportJson = {
                                jsonOutput = BoundingBoxAccessibilityService.getInteractiveElementsJson()
                                showJsonDialog = true
                            },
                            jsonOutput = if (showJsonDialog) jsonOutput else null,
                            onCloseJson = { showJsonDialog = false }
                        )
                        "settings" -> SettingsScreen(
                            openAiKey = openAiKey,
                            onOpenAiKeyChange = { openAiKey = it },
                            onSave = {
                                prefs.edit().putString("openai_key", openAiKey).apply()
                                settingsSaved = true
                            },
                            saved = settingsSaved
                        )
                        "about" -> AboutScreen()
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName)
        )
        context.startActivity(intent)
    }
}