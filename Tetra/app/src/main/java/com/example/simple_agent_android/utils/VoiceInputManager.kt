package com.example.simple_agent_android.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import java.util.Locale
import com.example.simple_agent_android.sentry.AgentErrorTracker
import com.example.simple_agent_android.sentry.trackFeatureUsage

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR,
    COMPLETED
}

class VoiceInputManager(private val context: Context) {
    private val TAG = "VoiceInputManager"
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // State management
    private val _voiceInputState = mutableStateOf(VoiceInputState.IDLE)
    val voiceInputState: State<VoiceInputState> = _voiceInputState
    
    private val _transcriptionText = mutableStateOf("")
    val transcriptionText: State<String> = _transcriptionText
    
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage
    
    // Callbacks
    private var onTranscriptionComplete: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onStateChange: ((VoiceInputState) -> Unit)? = null
    
    init {
        initializeSpeechRecognizer()
    }
    
    private fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.d(TAG, "Speech recognition is available, creating recognizer")
                speechRecognizer?.destroy() // Clean up any existing instance
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                
                if (speechRecognizer == null) {
                    Log.e(TAG, "Failed to create SpeechRecognizer instance")
                    return
                }
                
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                    updateState(VoiceInputState.LISTENING)
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech")
                    _transcriptionText.value = "Listening..."
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Could use this for visual feedback (volume indicator)
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Not needed for our use case
                }
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                    updateState(VoiceInputState.PROCESSING)
                    _transcriptionText.value = "Processing..."
                }
                
                override fun onError(error: Int) {
                    Log.e(TAG, "Speech recognition error: $error")
                    val errorMsg = getErrorMessage(error)
                    _errorMessage.value = errorMsg
                    updateState(VoiceInputState.ERROR)
                    
                    // Track voice input error
                    AgentErrorTracker.trackVoiceInputError(
                        error = Exception("Speech recognition error: $error - $errorMsg"),
                        stage = "recording",
                        context = mapOf(
                            "error_code" to error,
                            "error_message" to errorMsg,
                            "was_listening" to isListening
                        )
                    )
                    
                    onError?.invoke(errorMsg)
                    isListening = false
                }
                
                override fun onResults(results: Bundle?) {
                    Log.d(TAG, "Speech recognition results received")
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    Log.d(TAG, "Results bundle keys: ${results?.keySet()?.joinToString()}")
                    Log.d(TAG, "Matches: $matches")
                    Log.d(TAG, "Confidence scores: ${confidence?.contentToString()}")
                    
                    // Try to find any usable result, even with low confidence
                    var bestResult: String? = null
                    if (!matches.isNullOrEmpty()) {
                        // Try each result in order of preference
                        for (i in matches.indices) {
                            val result = matches[i]
                            if (result.isNotBlank()) {
                                bestResult = result
                                Log.d(TAG, "Found usable result at index $i: '$result'")
                                break
                            }
                        }
                    }
                    
                    if (bestResult != null) {
                        Log.d(TAG, "Using transcription: '$bestResult'")
                        _transcriptionText.value = bestResult
                        updateState(VoiceInputState.COMPLETED)
                        
                        // Track successful voice input
                        trackFeatureUsage(
                            feature = "voice_input",
                            action = "transcription_success",
                            success = true,
                            metadata = mapOf(
                                "transcription_length" to bestResult.length,
                                "confidence_available" to (confidence != null)
                            )
                        )
                        
                        onTranscriptionComplete?.invoke(bestResult)
                    } else {
                        // Check if we had any partial results that we can use
                        val currentText = _transcriptionText.value
                        if (currentText.startsWith("Listening: ") && currentText.length > 11) {
                            val partialResult = currentText.substring(11).trim()
                            if (partialResult.isNotBlank()) {
                                Log.d(TAG, "Using partial result as fallback: '$partialResult'")
                                _transcriptionText.value = partialResult
                                updateState(VoiceInputState.COMPLETED)
                                onTranscriptionComplete?.invoke(partialResult)
                                isListening = false
                                return
                            }
                        }
                        
                        val errorMsg = "No speech recognized - try speaking louder, slower, or closer to the microphone"
                        Log.w(TAG, errorMsg)
                        _errorMessage.value = errorMsg
                        updateState(VoiceInputState.ERROR)
                        onError?.invoke(errorMsg)
                    }
                    isListening = false
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "Partial results: $matches")
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0]
                        Log.d(TAG, "Partial transcription: $partialText")
                        _transcriptionText.value = "Listening: $partialText"
                        
                        // If we get partial results, we know speech is being detected
                        if (partialText.isNotBlank()) {
                            Log.d(TAG, "Speech detected in partial results")
                        }
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Not needed for our use case
                }
                })
            } else {
                Log.e(TAG, "Speech recognition not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
            
            // Track initialization error
            AgentErrorTracker.trackVoiceInputError(
                error = e,
                stage = "initialization",
                context = mapOf(
                    "recognition_available" to SpeechRecognizer.isRecognitionAvailable(context)
                )
            )
            
            speechRecognizer = null
        }
    }
    
    fun startListening(
        onComplete: (String) -> Unit,
        onError: (String) -> Unit = {},
        onStateChange: (VoiceInputState) -> Unit = {}
    ) {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        this.onTranscriptionComplete = onComplete
        this.onError = onError
        this.onStateChange = onStateChange
        
        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your agent instruction...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10) // Even more results
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Balanced timeouts - fast but not too aggressive
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
            // Add calling package to avoid intent issues
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Try both online and offline recognition
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            // Add secure flag
            putExtra(RecognizerIntent.EXTRA_SECURE, false)
            // Add confidence threshold - lower means more lenient
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            // Request web search results too
            putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, false)
        }
        
        try {
            isListening = true
            _transcriptionText.value = ""
            _errorMessage.value = ""
            updateState(VoiceInputState.IDLE)
            
            Log.d(TAG, "Starting speech recognition with intent extras:")
            for (key in intent.extras?.keySet() ?: emptySet()) {
                Log.d(TAG, "  $key: ${intent.extras?.get(key)}")
            }
            
            // Track voice input start
            trackFeatureUsage(
                feature = "voice_input",
                action = "start_listening",
                success = true,
                metadata = mapOf(
                    "recognition_available" to SpeechRecognizer.isRecognitionAvailable(context),
                    "language" to Locale.getDefault().toString()
                )
            )
            
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            val errorMsg = "Failed to start voice recognition: ${e.message}"
            _errorMessage.value = errorMsg
            updateState(VoiceInputState.ERROR)
            
            // Track voice input start error
            AgentErrorTracker.trackVoiceInputError(
                error = e,
                stage = "start_listening",
                context = mapOf(
                    "error_message" to errorMsg,
                    "recognition_available" to SpeechRecognizer.isRecognitionAvailable(context)
                )
            )
            
            onError(errorMsg)
            isListening = false
        }
    }
    
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            updateState(VoiceInputState.IDLE)
            Log.d(TAG, "Stopped listening")
        }
    }
    
    fun cancelListening() {
        if (isListening) {
            speechRecognizer?.cancel()
            isListening = false
            updateState(VoiceInputState.IDLE)
            _transcriptionText.value = ""
            Log.d(TAG, "Cancelled listening")
        }
    }
    
    private fun updateState(newState: VoiceInputState) {
        _voiceInputState.value = newState
        onStateChange?.invoke(newState)
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error - check microphone permissions"
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error - try again"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error - check internet connection"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - try again"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected - speak louder and clearer"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy - try again in a moment"
            SpeechRecognizer.ERROR_SERVER -> "Speech service error - try again"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout - try speaking sooner after tapping"
            else -> "Speech recognition error (code: $errorCode) - try again"
        }
    }
    
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    fun cleanup() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        updateState(VoiceInputState.IDLE)
    }
    
    // Alternative method using system speech recognition activity
    fun createSystemSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your agent instruction...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    fun startListeningWithFallback(
        onComplete: (String) -> Unit,
        onError: (String) -> Unit = {},
        onStateChange: (VoiceInputState) -> Unit = {}
    ) {
        // First try the standard approach
        startListening(
            onComplete = onComplete,
            onError = { error ->
                Log.w(TAG, "Primary speech recognition failed: $error")
                // If it fails, we could implement a fallback here
                // For now, just pass the error through
                onError(error)
            },
            onStateChange = onStateChange
        )
    }
    
    companion object {
        fun isVoiceRecognitionAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
        
        fun getAvailableLanguages(context: Context): List<String> {
            return try {
                val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
                val pm = context.packageManager
                val activities = pm.queryIntentActivities(intent, 0)
                if (activities.isNotEmpty()) {
                    listOf(Locale.getDefault().toString())
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                listOf(Locale.getDefault().toString())
            }
        }
    }
} 