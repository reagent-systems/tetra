package com.example.simple_agent_android.agentcore

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class UIElement(
    val text: String?,
    val contentDescription: String?,
    val className: String,
    val bounds: Bounds,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val priority: Int // Higher = more important
) {
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val centerX: Int get() = (left + right) / 2
        val centerY: Int get() = (top + bottom) / 2
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        val area: Int get() = width * height
    }
    
    val displayText: String get() = text ?: contentDescription ?: className.substringAfterLast('.')
    val isTextInput: Boolean get() = className.contains("EditText") || isEditable
    val isButton: Boolean get() = className.contains("Button") || (isClickable && !isTextInput)
}

data class ScreenAnalysis(
    val allElements: List<UIElement>,
    val interactableElements: List<UIElement>,
    val textInputs: List<UIElement>,
    val buttons: List<UIElement>,
    val scrollableAreas: List<UIElement>,
    val mainContent: List<UIElement>,
    val navigation: List<UIElement>,
    val screenType: ScreenType,
    val loadingState: LoadingState,
    val packageName: String? = null
)

enum class ScreenType {
    HOME_SCREEN,
    APP_LIST,
    SETTINGS,
    TEXT_INPUT_FORM,
    LIST_VIEW,
    DIALOG,
    WEB_VIEW,
    LOADING,
    ERROR,
    EMPTY,
    UNKNOWN
}

enum class LoadingState {
    LOADED,
    LOADING,
    EMPTY,
    ERROR
}

class ScreenParseException(message: String, cause: Throwable?) : Exception(message, cause)

object ScreenAnalyzer {
    private const val TAG = "ScreenAnalyzer"
    private const val MAX_PARSE_ATTEMPTS = 3
    private const val INITIAL_BACKOFF_MS = 200L
    
    fun analyzeScreen(screenJson: String): ScreenAnalysis {
        return analyzeScreenWithRetry(screenJson)
    }
    
