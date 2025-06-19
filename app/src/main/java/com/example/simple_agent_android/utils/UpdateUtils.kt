package com.example.simple_agent_android.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    sealed class DownloadStatus {
        object NotStarted : DownloadStatus()
        data class InProgress(val progress: Int) : DownloadStatus()
        object Completed : DownloadStatus()
        data class Failed(val error: String) : DownloadStatus()
    }

    suspend fun checkForUpdates(): UpdateCheckResult {
        return try {
            val currentVersionCode = getCurrentVersionCode()
            val currentVersionName = getCurrentVersionName()
            
            Log.d(TAG, "Current version: $currentVersionName (code: $currentVersionCode)")
            
            val latestVersionInfo = fetchLatestVersionInfo()
            
            Log.d(TAG, "Latest version: ${latestVersionInfo.versionName} (code: ${latestVersionInfo.versionCode})")
            
            when {
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
            UpdateCheckResult.CheckFailed(e.message ?: "Unknown error")
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

    fun downloadAndInstallUpdate(): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.NotStarted)
        
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // Create download request
            val request = DownloadManager.Request(Uri.parse(APK_DOWNLOAD_URL))
                .setTitle("Simple Agent Update")
                .setDescription("Downloading update")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "SimpleAgent-update.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadId = downloadManager.enqueue(request)
            
            // Register broadcast receiver for download completion
            val downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        context?.unregisterReceiver(this)
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                    installUpdate(Uri.parse(uriString))
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                                    Log.e(TAG, "Download failed: $reason")
                                }
                            }
                        }
                        cursor.close()
                    }
                }
            }
            
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
            
            // Monitor download progress
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                emit(DownloadStatus.InProgress(progress))
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            emit(DownloadStatus.Completed)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            emit(DownloadStatus.Failed("Download failed: $reason"))
                        }
                    }
                }
                cursor.close()
                kotlinx.coroutines.delay(500) // Update progress every 500ms
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during download", e)
            emit(DownloadStatus.Failed(e.message ?: "Unknown error"))
        }
    }

    private fun installUpdate(downloadedApkUri: Uri) {
        try {
            val apkFile = File(downloadedApkUri.path ?: return)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
        }
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