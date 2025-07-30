package com.example.simple_agent_android.ui.floating

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.example.simple_agent_android.MainActivity
import com.example.simple_agent_android.R

class FloatingCompletionScreen(private val context: Context) {
    private val TAG = "FloatingCompletionScreen"
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_SimpleAgentAndroid_Dialog)
    private var windowManager: WindowManager? = null
    private var completionView: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    
    // Callbacks
    private var onNewTask: ((String) -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show(taskSummary: String = "Your agent has successfully completed the requested task.") {
        try {
            Log.d(TAG, "Showing completion screen with summary: $taskSummary")
            if (completionView != null) {
                Log.d(TAG, "Completion screen already exists, dismissing first")
                dismiss()
            }

            createCompletionView(taskSummary)
            setupWindowParams()
            addToWindow()
            animateIn()
            Log.d(TAG, "Completion screen shown successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing completion screen", e)
        }
    }

    private fun createCompletionView(taskSummary: String) {
        completionView = LayoutInflater.from(themedContext)
            .inflate(R.layout.floating_completion_screen, null) as LinearLayout
            
        // Set task summary
        completionView?.findViewById<TextView>(R.id.tv_task_summary)?.text = taskSummary
        
        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        completionView?.apply {
            val taskInput = findViewById<EditText>(R.id.et_new_task)
            
            // Dismiss button
            findViewById<ImageButton>(R.id.btn_dismiss)?.setOnClickListener {
                Log.d(TAG, "Dismiss button clicked")
                dismiss()
                onDismiss?.invoke()
            }
            
            // Start task button
            findViewById<Button>(R.id.btn_start_task)?.setOnClickListener {
                val taskText = taskInput?.text?.toString()?.trim()
                if (!taskText.isNullOrEmpty()) {
                    Log.d(TAG, "Start task button clicked with: $taskText")
                    dismiss()
                    onNewTask?.invoke(taskText)
                } else {
                    Log.d(TAG, "Start task button clicked but no task text entered")
                    // Could show a toast or highlight the input field
                }
            }
            
            // Settings button
            findViewById<Button>(R.id.btn_settings)?.setOnClickListener {
                Log.d(TAG, "Settings button clicked")
                dismiss()
                openMainApp()
            }
            
            // Feedback button
            findViewById<Button>(R.id.btn_feedback)?.setOnClickListener {
                Log.d(TAG, "Feedback button clicked")
                dismiss()
                // Could open feedback form or email intent
            }
        }
    }

    private fun setupWindowParams() {
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
            gravity = Gravity.CENTER
        }
    }

    private fun addToWindow() {
        try {
            completionView?.alpha = 0f
            completionView?.scaleX = 0.8f
            completionView?.scaleY = 0.8f
            windowManager?.addView(completionView, params)
            Log.d(TAG, "Completion screen added to window")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding completion screen to window", e)
        }
    }

    private fun animateIn() {
        completionView?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(300)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.start()
    }

    private fun animateOut(onComplete: () -> Unit) {
        completionView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(200)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.withEndAction {
                onComplete()
            }
            ?.start()
    }

    fun dismiss() {
        try {
            Log.d(TAG, "Dismissing completion screen")
            animateOut {
                try {
                    completionView?.let { view ->
                        windowManager?.removeView(view)
                    }
                    completionView = null
                    Log.d(TAG, "Completion screen dismissed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing completion screen from window", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing completion screen", e)
        }
    }

    private fun openMainApp() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening main app", e)
        }
    }

    // Callback setters
    fun setOnNewTaskListener(listener: (String) -> Unit) {
        onNewTask = listener
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismiss = listener
    }
} 