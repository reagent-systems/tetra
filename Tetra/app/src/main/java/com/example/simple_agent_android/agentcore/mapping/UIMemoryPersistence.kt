package com.example.simple_agent_android.agentcore.mapping

import android.content.Context
import android.util.Log
import com.example.simple_agent_android.utils.SharedPrefsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.io.FileReader

/**
 * Handles persistence of UI memory data across app sessions
 */
object UIMemoryPersistence {
    private const val TAG = "UIMemoryPersistence"
    private const val MEMORY_FILE_NAME = "ui_memory.json"
    private const val BACKUP_FILE_NAME = "ui_memory_backup.json"
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    @Serializable
    data class SerializableScreenState(
        val screenId: String,
        val appPackage: String,
        val screenTitle: String?,
        val lastVisited: Long,
        val visitCount: Int,
        val confidence: Float,
        val screenType: String,
        val isStable: Boolean,
        val metadata: Map<String, String>
    )
    
    @Serializable
    data class SerializableElementFingerprint(
        val semanticId: String,
        val text: String?,
        val contentDescription: String?,
        val className: String,
        val bounds: SerializableBounds,
        val parentContext: String,
        val actionType: String,
        val isClickable: Boolean,
        val isEditable: Boolean,
        val isScrollable: Boolean,
        val priority: Int,
        val lastSeen: Long,
        val confidence: Float
    )
    
    @Serializable
    data class SerializableBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
    
    @Serializable
    data class SerializableNavigationPath(
        val fromScreenId: String,
        val toScreenId: String,
        val successRate: Float,
        val lastUsed: Long,
        val useCount: Int,
        val averageTimeMs: Long,
        val confidence: Float,
        val isObsolete: Boolean,
        val metadata: Map<String, String>
    )
    
    @Serializable
    data class UIMemoryData(
        val screenStates: List<SerializableScreenState>,
        val elementFingerprints: List<SerializableElementFingerprint>,
        val navigationPaths: List<SerializableNavigationPath>,
        val lastSaved: Long
    )
    
    /**
     * Save UI memory data to persistent storage
     */
    suspend fun saveUIMemory(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val memoryFile = File(context.filesDir, MEMORY_FILE_NAME)
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                
                // Create backup of existing file
                if (memoryFile.exists()) {
                    memoryFile.copyTo(backupFile, overwrite = true)
                }
                
                // Get current memory data (this would need to be implemented in UIMemoryManager)
                val memoryData = getCurrentMemoryData()
                
                // Serialize and save
                val jsonString = json.encodeToString(memoryData)
                FileWriter(memoryFile).use { writer ->
                    writer.write(jsonString)
                }
                
                Log.d(TAG, "UI memory saved successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving UI memory", e)
                false
            }
        }
    }
    
    /**
     * Load UI memory data from persistent storage
     */
    suspend fun loadUIMemory(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val memoryFile = File(context.filesDir, MEMORY_FILE_NAME)
                if (!memoryFile.exists()) {
                    Log.d(TAG, "No UI memory file found, starting fresh")
                    return@withContext true
                }
                
                val jsonString = FileReader(memoryFile).readText()
                val memoryData = json.decodeFromString<UIMemoryData>(jsonString)
                
                // Restore memory data (this would need to be implemented in UIMemoryManager)
                restoreMemoryData(memoryData)
                
                Log.d(TAG, "UI memory loaded successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error loading UI memory", e)
                // Try to restore from backup
                restoreFromBackup(context)
            }
        }
    }
    
    /**
     * Clear all UI memory data
     */
    suspend fun clearUIMemory(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val memoryFile = File(context.filesDir, MEMORY_FILE_NAME)
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                
                if (memoryFile.exists()) {
                    memoryFile.delete()
                }
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                
                Log.d(TAG, "UI memory cleared successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing UI memory", e)
                false
            }
        }
    }
    
    /**
     * Get memory file size for debugging
     */
    fun getMemoryFileSize(context: Context): Long {
        val memoryFile = File(context.filesDir, MEMORY_FILE_NAME)
        return if (memoryFile.exists()) memoryFile.length() else 0L
    }
    
    private fun getCurrentMemoryData(): UIMemoryData {
        // This would need to be implemented in UIMemoryManager to expose current data
        // For now, return empty data
        return UIMemoryData(
            screenStates = emptyList(),
            elementFingerprints = emptyList(),
            navigationPaths = emptyList(),
            lastSaved = System.currentTimeMillis()
        )
    }
    
    private fun restoreMemoryData(memoryData: UIMemoryData) {
        // This would need to be implemented in UIMemoryManager to restore data
        // For now, do nothing
    }
    
    private suspend fun restoreFromBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                if (!backupFile.exists()) {
                    Log.d(TAG, "No backup file found")
                    return@withContext false
                }
                
                val jsonString = FileReader(backupFile).readText()
                val memoryData = json.decodeFromString<UIMemoryData>(jsonString)
                
                restoreMemoryData(memoryData)
                
                Log.d(TAG, "UI memory restored from backup")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring from backup", e)
                false
            }
        }
    }
}
