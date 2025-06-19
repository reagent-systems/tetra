package com.example.simple_agent_android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.simple_agent_android.agentcore.AgentOrchestrator
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.ui.AboutScreen
import com.example.simple_agent_android.ui.DebugScreen
import com.example.simple_agent_android.ui.HomeScreen
import com.example.simple_agent_android.ui.SettingsScreen
import com.example.simple_agent_android.ui.SidebarDrawer
import com.example.simple_agent_android.ui.theme.ReagentDark
import com.example.simple_agent_android.ui.theme.ReagentWhite
import com.example.simple_agent_android.ui.theme.SimpleAgentAndroidTheme
import com.example.simple_agent_android.utils.OverlayPermissionUtils
import com.example.simple_agent_android.utils.SharedPrefsUtils
import com.example.simple_agent_android.utils.UpdateUtils
import com.example.simple_agent_android.utils.NotificationUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var updateUtils: UpdateUtils
    private var updateDialogState = mutableStateOf<UpdateUtils.UpdateCheckResult?>(null)
    private var showUpdateDialog = mutableStateOf(false)

    // Add permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications permission is required for agent status updates", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()

        // Request notification permission
        checkAndRequestNotificationPermission()

        updateUtils = UpdateUtils(this)

        // Check for updates on app startup
        lifecycleScope.launch {
            checkForUpdates()
        }

        setContent {
            SimpleAgentAndroidTheme {
                var drawerOpen by remember { mutableStateOf(false) }
                var selectedScreen by remember { mutableStateOf("home") }
                var agentRunning by remember { mutableStateOf(false) }
                var overlayActive by remember { mutableStateOf(BoundingBoxAccessibilityService.isOverlayActive()) }
                var showBoxes by remember { mutableStateOf(true) }
                var verticalOffset by remember { mutableStateOf(SharedPrefsUtils.getVerticalOffset(this@MainActivity)) }
                val prefs = getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)
                var openAiKey by remember { mutableStateOf(prefs.getString("openai_key", "") ?: "") }
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
                                if (!OverlayPermissionUtils.hasOverlayPermission(this@MainActivity)) {
                                    OverlayPermissionUtils.requestOverlayPermission(this@MainActivity)
                                } else {
                                    if (!BoundingBoxAccessibilityService.isOverlayActive()) {
                                        BoundingBoxAccessibilityService.startOverlay(false)
                                    } else {
                                        BoundingBoxAccessibilityService.setOverlayEnabled(false)
                                    }
                                    agentOutput = ""
                                    AgentStateManager.startAgent(
                                        instruction = agentInput,
                                        apiKey = openAiKey,
                                        appContext = this@MainActivity,
                                        onOutput = { output ->
                                            agentOutput += output + "\n"
                                        }
                                    )
                                }
                            },
                            onStopAgent = {
                                AgentStateManager.stopAgent()
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
                                if (!OverlayPermissionUtils.hasOverlayPermission(this@MainActivity)) {
                                    Log.d(TAG, "No overlay permission, requesting...")
                                    OverlayPermissionUtils.requestOverlayPermission(this@MainActivity)
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
                                if (!OverlayPermissionUtils.hasOverlayPermission(this@MainActivity)) {
                                    OverlayPermissionUtils.requestOverlayPermission(this@MainActivity)
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
                                SharedPrefsUtils.setVerticalOffset(this@MainActivity, verticalOffset)
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
                            saved = settingsSaved,
                            onCheckForUpdates = {
                                lifecycleScope.launch {
                                    checkForUpdates(showAlways = true)
                                }
                            }
                        )
                        "about" -> AboutScreen()
                        else -> {}
                    }

                    // Update dialog
                    UpdateDialog()
                }
            }
        }
    }

    @Composable
    private fun UpdateDialog() {
        var updateResult by remember { updateDialogState }
        var showDialog by remember { showUpdateDialog }
        var showUpdateLog by remember { mutableStateOf(false) }
        var downloadStatus by remember { mutableStateOf<UpdateUtils.DownloadStatus>(UpdateUtils.DownloadStatus.NotStarted) }
        val context = LocalContext.current

        updateResult?.let { result ->
            if (result is UpdateUtils.UpdateCheckResult.UpdateAvailable && showDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        if (downloadStatus !is UpdateUtils.DownloadStatus.InProgress) {
                            showDialog = false 
                            updateResult = null
                            showUpdateLog = false
                            downloadStatus = UpdateUtils.DownloadStatus.NotStarted
                        }
                    },
                    title = { Text("Update Available") },
                    text = { 
                        Column {
                            Text("A new version of Simple Agent is available.")
                            Text("Current version: ${result.currentVersion}")
                            Text("New version: ${result.newVersion}")
                            
                            Spacer(modifier = Modifier.heightIn(8.dp))
                            
                            when (downloadStatus) {
                                is UpdateUtils.DownloadStatus.NotStarted -> {
                                    TextButton(
                                        onClick = { showUpdateLog = !showUpdateLog },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (showUpdateLog) "Hide Update Log" else "Show Update Log")
                                    }
                                }
                                is UpdateUtils.DownloadStatus.InProgress -> {
                                    val progress = (downloadStatus as UpdateUtils.DownloadStatus.InProgress).progress
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Downloading update: $progress%")
                                        LinearProgressIndicator(
                                            progress = progress / 100f,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                is UpdateUtils.DownloadStatus.Failed -> {
                                    val error = (downloadStatus as UpdateUtils.DownloadStatus.Failed).error
                                    Text(
                                        text = "Download failed: $error",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                UpdateUtils.DownloadStatus.Completed -> {
                                    Text("Download completed. Installing...")
                                }
                            }
                            
                            if (showUpdateLog && downloadStatus is UpdateUtils.DownloadStatus.NotStarted) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp),
                                    colors = CardDefaults.cardColors(containerColor = ReagentDark)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        val scrollState = rememberScrollState()
                                        Text(
                                            text = result.updateLog,
                                            color = ReagentWhite,
                                            modifier = Modifier.verticalScroll(scrollState)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (downloadStatus is UpdateUtils.DownloadStatus.NotStarted || downloadStatus is UpdateUtils.DownloadStatus.Failed) {
                            TextButton(
                                onClick = {
                                    lifecycleScope.launch {
                                        updateUtils.downloadAndInstallUpdate().collect { status ->
                                            downloadStatus = status
                                            if (status is UpdateUtils.DownloadStatus.Completed) {
                                                showDialog = false
                                                updateResult = null
                                                showUpdateLog = false
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(if (downloadStatus is UpdateUtils.DownloadStatus.Failed) "Retry" else "Update")
                            }
                        }
                    },
                    dismissButton = {
                        if (downloadStatus !is UpdateUtils.DownloadStatus.InProgress) {
                            TextButton(
                                onClick = { 
                                    showDialog = false 
                                    updateResult = null
                                    showUpdateLog = false
                                    downloadStatus = UpdateUtils.DownloadStatus.NotStarted
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun checkForUpdates(showAlways: Boolean = false) {
        val result = updateUtils.checkForUpdates()
        when (result) {
            is UpdateUtils.UpdateCheckResult.UpdateAvailable -> {
                runOnUiThread {
                    updateDialogState.value = result
                    showUpdateDialog.value = true
                    Toast.makeText(
                        this, 
                        "Update available: ${result.newVersion}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            is UpdateUtils.UpdateCheckResult.NoUpdateAvailable -> {
                if (showAlways) {
                    runOnUiThread {
                        Toast.makeText(
                            this, 
                            "No updates available", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            is UpdateUtils.UpdateCheckResult.CheckFailed -> {
                runOnUiThread {
                    Toast.makeText(
                        this, 
                        "Update check failed: ${result.errorMessage}", 
                        Toast.LENGTH_SHORT
                    ).show()
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