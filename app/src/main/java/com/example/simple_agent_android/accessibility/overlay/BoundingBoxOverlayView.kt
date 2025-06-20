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
    private var boundingBoxes: List<Rect> = emptyList()
    private val offset = 2 // px offset for better alignment
    private val maxDrawBoxes = 100
    var showBoundingBoxes: Boolean = true

    fun updateBoundingBoxes(root: AccessibilityNodeInfo?) {
        val boxes = mutableListOf<Rect>()
        collectBoundingBoxes(root, boxes)
        boundingBoxes = boxes
        postInvalidate()
    }

    private fun collectBoundingBoxes(node: AccessibilityNodeInfo?, boxes: MutableList<Rect>) {
        if (node == null) return
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            // Apply offset
            rect.left += offset
            rect.top += offset + SharedPrefsUtils.getVerticalOffset(context)
            rect.right -= offset
            rect.bottom -= offset - SharedPrefsUtils.getVerticalOffset(context)
            boxes.add(rect)
        }
        for (i in 0 until node.childCount) {
            collectBoundingBoxes(node.getChild(i), boxes)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showBoundingBoxes) return
        for (i in 0 until minOf(boundingBoxes.size, maxDrawBoxes)) {
            val rect = boundingBoxes[i]
            canvas.drawRect(rect, paint)
        }
    }
} 