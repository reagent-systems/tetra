package com.example.simple_agent_android.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import java.io.ByteArrayOutputStream

object ScreenshotUtils {
    private val TAG = "ScreenshotUtils"
    
    fun captureView(view: View): String? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing view screenshot", e)
            null
        }
    }
    
    fun captureActivity(activity: Activity, callback: (String?) -> Unit) {
        try {
            val window: Window = activity.window
            val view = window.decorView.rootView
            
            // Method 1: Try to capture the root view directly
            val rootViewScreenshot = captureView(view)
            if (rootViewScreenshot != null) {
                callback(rootViewScreenshot)
                return
            }
            
            // Method 2: Fallback to pixel copy (Android 7.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                
                try {
                    PixelCopy.request(
                        window,
                        bitmap,
                        { result: Int ->
                            if (result == PixelCopy.SUCCESS) {
                                val base64 = bitmapToBase64(bitmap)
                                callback(base64)
                            } else {
                                Log.e(TAG, "PixelCopy failed with result: $result")
                                callback(null)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error with PixelCopy", e)
                    callback(null)
                }
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing activity screenshot", e)
            callback(null)
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to base64", e)
            null
        }
    }
    
    fun compressBitmap(bitmap: Bitmap, quality: Int = 80): String? {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing bitmap", e)
            null
        }
    }
} 