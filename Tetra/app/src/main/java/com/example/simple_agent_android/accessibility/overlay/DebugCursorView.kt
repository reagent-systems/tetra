package com.example.simple_agent_android.accessibility.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager

class DebugCursorView(context: Context) : View(context) {
    private val TAG = "DebugCursorView"
    
    private val cursorPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val ringPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    
    private val swipeCursorPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val swipeRingPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    
    private val swipeLinePaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private var cursorX = 0f
    private var cursorY = 0f
    private var showCursor = false
    private var animationProgress = 0f
    
    // Swipe properties
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeEndX = 0f
    private var swipeEndY = 0f
    private var showSwipe = false
    private var swipeAnimationProgress = 0f
    
    private val handler = Handler(Looper.getMainLooper())
    
    private val animationRunnable = object : Runnable {
        override fun run() {
            var needsUpdate = false
            
            if (showCursor) {
                animationProgress += 0.05f // Slower animation (was 0.1f)
                if (animationProgress >= 1.0f) {
                    showCursor = false
                    animationProgress = 0f
                }
                needsUpdate = true
            }
            
            if (showSwipe) {
                swipeAnimationProgress += 0.04f // Slightly faster than press
                if (swipeAnimationProgress >= 1.0f) {
                    showSwipe = false
                    swipeAnimationProgress = 0f
                }
                needsUpdate = true
            }
            
            if (needsUpdate) {
                invalidate()
                handler.postDelayed(this, 50) // 20 FPS
            }
        }
    }
    
    fun showPress(x: Int, y: Int) {
        cursorX = x.toFloat()
        cursorY = y.toFloat()
        showCursor = true
        animationProgress = 0f
        startAnimation()
    }
    
