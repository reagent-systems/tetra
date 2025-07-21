package com.example.simple_agent_android.accessibility.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.example.simple_agent_android.utils.SharedPrefsUtils

class BoundingBoxOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val centerPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    
    private var boundingBoxes: List<Rect> = emptyList()
    private var interactiveBoxes: List<Rect> = emptyList()
    private val offset = 2 // px offset for better alignment
    private val maxDrawBoxes = 100
    var showBoundingBoxes: Boolean = true

    fun updateBoundingBoxes(root: AccessibilityNodeInfo?) {
        val allBoxes = mutableListOf<Rect>()
        val interactiveBoxes = mutableListOf<Rect>()
        collectBoundingBoxes(root, allBoxes, interactiveBoxes)
        boundingBoxes = allBoxes
        this.interactiveBoxes = interactiveBoxes
        postInvalidate()
    }

    private fun collectBoundingBoxes(node: AccessibilityNodeInfo?, allBoxes: MutableList<Rect>, interactiveBoxes: MutableList<Rect>) {
        if (node == null) return
        if (node.packageName == "com.example.simple_agent_android") return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            // Apply offset - same as InteractiveElementUtils
            val adjustedRect = Rect(rect)
            adjustedRect.left += offset
            adjustedRect.top += offset + SharedPrefsUtils.getVerticalOffset(context)
            adjustedRect.right -= offset
            adjustedRect.bottom -= offset - SharedPrefsUtils.getVerticalOffset(context)
            
            // Add to all boxes
            allBoxes.add(adjustedRect)
            
            // Add to interactive boxes only if it's interactive
            if (node.isClickable || node.isFocusable || node.isLongClickable) {
                interactiveBoxes.add(adjustedRect)
            }
        }
        for (i in 0 until node.childCount) {
            collectBoundingBoxes(node.getChild(i), allBoxes, interactiveBoxes)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showBoundingBoxes) return
        
        // Draw all bounding boxes in red
        for (i in 0 until minOf(boundingBoxes.size, maxDrawBoxes)) {
            val rect = boundingBoxes[i]
            canvas.drawRect(rect, paint)
        }
        
        // Draw green center points only for interactive elements
        for (i in 0 until minOf(interactiveBoxes.size, maxDrawBoxes)) {
            val rect = interactiveBoxes[i]
            val centerX = rect.left + rect.width() / 2
            val centerY = rect.top + rect.height() / 2
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), 8f, centerPaint)
        }
    }
} 