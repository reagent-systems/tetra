package com.example.simple_agent_android.ui.floating

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.example.simple_agent_android.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.Log
import androidx.appcompat.view.ContextThemeWrapper
import android.view.animation.AccelerateDecelerateInterpolator

class FloatingAgentButton(context: Context) {
    private val TAG = "FloatingAgentButton"
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_SimpleAgentAndroid_Dialog)
    private var windowManager: WindowManager? = null
    private var fab: FloatingActionButton? = null
    private var controlsLayout: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var dialog: AlertDialog? = null
    private var onStartAgent: ((String) -> Unit)? = null
    private var onPauseAgent: (() -> Unit)? = null
    private var onStopAgent: (() -> Unit)? = null
    private var currentState = State.START
    private var isPaused = false
    private var wasDragged = false

    enum class State {
        START,
        CONTROL
    }

    init {
        try {
            Log.d(TAG, "Initializing FloatingAgentButton")
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            Log.d(TAG, "WindowManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FloatingAgentButton", e)
        }
    }

    fun show() {
        try {
            Log.d(TAG, "Attempting to show floating button")
            if (fab != null) {
                Log.d(TAG, "FAB already exists, skipping creation")
                return
            }

            // Create both views
            createStartView()
            createControlView()

            // Set up window params
            Log.d(TAG, "Setting up window parameters")
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }

            // Add the FAB to window (start state)
            Log.d(TAG, "Adding FAB to window")
            try {
                windowManager?.addView(fab, params)
                Log.d(TAG, "FAB added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding FAB to window", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in show()", e)
        }
    }

    private fun createStartView() {
        fab = LayoutInflater.from(themedContext).inflate(R.layout.floating_action_button, null) as FloatingActionButton
        setupDragListener(fab!!)
    }

    private fun createControlView() {
        controlsLayout = LayoutInflater.from(themedContext).inflate(R.layout.floating_controls, null) as LinearLayout
        
        // Set up drag listeners for both the layout and the buttons
        setupDragListener(controlsLayout!!)
        
        val pauseButton = controlsLayout?.findViewById<FloatingActionButton>(R.id.btn_pause)
        val stopButton = controlsLayout?.findViewById<FloatingActionButton>(R.id.btn_stop)
        
        // Set up drag listeners for the buttons
        pauseButton?.let { setupDragListener(it) }
        stopButton?.let { setupDragListener(it) }
    }

    private fun setupDragListener(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Touch DOWN on view ${v.id}")
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    wasDragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        wasDragged = true
                        params?.x = initialX + dx
                        params?.y = initialY + dy
                        try {
                            val currentView = if (currentState == State.START) fab else controlsLayout
                            windowManager?.updateViewLayout(currentView, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating view layout during drag", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Touch UP on view ${v.id}, wasDragged: $wasDragged")
                    if (!wasDragged) {
                        when (v.id) {
                            R.id.btn_pause -> {
                                Log.d(TAG, "Pause button clicked")
                                onPauseAgent?.invoke()
                            }
                            R.id.btn_stop -> {
                                Log.d(TAG, "Stop button clicked")
                                onStopAgent?.invoke()
                                switchToStartState()
                            }
                            R.id.fab_start_agent -> {
                                Log.d(TAG, "FAB clicked")
                                showInputDialog()
                            }
                        }
                    }
                    wasDragged = false
                    true
                }
                else -> false
            }
        }
    }

    private fun updatePauseButtonState() {
        Log.d(TAG, "Updating pause button state, isPaused: $isPaused")
        val pauseButton = controlsLayout?.findViewById<FloatingActionButton>(R.id.btn_pause)
        pauseButton?.setImageResource(
            if (isPaused) android.R.drawable.ic_media_play
            else android.R.drawable.ic_media_pause
        )
    }

    private fun switchToControlState() {
        if (currentState == State.CONTROL) return
        
        wasDragged = false // Reset drag state when switching

        // Add the controls layout with initial state (invisible)
        controlsLayout?.let { controls ->
            controls.alpha = 0f
            windowManager?.addView(controls, params)
            
            // Get references to the buttons
            val pauseButton = controls.findViewById<FloatingActionButton>(R.id.btn_pause)
            val stopButton = controls.findViewById<FloatingActionButton>(R.id.btn_stop)
            
            // Set initial state for buttons - positioned at FAB's location
            pauseButton.scaleX = 0f
            pauseButton.scaleY = 0f
            pauseButton.translationX = 0f
            
            stopButton.scaleX = 0f
            stopButton.scaleY = 0f
            stopButton.translationX = 0f
            
            // Calculate the final positions
            controls.post {
                val pauseInitialX = pauseButton.translationX
                val stopInitialX = stopButton.translationX
                
                // Create animations for the main FAB - shorter duration for fade out
                val fabScaleDownX = ObjectAnimator.ofFloat(fab, View.SCALE_X, 1f, 0.5f).apply { duration = 200 }
                val fabScaleDownY = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 1f, 0.5f).apply { duration = 200 }
                val fabFadeOut = ObjectAnimator.ofFloat(fab, View.ALPHA, 1f, 0f).apply { duration = 150 } // Faster fade out
                
                // Create animations for the pause button
                val pauseScaleX = ObjectAnimator.ofFloat(pauseButton, View.SCALE_X, 0f, 1f)
                val pauseScaleY = ObjectAnimator.ofFloat(pauseButton, View.SCALE_Y, 0f, 1f)
                val pauseTranslateX = ObjectAnimator.ofFloat(pauseButton, View.TRANSLATION_X, 0f, pauseInitialX)
                val pauseFadeIn = ObjectAnimator.ofFloat(pauseButton, View.ALPHA, 0f, 1f)
                
                // Create animations for the stop button
                val stopScaleX = ObjectAnimator.ofFloat(stopButton, View.SCALE_X, 0f, 1f)
                val stopScaleY = ObjectAnimator.ofFloat(stopButton, View.SCALE_Y, 0f, 1f)
                val stopTranslateX = ObjectAnimator.ofFloat(stopButton, View.TRANSLATION_X, 0f, stopInitialX)
                val stopFadeIn = ObjectAnimator.ofFloat(stopButton, View.ALPHA, 0f, 1f)
                
                // Create the animation sequence
                AnimatorSet().apply {
                    playTogether(
                        fabScaleDownX, fabScaleDownY, fabFadeOut,
                        pauseScaleX, pauseScaleY, pauseTranslateX, pauseFadeIn,
                        stopScaleX, stopScaleY, stopTranslateX, stopFadeIn
                    )
                    duration = 300
                    interpolator = AccelerateDecelerateInterpolator()
                    
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: android.animation.Animator) {
                            controls.visibility = View.VISIBLE
                            controls.alpha = 1f
                        }
                        
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            try {
                                windowManager?.removeView(fab)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing FAB view", e)
                            }
                        }
                    })
                    start()
                }
            }
        }
        currentState = State.CONTROL
    }

    fun switchToStartState() {
        if (currentState == State.START) return

        wasDragged = false // Reset drag state when switching
        isPaused = false // Reset pause state when stopping agent

        // Add the FAB with initial state (invisible)
        fab?.let { fab ->
            fab.alpha = 0f
            fab.scaleX = 0.5f
            fab.scaleY = 0.5f
            windowManager?.addView(fab, params)
        }

        controlsLayout?.let { controls ->
            // Get references to the buttons
            val pauseButton = controls.findViewById<FloatingActionButton>(R.id.btn_pause)
            val stopButton = controls.findViewById<FloatingActionButton>(R.id.btn_stop)
            
            // Get current positions of the buttons
            val pauseCurrentX = pauseButton.translationX
            val stopCurrentX = stopButton.translationX
            
            // Create animations for the FAB
            val fabScaleUpX = ObjectAnimator.ofFloat(fab, View.SCALE_X, 0.5f, 1f)
            val fabScaleUpY = ObjectAnimator.ofFloat(fab, View.SCALE_Y, 0.5f, 1f)
            val fabFadeIn = ObjectAnimator.ofFloat(fab, View.ALPHA, 0f, 1f)
            
            // Create animations for the pause button
            val pauseScaleX = ObjectAnimator.ofFloat(pauseButton, View.SCALE_X, 1f, 0f)
            val pauseScaleY = ObjectAnimator.ofFloat(pauseButton, View.SCALE_Y, 1f, 0f)
            val pauseTranslateX = ObjectAnimator.ofFloat(pauseButton, View.TRANSLATION_X, pauseCurrentX, 0f)
            val pauseFadeOut = ObjectAnimator.ofFloat(pauseButton, View.ALPHA, 1f, 0f)
            
            // Create animations for the stop button
            val stopScaleX = ObjectAnimator.ofFloat(stopButton, View.SCALE_X, 1f, 0f)
            val stopScaleY = ObjectAnimator.ofFloat(stopButton, View.SCALE_Y, 1f, 0f)
            val stopTranslateX = ObjectAnimator.ofFloat(stopButton, View.TRANSLATION_X, stopCurrentX, 0f)
            val stopFadeOut = ObjectAnimator.ofFloat(stopButton, View.ALPHA, 1f, 0f)
            
            // Create the animation sequence
            AnimatorSet().apply {
                playTogether(
                    fabScaleUpX, fabScaleUpY, fabFadeIn,
                    pauseScaleX, pauseScaleY, pauseTranslateX, pauseFadeOut,
                    stopScaleX, stopScaleY, stopTranslateX, stopFadeOut
                )
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: android.animation.Animator) {
                        fab?.visibility = View.VISIBLE
                    }
                    
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        try {
                            windowManager?.removeView(controls)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing controls view", e)
                        }
                    }
                })
                start()
            }
        }
        currentState = State.START
    }

    private fun showInputDialog() {
        try {
            Log.d(TAG, "Attempting to show input dialog")
            if (dialog?.isShowing == true) {
                Log.d(TAG, "Dialog already showing, skipping")
                return
            }

            val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.agent_input_dialog, null)
            val input = dialogView.findViewById<EditText>(R.id.et_agent_input)
            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
            val btnStart = dialogView.findViewById<Button>(R.id.btn_start)

            dialog = AlertDialog.Builder(themedContext, R.style.Theme_SimpleAgentAndroid_Dialog)
                .setView(dialogView)
                .create()

            dialog?.window?.setType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
            )

            btnCancel.setOnClickListener {
                dialog?.dismiss()
            }

            btnStart.setOnClickListener {
                val instruction = input.text.toString()
                if (instruction.isNotEmpty()) {
                    // Reset pause state when starting a new agent
                    isPaused = false
                    onStartAgent?.invoke(instruction)
                    switchToControlState()
                }
                dialog?.dismiss()
            }

            dialog?.show()
            Log.d(TAG, "Input dialog shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing input dialog", e)
        }
    }

    fun hide() {
        try {
            Log.d(TAG, "Attempting to hide floating button")
            fab?.let {
                try {
                    windowManager?.removeView(it)
                    Log.d(TAG, "FAB removed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing FAB", e)
                }
            }
            controlsLayout?.let {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing controls", e)
                }
            }
            dialog?.dismiss()
            fab = null
            controlsLayout = null
            dialog = null
            Log.d(TAG, "Floating button hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in hide()", e)
        }
    }

    fun setOnStartAgentListener(listener: (String) -> Unit) {
        Log.d(TAG, "Setting onStartAgent listener")
        onStartAgent = listener
    }

    fun setOnPauseAgentListener(listener: () -> Unit) {
        onPauseAgent = listener
    }

    fun setOnStopAgentListener(listener: () -> Unit) {
        onStopAgent = listener
    }

    fun setPaused(paused: Boolean) {
        Log.d(TAG, "Setting paused state to: $paused")
        isPaused = paused
        updatePauseButtonState()
    }

    companion object {
        private const val TAG = "FloatingAgentButton"
        
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
} 