    fun showSwipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        swipeStartX = startX.toFloat()
        swipeStartY = startY.toFloat()
        swipeEndX = endX.toFloat()
        swipeEndY = endY.toFloat()
        showSwipe = true
        swipeAnimationProgress = 0f
        startAnimation()
    }
    
    private fun startAnimation() {
        handler.removeCallbacks(animationRunnable)
        handler.post(animationRunnable)
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw swipe animation
        if (showSwipe) {
            val progress = swipeAnimationProgress
            val baseRadius = 15f
            val maxRadius = 60f
            
            // Show start point with expanding ring (like press cursor)
            val startRingRadius = baseRadius + (maxRadius - baseRadius) * progress
            val startRingAlpha = (255 * (1f - progress * 0.8f)).toInt()
            swipeRingPaint.alpha = startRingAlpha
            canvas.drawCircle(swipeStartX, swipeStartY, startRingRadius, swipeRingPaint)
            
            // Start point center dot
            val startDotAlpha = (255 * (1f - progress * 0.3f)).toInt()
            swipeCursorPaint.alpha = startDotAlpha
            canvas.drawCircle(swipeStartX, swipeStartY, baseRadius * 0.4f, swipeCursorPaint)
            
            // Draw connecting line that appears progressively
            if (progress > 0.2f) {
                val lineProgress = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                val currentEndX = swipeStartX + (swipeEndX - swipeStartX) * lineProgress
                val currentEndY = swipeStartY + (swipeEndY - swipeStartY) * lineProgress
                
                val lineAlpha = (255 * (1f - progress * 0.5f)).toInt()
                swipeLinePaint.alpha = lineAlpha
                canvas.drawLine(swipeStartX, swipeStartY, currentEndX, currentEndY, swipeLinePaint)
            }
            
            // Show end point with expanding ring (appears after line starts)
            if (progress > 0.4f) {
                val endProgress = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
                val endRingRadius = baseRadius + (maxRadius - baseRadius) * endProgress
                val endRingAlpha = (255 * (1f - endProgress * 0.8f)).toInt()
                swipeRingPaint.alpha = endRingAlpha
                canvas.drawCircle(swipeEndX, swipeEndY, endRingRadius, swipeRingPaint)
                
                // End point center dot
                val endDotAlpha = (255 * (1f - endProgress * 0.3f)).toInt()
                swipeCursorPaint.alpha = endDotAlpha
                canvas.drawCircle(swipeEndX, swipeEndY, baseRadius * 0.4f, swipeCursorPaint)
                
                // Draw arrow when animation is nearly complete
                if (progress > 0.7f) {
                    drawArrow(canvas, swipeStartX, swipeStartY, swipeEndX, swipeEndY, endDotAlpha)
                }
            }
        }
        
        // Draw press cursor
        if (showCursor) {
            val baseRadius = 20f
            val maxRadius = 80f // Increased max radius
            
            // Draw expanding ring
            val ringRadius = baseRadius + (maxRadius - baseRadius) * animationProgress
            val ringAlpha = (255 * (1f - animationProgress)).toInt()
            ringPaint.alpha = ringAlpha
            canvas.drawCircle(cursorX, cursorY, ringRadius, ringPaint)
            
            // Draw center dot
            val dotAlpha = (255 * (1f - animationProgress * 0.3f)).toInt() // Slower fade
            cursorPaint.alpha = dotAlpha
            canvas.drawCircle(cursorX, cursorY, baseRadius * 0.5f, cursorPaint)
        }
    }
    
    private fun drawArrow(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, alpha: Int) {
        val dx = endX - startX
        val dy = endY - startY
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (length > 0) {
            val unitX = dx / length
            val unitY = dy / length
            
            val arrowLength = 25f
            val arrowAngle = Math.PI / 6 // 30 degrees
            
            // Calculate arrow points
            val arrowX1 = endX - arrowLength * (unitX * kotlin.math.cos(arrowAngle) - unitY * kotlin.math.sin(arrowAngle)).toFloat()
            val arrowY1 = endY - arrowLength * (unitX * kotlin.math.sin(arrowAngle) + unitY * kotlin.math.cos(arrowAngle)).toFloat()
            
            val arrowX2 = endX - arrowLength * (unitX * kotlin.math.cos(-arrowAngle) - unitY * kotlin.math.sin(-arrowAngle)).toFloat()
            val arrowY2 = endY - arrowLength * (unitX * kotlin.math.sin(-arrowAngle) + unitY * kotlin.math.cos(-arrowAngle)).toFloat()
            
            // Draw arrow head with matching alpha
            val path = Path()
            path.moveTo(endX, endY)
            path.lineTo(arrowX1, arrowY1)
            path.lineTo(arrowX2, arrowY2)
            path.close()
            
            swipeCursorPaint.alpha = alpha
            canvas.drawPath(path, swipeCursorPaint)
        }
    }
    
    companion object {
        private var instance: DebugCursorView? = null
        private var windowManager: WindowManager? = null
        private var isActive = false
        
        fun initialize(context: Context) {
            if (instance == null) {
                windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }
        }
        
        fun show(context: Context) {
            if (instance != null) return
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                instance = DebugCursorView(context)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                try {
                    windowManager?.addView(instance, params)
                    isActive = true
                    Log.d("DebugCursorView", "Debug cursor view added")
                } catch (e: Exception) {
                    Log.e("DebugCursorView", "Error adding debug cursor view", e)
                    instance = null
                }
            }
        }
        
        fun hide() {
            instance?.let {
                try {
                    windowManager?.removeView(it)
                    Log.d("DebugCursorView", "Debug cursor view removed")
                } catch (e: Exception) {
                    Log.e("DebugCursorView", "Error removing debug cursor view", e)
                }
                instance = null
                isActive = false
            }
        }
        
        fun showPress(x: Int, y: Int) {
            instance?.showPress(x, y)
        }
        
        fun showSwipe(startX: Int, startY: Int, endX: Int, endY: Int) {
            instance?.showSwipe(startX, startY, endX, endY)
        }
        
        fun isActive(): Boolean = isActive
    }
} 