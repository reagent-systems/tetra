package com.example.simple_agent_android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo

class BoundingBoxOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var boundingBoxes: List<Rect> = emptyList()
    private val offset = 2 // px offset for better alignment

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
            rect.top += offset + BoundingBoxAccessibilityService.getVerticalOffset(context)
            rect.right -= offset
            rect.bottom -= offset - BoundingBoxAccessibilityService.getVerticalOffset(context)
            boxes.add(rect)
        }
        for (i in 0 until node.childCount) {
            collectBoundingBoxes(node.getChild(i), boxes)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in boundingBoxes) {
            canvas.drawRect(rect, paint)
        }
    }
} 