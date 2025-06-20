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
import com.example.simple_agent_android.accessibility.service.BoundingBoxAccessibilityService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    
    fun initialize(context: Context) {
        // Load saved preferences
        _openAiKey.value = SharedPrefsUtils.getOpenAIKey(context)
        _verticalOffset.value = SharedPrefsUtils.getVerticalOffset(context)
        _overlayActive.value = BoundingBoxAccessibilityService.isOverlayActive()
        
        // Initialize AgentStateManager
        AgentStateManager.initialize(context)
        
        // Observe agent state changes from AgentStateManager
        AgentStateManager.agentRunningFlow
            .onEach { isRunning ->
                _agentRunning.value = isRunning
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
} 