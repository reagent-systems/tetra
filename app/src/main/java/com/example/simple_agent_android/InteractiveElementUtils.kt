package com.example.simple_agent_android

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.simple_agent_android.BoundingBoxAccessibilityService
import org.json.JSONArray
import org.json.JSONObject

object InteractiveElementUtils {
    fun getInteractiveElementsJson(service: BoundingBoxAccessibilityService?): String {
        val root = service?.rootInActiveWindow ?: return "{}"
        val elements = mutableListOf<JSONObject>()
        fun getFirstNonEmptyTextOrDesc(node: AccessibilityNodeInfo?): Pair<String, String> {
            if (node == null) return "" to ""
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if (text.isNotEmpty() || desc.isNotEmpty()) return text to desc
            for (i in 0 until node.childCount) {
                val result = getFirstNonEmptyTextOrDesc(node.getChild(i))
                if (result.first.isNotEmpty() || result.second.isNotEmpty()) return result
            }
            return "" to ""
        }

        fun collect(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.packageName == "com.example.simple_agent_android") return
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if ((node.isClickable || node.isFocusable || node.isLongClickable) && !rect.isEmpty) {
                val obj = JSONObject()
                var text = node.text?.toString() ?: ""
                var contentDescription = node.contentDescription?.toString() ?: ""
                if (text.isEmpty() && contentDescription.isEmpty()) {
                    val fallback = getFirstNonEmptyTextOrDesc(node)
                    text = fallback.first
                    contentDescription = fallback.second
                }
                if (text.isNotEmpty()) obj.put("text", text)
                if (contentDescription.isNotEmpty()) obj.put("contentDescription", contentDescription)
                val className = node.className?.toString() ?: ""
                if (className.isNotEmpty()) obj.put("className", className)
                val resourceId = node.viewIdResourceName ?: ""
                if (resourceId.isNotEmpty()) obj.put("resourceId", resourceId)
                val packageName = node.packageName?.toString() ?: ""
                if (packageName.isNotEmpty()) obj.put("packageName", packageName)
                obj.put("x", rect.left)
                obj.put("y", rect.top)
                obj.put("width", rect.width())
                obj.put("height", rect.height())
                elements.add(obj)
            }
            for (i in 0 until node.childCount) {
                collect(node.getChild(i))
            }
        }
        collect(root)
        val arr = JSONArray(elements)
        return arr.toString(2)
    }
} 