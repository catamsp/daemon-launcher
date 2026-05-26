package com.catamsp.Daemon.widgets.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import java.io.IOException

class DaemonVideoService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var mediaPlayer: MediaPlayer? = null
        private var gifDrawable: AnimatedImageDrawable? = null
        
        private var isVideo = false
        private var isVisibleState = false
        private var currentUriString: String? = null
        
        // Loop for GIF drawing
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isVisibleState && gifDrawable != null) {
                    drawGifFrame()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val prefs = getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            currentUriString = prefs.getString("video_uri", null)
        }

        private fun initMedia() {
            releaseMedia()
            if (currentUriString.isNullOrEmpty()) return

            try {
                val uri = Uri.parse(currentUriString)
                val mimeType = baseContext.contentResolver.getType(uri) ?: ""

                if (mimeType.startsWith("video/") || currentUriString!!.endsWith(".mp4") || currentUriString!!.endsWith(".mkv")) {
                    // --- VIDEO MODE ---
                    isVideo = true
                    mediaPlayer = MediaPlayer().apply {
                        setSurface(surfaceHolder.surface)
                        setDataSource(baseContext, uri)
                        isLooping = true
                        setVolume(0f, 0f) // Mute wallpaper
                        setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        setOnPreparedListener { 
                            if (isVisibleState) start() 
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.e("DaemonVideo", "MediaPlayer Error: $what, $extra")
                            true
                        }
                        prepareAsync()
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // --- GIF MODE ---
                    isVideo = false
                    val source = ImageDecoder.createSource(baseContext.contentResolver, uri)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    if (drawable is AnimatedImageDrawable) {
                        gifDrawable = drawable
                        gifDrawable?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        if (isVisibleState) {
                            gifDrawable?.start()
                            Choreographer.getInstance().postFrameCallback(frameCallback)
                        }
                        
                        // Force initial layout if surface is already known
                        val w = surfaceHolder.surfaceFrame.width()
                        val h = surfaceHolder.surfaceFrame.height()
                        if (w > 0 && h > 0) updateGifBounds(w, h)
                    } else {
                        drawStaticFallback(drawable)
                    }
                }
            } catch (e: Exception) {
                Log.e("DaemonVideo", "Failed to load media", e)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisibleState = visible
            if (visible) {
                if (isVideo) {
                    mediaPlayer?.start()
                } else {
                    gifDrawable?.start()
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            } else {
                if (isVideo) {
                    mediaPlayer?.pause()
                } else {
                    gifDrawable?.stop()
                    Choreographer.getInstance().removeFrameCallback(frameCallback)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            initMedia()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releaseMedia()
        }
        
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            updateGifBounds(width, height)
        }
        
        private fun updateGifBounds(width: Int, height: Int) {
            if (!isVideo && gifDrawable != null) {
                val scale = Math.max(width.toFloat() / gifDrawable!!.intrinsicWidth, height.toFloat() / gifDrawable!!.intrinsicHeight)
                val w = (gifDrawable!!.intrinsicWidth * scale).toInt()
                val h = (gifDrawable!!.intrinsicHeight * scale).toInt()
                val left = (width - w) / 2
                val top = (height - h) / 2
                gifDrawable?.setBounds(left, top, left + w, top + h)
                drawGifFrame()
            }
        }

        private fun drawGifFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null && gifDrawable != null) {
                    canvas.drawColor(Color.BLACK)
                    gifDrawable?.draw(canvas)
                }
            } catch (e: Exception) {
                Log.e("DaemonVideo", "Drawing error", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("DaemonVideo", "Unlock error", e)
                    }
                }
            }
        }
        
        private fun drawStaticFallback(drawable: Drawable) {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    val width = canvas.width
                    val height = canvas.height
                    val scale = Math.max(width.toFloat() / drawable.intrinsicWidth, height.toFloat() / drawable.intrinsicHeight)
                    val w = (drawable.intrinsicWidth * scale).toInt()
                    val h = (drawable.intrinsicHeight * scale).toInt()
                    val left = (width - w) / 2
                    val top = (height - h) / 2
                    drawable.setBounds(left, top, left + w, top + h)
                    drawable.draw(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun releaseMedia() {
            mediaPlayer?.release()
            mediaPlayer = null
            gifDrawable?.stop()
            gifDrawable = null
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "video_uri") {
                currentUriString = sharedPreferences?.getString(key, null)
                initMedia()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            releaseMedia()
            getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this)
        }
    }
}