package com.example.simple_agent_android.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.example.simple_agent_android.utils.SharedPrefsUtils

class AccessibilityNodeHandler(private val service: AccessibilityService) {
    private val TAG = "AccessibilityNodeHandler"

    fun setTextAt(x: Int, y: Int, text: String) {
        try {
            // Apply offset correction - reverse the offset applied in InteractiveElementUtils
            val verticalOffset = SharedPrefsUtils.getVerticalOffset(service.applicationContext)
            val originalX = x
            val originalY = y - verticalOffset
            
            Log.d(TAG, "setTextAt called with coordinates ($x, $y) and text: '$text'")
            Log.d(TAG, "Original coordinates: ($originalX, $originalY), offset: $verticalOffset")
            
            val rootNode = service.rootInActiveWindow
            if (rootNode == null) {
                Log.e(TAG, "No root node available")
                return
            }
            
            // Find the editable node at the coordinates
            val editableNode = findEditableNodeAt(rootNode, originalX, originalY)
            
            if (editableNode != null) {
                Log.d(TAG, "Found editable node: ${editableNode.className}")
                
                // ===== COMPREHENSIVE NODE INSPECTION =====
                logNodeProperties(editableNode, "TARGET_TEXT_FIELD")
                
                setTextInNode(editableNode, text)
            } else {
                Log.d(TAG, "No editable node found at coordinates ($originalX, $originalY)")
                
                // Debug: Show all nodes near the target coordinates
                debugAllNodesNear(rootNode, originalX, originalY)
                
                // Fallback: try to find ANY editable node on screen
                val anyEditableNode = findAnyEditableNode(rootNode)
                if (anyEditableNode != null) {
                    Log.d(TAG, "Found fallback editable node: ${anyEditableNode.className}")
                    logNodeProperties(anyEditableNode, "FALLBACK_TEXT_FIELD")
                    setTextInNode(anyEditableNode, text)
                } else {
                    Log.e(TAG, "No editable nodes found on screen")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setTextAt", e)
        }
    }
    
    private fun setTextInNode(node: AccessibilityNodeInfo, text: String) {
        try {
            Log.d(TAG, "Setting text in node: '$text'")
            
            // Step 1: Check if we can focus the node
            if (node.isFocusable && !node.isFocused) {
                Log.d(TAG, "Node is focusable but not focused, attempting to focus")
                val focusResult = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Log.d(TAG, "Focus action result: $focusResult")
                Thread.sleep(300) // Wait for focus to take effect
                
                // Log focus state after action
                Log.d(TAG, "Node focused after action: ${node.isFocused}")
            } else if (node.isFocused) {
                Log.d(TAG, "Node is already focused")
            } else {
                Log.d(TAG, "Node is not focusable")
            }
            
            // Step 2: Check if we can click the node (some fields need to be clicked first)
            if (node.isClickable && !node.isFocused) {
                Log.d(TAG, "Node is clickable but not focused, attempting click")
                val clickResult = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Click action result: $clickResult")
                Thread.sleep(300) // Wait for click to take effect
            }
            
            // Step 3: Attempt to set text directly
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Check if the node supports SET_TEXT action
                val hasSetTextAction = node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
                if (hasSetTextAction) {
                    Log.d(TAG, "Node supports SET_TEXT action, attempting direct text setting")
                    val args = Bundle()
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    val setTextResult = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    Log.d(TAG, "Set text action result: $setTextResult")
                    
                    if (setTextResult) {
                        Thread.sleep(300) // Wait for text to be set
                        val currentText = node.text?.toString() ?: ""
                        Log.d(TAG, "Text after SET_TEXT - expected: '$text', actual: '$currentText'")
                        
                        // If text was set successfully, we're done
                        if (currentText == text || currentText.contains(text)) {
                            Log.d(TAG, "Text successfully set via SET_TEXT action")
                            return
                        }
                    }
                } else {
                    Log.d(TAG, "Node does not support SET_TEXT action")
                }
            }
            
            // Step 4: Fallback to clipboard method
            Log.d(TAG, "Attempting clipboard fallback method")
            tryClipboardFallback(node, text)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting text in node", e)
        }
    }
    
    private fun tryClipboardFallback(node: AccessibilityNodeInfo, text: String) {
        try {
            Log.d(TAG, "=== CLIPBOARD FALLBACK METHOD ===")
            
            val clipboard = service.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboard == null) {
                Log.e(TAG, "Clipboard service not available")
                return
            }
            
            // Set text to clipboard
            val clip = android.content.ClipData.newPlainText("agent_text", text)
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "Text copied to clipboard: '$text'")
            Thread.sleep(100)
            
            // Check if node supports paste action
            val hasPasteAction = node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }
            Log.d(TAG, "Node supports PASTE action: $hasPasteAction")
            
