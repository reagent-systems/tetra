package com.example.simple_agent_android.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

class UpdateUtils(private val context: Context) {
    companion object {
        private const val TAG = "UpdateUtils"
        private const val VERSION_CHECK_URL = "https://cdn.bentlybro.com/SA-A/version.json"
        private const val APK_DOWNLOAD_URL = "https://cdn.bentlybro.com/SA-A/app-release.apk"
    }

    suspend fun checkForUpdates(): UpdateCheckResult {
        try {
            val currentVersionCode = getCurrentVersionCode()
            val currentVersionName = getCurrentVersionName()
            
            Log.d(TAG, "Current version: $currentVersionName (code: $currentVersionCode)")
            
            val latestVersionInfo = fetchLatestVersionInfo()
            
            Log.d(TAG, "Latest version: ${latestVersionInfo.versionName} (code: ${latestVersionInfo.versionCode})")
            
            return when {
                latestVersionInfo.versionCode > currentVersionCode -> {
                    Log.i(TAG, "Update available: ${latestVersionInfo.versionName}")
                    UpdateCheckResult.UpdateAvailable(
                        currentVersion = currentVersionName,
                        newVersion = latestVersionInfo.versionName,
                        updateLog = latestVersionInfo.updateLog
                    )
                }
                latestVersionInfo.versionCode == currentVersionCode -> {
                    Log.d(TAG, "No update needed - current version is up to date")
                    UpdateCheckResult.NoUpdateAvailable
                }
                else -> {
                    Log.w(TAG, "Local version is newer than remote version")
                    UpdateCheckResult.NoUpdateAvailable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return UpdateCheckResult.CheckFailed(e.message ?: "Unknown error")
        }
    }

    private suspend fun fetchLatestVersionInfo(): VersionInfo = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(VERSION_CHECK_URL)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            
            val jsonString = response.body?.string() ?: throw IOException("Empty response")
            val jsonObject = JSONObject(jsonString)
            
            VersionInfo(
                versionCode = jsonObject.getInt("versionCode"),
                versionName = jsonObject.getString("versionName"),
                updateLog = jsonObject.optString("updateLog", "No update details available.")
            )
        }
    }

    fun downloadAndInstallUpdate() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val request = DownloadManager.Request(Uri.parse(APK_DOWNLOAD_URL))
            .setTitle("Simple Agent Update")
            .setDescription("Downloading update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "SimpleAgent-update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        
        val downloadId = downloadManager.enqueue(request)
        
        triggerInstallation(downloadId)
    }

    private fun triggerInstallation(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = cursor.getInt(columnIndex)
            
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val downloadedApkUri = Uri.parse(cursor.getString(uriIndex))
                
                val apkFile = File(downloadedApkUri.path ?: return)
                val uri = FileProvider.getUriForFile(
                    context, 
                    "${context.packageName}.fileprovider", 
                    apkFile
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(intent)
            }
        }
        cursor.close()
    }

    private fun getCurrentVersionCode(): Int {
        return context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }

    private fun getCurrentVersionName(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }

    data class VersionInfo(
        val versionCode: Int,
        val versionName: String,
        val updateLog: String
    )

    sealed class UpdateCheckResult {
        object NoUpdateAvailable : UpdateCheckResult()
        data class UpdateAvailable(
            val currentVersion: String,
            val newVersion: String,
            val updateLog: String
        ) : UpdateCheckResult()
        data class CheckFailed(val errorMessage: String) : UpdateCheckResult()
    }
} 