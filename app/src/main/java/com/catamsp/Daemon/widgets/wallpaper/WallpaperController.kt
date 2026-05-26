package com.catamsp.Daemon.widgets.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object WallpaperController {

    // Copies the user's selected file to our private internal storage.
    // This prevents Android from revoking our URI read permissions when the picker closes.
    private fun cacheMediaFile(context: Context, sourceUri: Uri, filename: String): Uri? {
        return try {
            val file = File(context.filesDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    // Unified Engine Route
    fun applyDaemonWallpaper(context: Context, uri: Uri, isParallax: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            val cachedUri = cacheMediaFile(context, uri, "daemon_parallax_bg")
            if (cachedUri != null) {
                val prefs = context.getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("parallax_uri", cachedUri.toString())
                    .putBoolean("is_parallax", isParallax)
                    .apply()

                launch(Dispatchers.Main) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, DaemonParallaxService::class.java))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    // Video / GIF Engine
    fun applyVideoWallpaper(context: Context, uri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            // We need the mime type to know if it's a video or gif later
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val extension = if (mimeType.startsWith("video/") || uri.toString().endsWith(".mp4")) ".mp4" else ".gif"
            
            val cachedUri = cacheMediaFile(context, uri, "daemon_video_bg$extension")
            if (cachedUri != null) {
                val prefs = context.getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("video_uri", cachedUri.toString()).apply()

                launch(Dispatchers.Main) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, DaemonVideoService::class.java))
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    // External 3D / Live Wallpaper App
    fun launchNativeLiveWallpaperPicker(context: Context) {
        try {
            val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Select Wallpaper App"))
            } catch (e2: Exception) {
                // Ignore if everything fails
            }
        }
    }
}