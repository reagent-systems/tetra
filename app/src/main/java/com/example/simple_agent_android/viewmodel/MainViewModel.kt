package com.example.simple_agent_android.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simple_agent_android.agentcore.AgentStateManager
import com.example.simple_agent_android.utils.SharedPrefsUtils
import com.example.simple_agent_android.utils.OverlayPermissionUtils
import com.example.simple_agent_android.utils.VoiceInputManager
import com.example.simple_agent_android.utils.VoiceInputState
import com.example.simple_agent_android.utils.AccessibilityUtils
import com.example.simple_agent_android.accessibility.service.BoundingBoxAccessibilityService
import com.example.simple_agent_android.notification.AgentNotificationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
import android.os.Handler
import android.os.Looper

class MainViewModel : ViewModel() {
    
    // UI State
    private val _drawerOpen = mutableStateOf(false)
    val drawerOpen: State<Boolean> = _drawerOpen
    
    private val _selectedScreen = mutableStateOf("home")
    val selectedScreen: State<String> = _selectedScreen
    
    // Agent State
    private val _agentRunning = mutableStateOf(false)
    val agentRunning: State<Boolean> = _agentRunning
    
    private val _agentInput = mutableStateOf("")
    val agentInput: State<String> = _agentInput
    
    private val _agentOutput = mutableStateOf("")
    val agentOutput: State<String> = _agentOutput
    
    // Settings State
    private val _openAiKey = mutableStateOf("")
    val openAiKey: State<String> = _openAiKey
    
    private val _settingsSaved = mutableStateOf(false)
    val settingsSaved: State<Boolean> = _settingsSaved
    
    // Overlay State
    private val _overlayActive = mutableStateOf(false)
    val overlayActive: State<Boolean> = _overlayActive
    
    private val _showBoxes = mutableStateOf(true)
    val showBoxes: State<Boolean> = _showBoxes
    
    private val _floatingUiEnabled = mutableStateOf(false)
    val floatingUiEnabled: State<Boolean> = _floatingUiEnabled
    
    private val _verticalOffset = mutableStateOf(0)
    val verticalOffset: State<Int> = _verticalOffset
    
    // Dialog State
    private val _showJsonDialog = mutableStateOf(false)
    val showJsonDialog: State<Boolean> = _showJsonDialog
    
    private val _jsonOutput = mutableStateOf("")
    val jsonOutput: State<String> = _jsonOutput
    
    // Voice Input State
    private var voiceInputManager: VoiceInputManager? = null
    
    private val _voiceInputState = mutableStateOf(VoiceInputState.IDLE)
    val voiceInputState: State<VoiceInputState> = _voiceInputState
    
    private val _voiceTranscription = mutableStateOf("")
    val voiceTranscription: State<String> = _voiceTranscription
    
    private val _voiceInputAvailable = mutableStateOf(false)
    val voiceInputAvailable: State<Boolean> = _voiceInputAvailable
    
    // Notification State
    private var notificationManager: AgentNotificationManager? = null
    
    private val _notificationEnabled = mutableStateOf(false)
    val notificationEnabled: State<Boolean> = _notificationEnabled
    
    // Accessibility Service State
    private val _accessibilityServiceEnabled = mutableStateOf(false)
    val accessibilityServiceEnabled: State<Boolean> = _accessibilityServiceEnabled
    
    // Handler for periodic accessibility service status checks
    private var statusCheckHandler: Handler? = null
    private var statusCheckRunnable: Runnable? = null
    
    fun initialize(context: Context) {
        // Load saved preferences
        _openAiKey.value = SharedPrefsUtils.getOpenAIKey(context)
        _verticalOffset.value = SharedPrefsUtils.getVerticalOffset(context)
        _overlayActive.value = BoundingBoxAccessibilityService.isOverlayActive()
        
        // Initialize AgentStateManager
        AgentStateManager.initialize(context)
        
        // Initialize Voice Input Manager
        voiceInputManager = VoiceInputManager(context)
        _voiceInputAvailable.value = VoiceInputManager.isVoiceRecognitionAvailable(context)
        
        // Initialize Notification Manager
        notificationManager = AgentNotificationManager(context)
        
        // Check accessibility service status and auto-enable notifications if running
        updateAccessibilityAndNotificationStatus(context)
        
        // Start periodic accessibility service status monitoring
        startAccessibilityStatusMonitoring(context)
        
        // Observe agent state changes from AgentStateManager
        AgentStateManager.agentRunningFlow
            .onEach { isRunning ->
                _agentRunning.value = isRunning
                // Update notification when agent state changes (if notifications are enabled)
                if (_notificationEnabled.value) {
                    notificationManager?.updateNotification(isRunning)
                }
            }
            .launchIn(viewModelScope)
    }
    
    // UI Actions
    fun openDrawer() {
        _drawerOpen.value = true
    }
    
    fun closeDrawer() {
        _drawerOpen.value = false
    }
    
    fun selectScreen(screen: String) {
        _selectedScreen.value = screen
        _drawerOpen.value = false
        if (screen != "settings") {
            _settingsSaved.value = false
        }
    }
    
    // Agent Actions
    fun updateAgentInput(input: String) {
        _agentInput.value = input
    }
    