    private fun analyzeScreenWithRetry(screenJson: String): ScreenAnalysis {
        var backoffMs = INITIAL_BACKOFF_MS
        var lastException: Exception? = null
        
        for (attempt in 1..MAX_PARSE_ATTEMPTS) {
            try {
                return performScreenParsing(screenJson)
            } catch (e: JSONException) {
                lastException = e
                Log.w(TAG, "Screen parsing attempt $attempt failed: ${e.message}")
                
                if (attempt < MAX_PARSE_ATTEMPTS) {
                    Log.i(TAG, "Retrying screen parsing in ${backoffMs}ms (attempt ${attempt + 1}/$MAX_PARSE_ATTEMPTS)")
                    try {
                        Thread.sleep(backoffMs)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    backoffMs *= 2 // Exponential backoff
                }
            } catch (e: Exception) {
                // For non-JSON exceptions, don't retry as they're likely not transient
                Log.e(TAG, "Non-recoverable error analyzing screen", e)
                lastException = e
                break
            }
        }
        
        // If all retries failed, throw the custom exception
        Log.e(TAG, "Failed to parse screen after $MAX_PARSE_ATTEMPTS attempts")
        throw ScreenParseException("Failed to parse screen after $MAX_PARSE_ATTEMPTS attempts", lastException)
    }
    
    private fun performScreenParsing(screenJson: String): ScreenAnalysis {
        val elements = parseElements(screenJson)
        val interactableElements = elements.filter { it.isClickable && it.isEnabled }
        val packageName = extractPackageName(screenJson)
        
        return ScreenAnalysis(
            allElements = elements,
            interactableElements = interactableElements,
            textInputs = elements.filter { it.isTextInput },
            buttons = elements.filter { it.isButton },
            scrollableAreas = elements.filter { it.isScrollable },
            mainContent = categorizeAsMainContent(elements),
            navigation = categorizeAsNavigation(elements),
            screenType = determineScreenType(elements),
            loadingState = determineLoadingState(elements),
            packageName = packageName
        )
    }
    
    private fun extractPackageName(screenJson: String): String? {
        try {
            val jsonArray = JSONArray(screenJson)
            if (jsonArray.length() > 0) {
                val firstElement = jsonArray.getJSONObject(0)
                return firstElement.optString("packageName").takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting package name", e)
        }
        return null
    }
    
    private fun parseElements(screenJson: String): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        
        try {
            val jsonArray = JSONArray(screenJson)
            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.getJSONObject(i)
                elements.add(parseElement(element))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing screen JSON", e)
        }
        
        return elements.sortedByDescending { it.priority }
    }
    
    private fun parseElement(element: JSONObject): UIElement {
        // Handle missing or malformed bounds gracefully
        val boundsJson = element.optJSONObject("bounds")
        val uiBounds = if (boundsJson != null) {
            UIElement.Bounds(
                left = boundsJson.optInt("left", 0),
                top = boundsJson.optInt("top", 0),
                right = boundsJson.optInt("right", 0),
                bottom = boundsJson.optInt("bottom", 0)
            )
        } else {
            // If bounds is completely missing, create a default bounds
            Log.w(TAG, "Element missing bounds, using default: ${element.optString("className", "unknown")}")
            UIElement.Bounds(left = 0, top = 0, right = 0, bottom = 0)
        }
        
        val text = element.optString("text").takeIf { it.isNotEmpty() }
        val contentDescription = element.optString("contentDescription").takeIf { it.isNotEmpty() }
        val className = element.optString("className", "unknown")
        val isClickable = element.optBoolean("clickable", false)
        val isEnabled = element.optBoolean("enabled", true)
        val isScrollable = element.optBoolean("scrollable", false)
        val isEditable = element.optBoolean("editable", false)
        
        val priority = calculatePriority(text, contentDescription, className, uiBounds, isClickable, isEnabled)
        
        return UIElement(
            text = text,
            contentDescription = contentDescription,
            className = className,
            bounds = uiBounds,
            isClickable = isClickable,
            isEnabled = isEnabled,
            isScrollable = isScrollable,
            isEditable = isEditable,
            priority = priority
        )
    }
    
    private fun calculatePriority(
        text: String?,
        contentDescription: String?,
        className: String,
        bounds: UIElement.Bounds,
        isClickable: Boolean,
        isEnabled: Boolean
    ): Int {
        var priority = 0
        
        // Base priority for interactable elements
        if (isClickable && isEnabled) priority += 50
        
        // Higher priority for buttons and text inputs
        when {
            className.contains("Button") -> priority += 30
            className.contains("EditText") -> priority += 40
            className.contains("TextView") && isClickable -> priority += 25
            className.contains("ImageView") && isClickable -> priority += 20
        }
        
        // Priority based on text content
        val displayText = (text ?: contentDescription ?: "").lowercase()
        when {
            displayText.contains("submit") || displayText.contains("send") || displayText.contains("ok") -> priority += 25
            displayText.contains("next") || displayText.contains("continue") -> priority += 20
            displayText.contains("search") -> priority += 20
            displayText.contains("login") || displayText.contains("sign in") -> priority += 20
            displayText.contains("cancel") || displayText.contains("back") -> priority += 10
            displayText.contains("menu") || displayText.contains("more") -> priority += 15
        }
        
        // Size-based priority (medium-sized elements are often more important)
        val area = bounds.area
        when {
            area > 50000 -> priority -= 10 // Very large elements (often backgrounds)
            area in 5000..50000 -> priority += 10 // Good-sized interactive elements
            area in 1000..5000 -> priority += 5 // Small but potentially important
            area < 1000 -> priority -= 5 // Very small elements
        }
        
        // Position-based priority (center and top are often more important)
        if (bounds.centerY < 800) priority += 5 // Top of screen
        if (bounds.centerX in 200..800) priority += 5 // Center horizontally
        
        return priority
    }
    
    private fun categorizeAsMainContent(elements: List<UIElement>): List<UIElement> {
        return elements.filter { element ->
            val className = element.className.lowercase()
            val displayText = element.displayText.lowercase()
            
            // Main content indicators
            className.contains("recycler") ||
            className.contains("listview") ||
            className.contains("textview") && element.bounds.area > 10000 ||
            (element.isClickable && !displayText.contains("menu") && !displayText.contains("back"))
        }
    }
    
    private fun categorizeAsNavigation(elements: List<UIElement>): List<UIElement> {
        return elements.filter { element ->
            val displayText = element.displayText.lowercase()
            val className = element.className.lowercase()
            
            // Navigation indicators
            displayText.contains("menu") ||
            displayText.contains("back") ||
            displayText.contains("home") ||
            displayText.contains("up") ||
            className.contains("toolbar") ||
            className.contains("actionbar") ||
            (element.bounds.top < 200 && element.isClickable) // Top navigation area
        }
    }
    
    private fun determineScreenType(elements: List<UIElement>): ScreenType {
        val textInputCount = elements.count { it.isTextInput }
        val buttonCount = elements.count { it.isButton }
        val scrollableCount = elements.count { it.isScrollable }
        
        return when {
            elements.isEmpty() -> ScreenType.EMPTY
            textInputCount >= 2 -> ScreenType.TEXT_INPUT_FORM
            scrollableCount > 0 && elements.size > 10 -> ScreenType.LIST_VIEW
            elements.any { it.displayText.lowercase().contains("loading") } -> ScreenType.LOADING
            elements.size < 5 && buttonCount >= 2 -> ScreenType.DIALOG
            elements.any { it.className.contains("WebView") } -> ScreenType.WEB_VIEW
            elements.any { it.displayText.lowercase().contains("settings") } -> ScreenType.SETTINGS
            else -> ScreenType.UNKNOWN
        }
    }
    
    private fun determineLoadingState(elements: List<UIElement>): LoadingState {
        return when {
            elements.isEmpty() -> LoadingState.EMPTY
            elements.any { it.displayText.lowercase().contains("loading") } -> LoadingState.LOADING
            elements.any { it.displayText.lowercase().contains("error") } -> LoadingState.ERROR
            else -> LoadingState.LOADED
        }
    }
    
    fun findBestElementForTask(analysis: ScreenAnalysis, taskDescription: String): UIElement? {
        val taskLower = taskDescription.lowercase()
        
        // Look for elements that match the task description
        val candidates = analysis.interactableElements.filter { element ->
            val displayText = element.displayText.lowercase()
            
            // Direct text matching
            taskLower.split(" ").any { word ->
                displayText.contains(word) && word.length > 2
            }
        }
        
        // Return highest priority matching element, or highest priority element overall
        return candidates.maxByOrNull { it.priority } ?: analysis.interactableElements.firstOrNull()
    }
} 