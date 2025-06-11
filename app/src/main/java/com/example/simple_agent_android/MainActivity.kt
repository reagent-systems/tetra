package com.example.simple_agent_android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.simple_agent_android.ui.theme.SimpleAgentAndroidTheme
import androidx.compose.ui.unit.dp
import android.os.Build
import android.content.Context
import android.net.Uri
import com.example.simple_agent_android.BoundingBoxAccessibilityService
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.material3.OutlinedTextField
import com.example.simple_agent_android.agentcore.AgentOrchestrator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.simple_agent_android.ui.HomeScreen
import com.example.simple_agent_android.ui.DebugScreen
import com.example.simple_agent_android.ui.SidebarDrawer
import com.example.simple_agent_android.ui.SettingsScreen
import com.example.simple_agent_android.ui.AboutScreen

class MainActivity : ComponentActivity() {
    companion object {
        private val serverStarted = AtomicBoolean(false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!serverStarted.getAndSet(true)) {
            Thread {
                try {
                    AgentApiServer().start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
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
                            agentOutput = agentOutput
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleAgentAndroidTheme {
        Greeting("Android")
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