            if (hasPasteAction) {
                // Try to select all existing text first
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    try {
                        val hasSelectAllAction = node.actionList.any { it.id == 64 } // ACTION_SELECT_ALL
                        Log.d(TAG, "Node supports SELECT_ALL action: $hasSelectAllAction")
                        
                        if (hasSelectAllAction) {
                            val selectAllResult = node.performAction(64) // ACTION_SELECT_ALL
                            Log.d(TAG, "Select all action result: $selectAllResult")
                            Thread.sleep(100)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Select all action failed: ${e.message}")
                    }
                }
                
                // Paste the text
                val pasteResult = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                Log.d(TAG, "Paste action result: $pasteResult")
                
                if (pasteResult) {
                    Thread.sleep(300) // Wait for paste to complete
                    val currentText = node.text?.toString() ?: ""
                    Log.d(TAG, "Text after PASTE - expected: '$text', actual: '$currentText'")
                    
                    if (currentText == text || currentText.contains(text)) {
                        Log.d(TAG, "Text successfully set via clipboard paste")
                    } else {
                        Log.w(TAG, "Paste action succeeded but text doesn't match")
                    }
                } else {
                    Log.w(TAG, "Paste action failed")
                }
            } else {
                Log.w(TAG, "Node does not support paste action, trying alternative methods")
                
                // Alternative: Try to simulate key events (this is more complex and may not work)
                Log.d(TAG, "No alternative clipboard methods available")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error with clipboard fallback", e)
        }
    }

    private fun findEditableNodeAt(root: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)
        
        // Check if this node is editable and contains the point
        if (rect.contains(x, y) && (root.isEditable || isTextInputNode(root))) {
            Log.d(TAG, "Found editable node at exact coordinates: ${root.className}")
            return root
        }
        
        // Recursively search children
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            val result = child?.let { findEditableNodeAt(it, x, y) }
            if (result != null) {
                return result
            }
        }
        return null
    }
    
    private fun findNearbyEditableNode(root: AccessibilityNodeInfo, x: Int, y: Int, radius: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)
        
        // Check if this node is editable and within radius
        if ((root.isEditable || isTextInputNode(root)) && isWithinRadius(rect, x, y, radius)) {
            Log.d(TAG, "Found nearby editable node: ${root.className}")
            return root
        }
        
        // Recursively search children
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            val result = child?.let { findNearbyEditableNode(it, x, y, radius) }
            if (result != null) {
                return result
            }
        }
        return null
    }
    
    private fun isTextInputNode(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""
        return className.contains("EditText") || 
               className.contains("TextInputEditText") ||
               className.contains("AutoCompleteTextView") ||
               (node.isFocusable && node.isClickable && className.contains("Text"))
    }
    
    private fun isWithinRadius(rect: Rect, x: Int, y: Int, radius: Int): Boolean {
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val distance = kotlin.math.sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble())
        return distance <= radius
    }

    private fun debugAllNodesNear(root: AccessibilityNodeInfo, targetX: Int, targetY: Int) {
        Log.d(TAG, "=== DEBUG: All nodes near target ($targetX, $targetY) ===")
        debugNodeRecursive(root, targetX, targetY, 0)
    }
    
    private fun debugNodeRecursive(node: AccessibilityNodeInfo?, targetX: Int, targetY: Int, depth: Int) {
        if (node == null) return
        
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val distance = kotlin.math.sqrt(((targetX - rect.centerX()) * (targetX - rect.centerX()) + (targetY - rect.centerY()) * (targetY - rect.centerY())).toDouble())
        
        if (distance <= 200) { // Only log nodes within 200px of target
            val indent = "  ".repeat(depth)
            Log.d(TAG, "${indent}Node: ${node.className} at $rect, distance: ${distance.toInt()}px")
            Log.d(TAG, "${indent}  - isEditable: ${node.isEditable}")
            Log.d(TAG, "${indent}  - isClickable: ${node.isClickable}")
            Log.d(TAG, "${indent}  - isFocusable: ${node.isFocusable}")
            Log.d(TAG, "${indent}  - text: '${node.text}'")
            Log.d(TAG, "${indent}  - contentDescription: '${node.contentDescription}'")
            Log.d(TAG, "${indent}  - isTextInput: ${isTextInputNode(node)}")
        }
        
        for (i in 0 until node.childCount) {
            debugNodeRecursive(node.getChild(i), targetX, targetY, depth + 1)
        }
    }
    
    private fun findAnyEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable || isTextInputNode(root)) {
            return root
        }
        
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            val result = child?.let { findAnyEditableNode(it) }
            if (result != null) {
                return result
            }
        }
        return null
    }

    fun getRootNode(): AccessibilityNodeInfo? = service.rootInActiveWindow

    private fun logNodeProperties(node: AccessibilityNodeInfo, label: String) {
        Log.d(TAG, "=== NODE PROPERTIES: $label ===")
        Log.d(TAG, "  Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "  className: ${node.className}")
        Log.d(TAG, "  text: '${node.text}'")
        Log.d(TAG, "  contentDescription: '${node.contentDescription}'")
        
        // Basic properties
        Log.d(TAG, "  isFocusable(): ${node.isFocusable}")
        Log.d(TAG, "  isFocused(): ${node.isFocused}")
        Log.d(TAG, "  isClickable(): ${node.isClickable}")
        Log.d(TAG, "  isEnabled(): ${node.isEnabled}")
        Log.d(TAG, "  isVisibleToUser(): ${node.isVisibleToUser}")
        Log.d(TAG, "  isEditable(): ${node.isEditable}")
        
        // Check available actions
        val actions = node.actionList
        Log.d(TAG, "  Available actions (${actions.size}):")
        for (action in actions) {
            Log.d(TAG, "    - ${action.id}: ${action.label}")
        }
        
        // Check for specific important actions
        val hasClickAction = actions.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
        val hasFocusAction = actions.any { it.id == AccessibilityNodeInfo.ACTION_FOCUS }
        val hasSetTextAction = actions.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
        val hasPasteAction = actions.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }
        
        Log.d(TAG, "  hasClickAction: $hasClickAction")
        Log.d(TAG, "  hasFocusAction: $hasFocusAction")
        Log.d(TAG, "  hasSetTextAction: $hasSetTextAction")
        Log.d(TAG, "  hasPasteAction: $hasPasteAction")
        
        // Bounds information
        val rect = Rect()
        node.getBoundsInScreen(rect)
        Log.d(TAG, "  bounds: $rect")
        Log.d(TAG, "  center: (${rect.centerX()}, ${rect.centerY()})")
        
        // Input type information if available
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Log.d(TAG, "  inputType: ${node.inputType}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "  inputType: not available")
        }
        
        Log.d(TAG, "=== END NODE PROPERTIES ===")
    }
} 