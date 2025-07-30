package com.example.simple_agent_android.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale

object DeviceInfoUtils {
    
    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }
    
    fun getDeviceInfo(context: Context): String {
        val displayMetrics = getDisplayMetrics(context)
        
        return buildString {
            append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append(", ${Build.MANUFACTURER} ${Build.MODEL}")
            append(", ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
            append(", ${displayMetrics.densityDpi}dpi")
            append(", ${Locale.getDefault().language}")
        }
    }
    
    private fun getDisplayMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }
    
    fun getDetailedDeviceInfo(context: Context): Map<String, String> {
        val displayMetrics = getDisplayMetrics(context)
        
        return mapOf(
            "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "android_version" to "Android ${Build.VERSION.RELEASE}",
            "api_level" to Build.VERSION.SDK_INT.toString(),
            "screen_resolution" to "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}",
            "screen_density" to "${displayMetrics.densityDpi}dpi",
            "locale" to Locale.getDefault().toString(),
            "app_version" to getAppVersion(context),
            "build_type" to Build.TYPE,
            "cpu_abi" to Build.SUPPORTED_ABIS.joinToString(", ")
        )
    }
} 