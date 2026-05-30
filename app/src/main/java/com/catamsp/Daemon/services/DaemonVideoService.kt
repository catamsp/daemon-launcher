package com.catamsp.Daemon.services

import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.util.Log
import android.content.Context

class DaemonVideoService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private var fileInputStream: java.io.FileInputStream? = null
        private var isSurfaceValid = false
        private var isPrepared = false

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            isSurfaceValid = true
            setupMediaPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isSurfaceValid = false
            releaseMediaPlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            
            // Only attempt playback commands if the player is fully loaded
            if (isPrepared && isSurfaceValid) {
                if (visible) {
                    mediaPlayer?.start()
                } else {
                    mediaPlayer?.pause()
                }
            }
        }

        private fun setupMediaPlayer(holder: SurfaceHolder) {
            // Fetch the cached absolute path from private storage preferences
            val prefs = getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
            val videoPath = prefs.getString("video_path", null)
            
            if (videoPath.isNullOrEmpty()) return

            try {
                mediaPlayer = MediaPlayer().apply {
                    setSurface(holder.surface)
                    
                    // CRITICAL FIX: Use a FileDescriptor so the mediaserver process 
                    // can read our private internal storage without permission errors.
                    val videoFile = java.io.File(videoPath)
                    if (!videoFile.exists()) return
                    
                    fileInputStream = java.io.FileInputStream(videoFile)
                    setDataSource(fileInputStream!!.fd)

                    isLooping = true
                    setVolume(0f, 0f) // Mute the wallpaper
                    
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)

                    isPrepared = false // Reset the lock

                    setOnPreparedListener { 
                        isPrepared = true // Unlock!
                        if (isVisible) start() 
                    }

                    setOnErrorListener { _, what, extra ->
                        // Prevent crash loops on bad files
                        Log.e("DaemonVideo", "MediaPlayer Error: $what, $extra")
                        isPrepared = false
                        isSurfaceValid = false // Add this to be safe
                        releaseMediaPlayer()
                        true // Return true to indicate the error was handled
                    }

                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("DaemonVideoService", "Failed to set up media player", e)
                releaseMediaPlayer()
            }
        }

        private fun releaseMediaPlayer() {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
            } catch (e: Exception) {
                Log.e("DaemonVideoService", "Error releasing media player", e)
            } finally {
                mediaPlayer = null
                
                try {
                    fileInputStream?.close()
                } catch (e: Exception) {
                    Log.e("DaemonVideoService", "Error closing file input stream", e)
                } finally {
                    fileInputStream = null
                }
            }
        }
    }
}