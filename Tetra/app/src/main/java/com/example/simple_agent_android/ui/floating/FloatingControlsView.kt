package com.example.simple_agent_android.ui.floating

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import com.example.simple_agent_android.R

class FloatingControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val pauseButton: ImageButton
    private val stopButton: ImageButton

    var onPauseClick: (() -> Unit)? = null
    var onStopClick: (() -> Unit)? = null
    var onDragEvent: ((action: Int, rawX: Float, rawY: Float) -> Unit)? = null
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialX = 0
    private var initialY = 0
    private var wasDragged = false

    private fun setupDragListener(view: View) {
        view.setOnTouchListener { _, event ->
            onDragEvent?.invoke(event.action, event.rawX, event.rawY)
            false
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.floating_controls, this, true)
        pauseButton = findViewById(R.id.btn_pause)
        stopButton = findViewById(R.id.btn_stop)

        pauseButton.setOnClickListener { if (!wasDragged) onPauseClick?.invoke() }
        stopButton.setOnClickListener { if (!wasDragged) onStopClick?.invoke() }

        // Set drag listener on parent and both buttons
        setupDragListener(this)
        setupDragListener(pauseButton)
        setupDragListener(stopButton)
    }

    fun setPaused(paused: Boolean) {
        // Optionally update UI to reflect paused state
        pauseButton.setImageResource(
            if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        )
    }
} 