    fun startAgent(context: Context) {
        if (!OverlayPermissionUtils.hasOverlayPermission(context)) {
            OverlayPermissionUtils.requestOverlayPermission(context)
            return
        }
        
        if (!BoundingBoxAccessibilityService.isOverlayActive()) {
            BoundingBoxAccessibilityService.startOverlay(false)
        } else {
            BoundingBoxAccessibilityService.setOverlayEnabled(false)
        }
        
        _agentOutput.value = ""
        AgentStateManager.startAgent(
            instruction = _agentInput.value,
            apiKey = _openAiKey.value,
            appContext = context,
            onOutput = { output ->
                _agentOutput.value += output + "\n"
            }
        )
        // Note: _agentRunning.value will be updated automatically via the flow observer
    }
    
    fun stopAgent() {
        AgentStateManager.stopAgent()
        // Note: _agentRunning.value will be updated automatically via the flow observer
    }
    
    // Settings Actions
    fun updateOpenAiKey(key: String) {
        _openAiKey.value = key
    }
    
    fun saveSettings(context: Context) {
        SharedPrefsUtils.setOpenAIKey(context, _openAiKey.value)
        _settingsSaved.value = true
    }
    
    // Overlay Actions
    fun toggleFloatingUi(context: Context) {
        if (!OverlayPermissionUtils.hasOverlayPermission(context)) {
            OverlayPermissionUtils.requestOverlayPermission(context)
            return
        }
        
        try {
            _floatingUiEnabled.value = !_floatingUiEnabled.value
            if (_floatingUiEnabled.value) {
                BoundingBoxAccessibilityService.showFloatingButton()
            } else {
                BoundingBoxAccessibilityService.hideFloatingButton()
            }
        } catch (e: Exception) {
            _floatingUiEnabled.value = false
        }
    }
    
    fun toggleOverlay(context: Context) {
        if (!OverlayPermissionUtils.hasOverlayPermission(context)) {
            OverlayPermissionUtils.requestOverlayPermission(context)
            return
        }
        
        if (BoundingBoxAccessibilityService.isOverlayActive()) {
            BoundingBoxAccessibilityService.stopOverlay()
            _overlayActive.value = false
        } else {
            BoundingBoxAccessibilityService.startOverlay(_showBoxes.value)
            _overlayActive.value = true
        }
    }
    
    fun toggleShowBoxes() {
        _showBoxes.value = !_showBoxes.value
        BoundingBoxAccessibilityService.setOverlayEnabled(_showBoxes.value)
    }
    
    fun updateVerticalOffset(context: Context, offset: Int) {
        _verticalOffset.value = offset
        SharedPrefsUtils.setVerticalOffset(context, offset)
    }
    
    // Dialog Actions
    fun exportJson() {
        _jsonOutput.value = BoundingBoxAccessibilityService.getInteractiveElementsJson()
        _showJsonDialog.value = true
    }
    
    fun closeJsonDialog() {
        _showJsonDialog.value = false
    }
    
    // Voice Input Actions
    fun startVoiceInput() {
        voiceInputManager?.startListening(
            onComplete = { transcription ->
                _voiceTranscription.value = transcription
                _agentInput.value = transcription // Auto-fill the input field
                _voiceInputState.value = VoiceInputState.COMPLETED
            },
            onError = { error ->
                _voiceTranscription.value = "Error: $error"
                _voiceInputState.value = VoiceInputState.ERROR
            },
            onStateChange = { state ->
                _voiceInputState.value = state
                if (state == VoiceInputState.LISTENING) {
                    _voiceTranscription.value = "Listening..."
                } else if (state == VoiceInputState.PROCESSING) {
                    _voiceTranscription.value = "Processing..."
                }
            }
        )
    }
    
    fun stopVoiceInput() {
        voiceInputManager?.stopListening()
        _voiceInputState.value = VoiceInputState.IDLE
    }
    
    fun cancelVoiceInput() {
        voiceInputManager?.cancelListening()
        _voiceInputState.value = VoiceInputState.IDLE
        _voiceTranscription.value = ""
    }
    
    // Accessibility and Notification Management - Auto-managed based on accessibility service
    private fun updateAccessibilityAndNotificationStatus(context: Context) {
        val accessibilityServiceEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
        
        // Update accessibility service status
        _accessibilityServiceEnabled.value = accessibilityServiceEnabled
        
        // Update notification status based on accessibility service
        if (accessibilityServiceEnabled != _notificationEnabled.value) {
            _notificationEnabled.value = accessibilityServiceEnabled
            if (_notificationEnabled.value) {
                notificationManager?.showPersistentNotification(_agentRunning.value)
            } else {
                notificationManager?.hidePersistentNotification()
            }
        }
    }
    
    // Method to refresh status (can be called when user returns to app)
    fun refreshNotificationStatus(context: Context) {
        updateAccessibilityAndNotificationStatus(context)
        // Restart monitoring if it's not running
        if (statusCheckHandler == null) {
            startAccessibilityStatusMonitoring(context)
        }
    }
    
    // Start monitoring accessibility service status
    private fun startAccessibilityStatusMonitoring(context: Context) {
        statusCheckHandler = Handler(Looper.getMainLooper())
        statusCheckRunnable = object : Runnable {
            override fun run() {
                updateAccessibilityAndNotificationStatus(context)
                // Check every 2 seconds
                statusCheckHandler?.postDelayed(this, 2000)
            }
        }
        statusCheckHandler?.post(statusCheckRunnable!!)
    }
    
    // Stop monitoring accessibility service status
    private fun stopAccessibilityStatusMonitoring() {
        statusCheckRunnable?.let { runnable ->
            statusCheckHandler?.removeCallbacks(runnable)
        }
        statusCheckHandler = null
        statusCheckRunnable = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAccessibilityStatusMonitoring()
        voiceInputManager?.cleanup()
        notificationManager?.hidePersistentNotification()
    }
} 