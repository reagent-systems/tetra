package com.example.simple_agent_android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simple_agent_android.ui.AboutScreen
import com.example.simple_agent_android.ui.DebugScreen
import com.example.simple_agent_android.ui.FeedbackScreen
import com.example.simple_agent_android.ui.HomeScreen
import com.example.simple_agent_android.ui.OnboardingScreen
import com.example.simple_agent_android.ui.SettingsScreen
import com.example.simple_agent_android.ui.SidebarDrawer
import com.example.simple_agent_android.ui.theme.ReagentDark
import com.example.simple_agent_android.ui.theme.ReagentWhite
import com.example.simple_agent_android.ui.theme.SimpleAgentAndroidTheme
import com.example.simple_agent_android.utils.UpdateUtils
import com.example.simple_agent_android.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import io.sentry.Sentry

class MainActivity : ComponentActivity() {
    private lateinit var updateUtils: UpdateUtils
    private var updateDialogState = mutableStateOf<UpdateUtils.UpdateCheckResult?>(null)
    private var showUpdateDialog = mutableStateOf(false)
    private lateinit var viewModel: MainViewModel

    // Add permission launchers
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications permission is required for agent status updates", Toast.LENGTH_LONG).show()
        }
    }
    
    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun checkAndRequestMicrophonePermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestMicrophonePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    // Initialize Sentry error tracking
    com.example.simple_agent_android.sentry.SentryManager.initialize(this)

        enableEdgeToEdge()

        // Request permissions
        checkAndRequestNotificationPermission()
        checkAndRequestMicrophonePermission()

        updateUtils = UpdateUtils(this)

        // Check for updates on app startup
        lifecycleScope.launch {
            checkForUpdates()
        }

        setContent {
            SimpleAgentAndroidTheme {
                viewModel = viewModel()
                
                // Initialize ViewModel with context
                LaunchedEffect(Unit) {
                    viewModel.initialize(this@MainActivity)
                    
                    // Check if onboarding should be shown
                    if (viewModel.shouldShowOnboarding(this@MainActivity)) {
                        viewModel.startOnboarding()
                    }
                }

                // Show onboarding if needed
                if (viewModel.showOnboarding.value) {
                    val currentStep = viewModel.getCurrentOnboardingStep()
                    if (currentStep != null) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            currentStep = currentStep,
                            currentStepIndex = viewModel.currentOnboardingStep.value,
                            totalSteps = viewModel.onboardingSteps.value.size,
                            onNextStep = viewModel::nextOnboardingStep,
                            onSkipStep = viewModel::skipOnboardingStep,
                            onCompleteOnboarding = {
                                viewModel.markOnboardingComplete(this@MainActivity)
                                viewModel.completeOnboarding()
                            },
                            onNavigateToScreen = viewModel::handleOnboardingNavigation,
                            onDismiss = viewModel::dismissOnboarding,
                            onToggleOverlay = { viewModel.toggleOverlay(this@MainActivity) },
                            onEnableAccessibility = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            },
                            onUpdateVerticalOffset = { offset ->
                                viewModel.updateVerticalOffset(this@MainActivity, offset)
                            },
                            onSaveApiKey = {
                                viewModel.saveSettings(this@MainActivity)
                            }
                        )
                    }
                                } else {
                                        SidebarDrawer(
                        drawerOpen = viewModel.drawerOpen.value,
                        onDrawerOpen = viewModel::openDrawer,
                        onDrawerClose = viewModel::closeDrawer,
                        selectedScreen = viewModel.selectedScreen.value,
                        onSelectScreen = viewModel::selectScreen,
                    ) {
                    when (viewModel.selectedScreen.value) {
                        "home" -> HomeScreen(
                            agentRunning = viewModel.agentRunning.value,
                            onStartAgent = { viewModel.startAgent(this@MainActivity) },
                            onStopAgent = viewModel::stopAgent,
                            agentInput = viewModel.agentInput.value,
                            onAgentInputChange = viewModel::updateAgentInput,
                            onEnableAccessibility = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            },
                            agentOutput = viewModel.agentOutput.value,
                            floatingUiEnabled = viewModel.floatingUiEnabled.value,
                            onToggleFloatingUi = { viewModel.toggleFloatingUi(this@MainActivity) },
                            // Voice Input Parameters
                            voiceInputState = viewModel.voiceInputState.value,
                            voiceTranscription = viewModel.voiceTranscription.value,
                            voiceInputAvailable = viewModel.voiceInputAvailable.value,
                            onStartVoiceInput = viewModel::startVoiceInput,
                            onStopVoiceInput = viewModel::stopVoiceInput,
                            onCancelVoiceInput = viewModel::cancelVoiceInput,
                            // Status Parameters
                            accessibilityServiceEnabled = viewModel.accessibilityServiceEnabled.value,
                            // Onboarding Parameters
                            onShowTutorial = viewModel::startOnboarding
                        )
                        "debug" -> DebugScreen(
                            showBoxes = viewModel.showBoxes.value,
                            onToggleBoxes = viewModel::toggleShowBoxes,
                            overlayActive = viewModel.overlayActive.value,
                            onToggleOverlay = { viewModel.toggleOverlay(this@MainActivity) },
                            verticalOffset = viewModel.verticalOffset.value,
                            onVerticalOffsetChange = { viewModel.updateVerticalOffset(this@MainActivity, it) },
                            debugCursorEnabled = viewModel.debugCursorEnabled.value,
                            onToggleDebugCursor = { viewModel.toggleDebugCursor(this@MainActivity) },
                            onExportJson = viewModel::exportJson,
                            jsonOutput = if (viewModel.showJsonDialog.value) viewModel.jsonOutput.value else null,
                            onCloseJson = viewModel::closeJsonDialog
                        )
                        "settings" -> SettingsScreen(
                            openAiKey = viewModel.openAiKey.value,
                            onOpenAiKeyChange = viewModel::updateOpenAiKey,
                            openAiBaseUrl = viewModel.openAiBaseUrl.value,
                            onOpenAiBaseUrlChange = viewModel::updateOpenAiBaseUrl,
                            openAiModel = viewModel.openAiModel.value,
                            onOpenAiModelChange = viewModel::updateOpenAiModel,
                            onSave = { viewModel.saveSettings(this@MainActivity) },
                            saved = viewModel.settingsSaved.value,
                            onCheckForUpdates = {
                                lifecycleScope.launch {
                                    checkForUpdates(showAlways = true)
                                }
                            },
                            onRedoOnboarding = viewModel::startOnboarding,
                            completionScreenEnabled = viewModel.completionScreenEnabled.value,
                            onCompletionScreenToggle = viewModel::updateCompletionScreenEnabled
                        )
                        "feedback" -> FeedbackScreen(viewModel)
                        "about" -> AboutScreen()
                        else -> {}
                    }

                    // Update dialog
                    UpdateDialog()
                }
            }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh notification status when app comes to foreground
        if (::viewModel.isInitialized) {
            viewModel.refreshNotificationStatus(this)
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