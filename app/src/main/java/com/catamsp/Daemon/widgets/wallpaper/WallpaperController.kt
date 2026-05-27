package com.catamsp.Daemon.widgets.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.DisplayMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import com.catamsp.Daemon.services.DaemonVideoService

object WallpaperController {

    // Copies the user's selected file to our private internal storage to get a real file path
    // This bypasses volatile URI permissions that expire when the activity finishes.
    private fun cacheMediaFile(context: Context, sourceUri: Uri, prefix: String, extension: String): String? {
        return try {
            // Delete old cached files to save space
            context.filesDir.listFiles { _, name -> name.startsWith(prefix) }?.forEach { it.delete() }
            
            val filename = "${prefix}_${System.currentTimeMillis()}$extension"
            val file = File(context.filesDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun applyVideoWallpaper(activity: Activity, uri: Uri, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val videoPath = cacheMediaFile(activity, uri, "daemon_video_bg", ".mp4")
            if (videoPath != null) {
                // Save to private prefs for the service
                val prefs = activity.getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("video_path", videoPath).apply()

                withContext(Dispatchers.Main) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(activity, DaemonVideoService::class.java))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                    onSuccess()
                }
            }
        }
    }

    fun applyStaticWallpaper(activity: Activity, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Pre-cache the file so we have a reliable absolute path
                val imagePath = cacheMediaFile(activity, uri, "daemon_static_bg", ".jpg") 
                    ?: throw Exception("Failed to cache image")

                // 2. Get Screen Dimensions
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(metrics)
                val screenWidth = metrics.widthPixels
                val screenHeight = metrics.heightPixels

                // 3. Load the Bitmap efficiently
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                // Calculate inSampleSize to avoid OOM on massive images
                options.inSampleSize = calculateInSampleSize(options, screenWidth, screenHeight)
                options.inJustDecodeBounds = false
                
                val originalBitmap = BitmapFactory.decodeFile(imagePath, options)
                    ?: throw Exception("Failed to decode bitmap")

                // 4. Center-Crop the Bitmap
                val croppedBitmap = centerCropBitmap(originalBitmap, screenWidth, screenHeight)

                // 5. Apply to Android System
                val wallpaperManager = WallpaperManager.getInstance(activity)
                wallpaperManager.setBitmap(croppedBitmap)

                // Clean up memory
                if (originalBitmap != croppedBitmap) originalBitmap.recycle()

                // 6. Smooth Transition to Home Screen
                withContext(Dispatchers.Main) {
                    onSuccess()
                    activity.finish()
                    // Smooth cross-fade exit
                    @Suppress("DEPRECATION")
                    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        
        var x = 0
        var y = 0
        var width = src.width
        var height = src.height

        if (srcRatio > targetRatio) {
            // Image is wider than screen: crop the sides
            width = (src.height * targetRatio).toInt()
            x = (src.width - width) / 2
        } else {
            // Image is taller than screen: crop top/bottom
            height = (src.width / targetRatio).toInt()
            y = (src.height - height) / 2
        }

        val cropped = Bitmap.createBitmap(src, x, y, width, height)
        // Scale it to exact screen size to prevent OS stretching
        return Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
    }

    // Mode 2: External 3D / Live Wallpaper App
    fun launchNativeLiveWallpaperPicker(context: Context) {
        val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Mode 3: Custom Parallax Engine (Stubbed for Revert)
    fun applyParallaxWallpaper(context: Context, uri: Uri) {
        // Feature disabled during revert
    }
}