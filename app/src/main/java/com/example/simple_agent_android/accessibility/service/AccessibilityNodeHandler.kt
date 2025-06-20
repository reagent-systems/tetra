package com.example.simple_agent_android.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class AccessibilityNodeHandler(private val service: AccessibilityService) {
    private val TAG = "AccessibilityNodeHandler"

    fun setTextAt(x: Int, y: Int, text: String) {
        val root = service.rootInActiveWindow ?: return
        val node = findEditableNodeAt(root, x, y)
        if (node != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val args = Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } else {
                // Fallback: try focus and paste (not as reliable)
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            }
        }
    }

    private fun findEditableNodeAt(root: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)
        if (rect.contains(x, y) && root.isEditable) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            val result = child?.let { findEditableNodeAt(it, x, y) }
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun getRootNode(): AccessibilityNodeInfo? = service.rootInActiveWindow
} 