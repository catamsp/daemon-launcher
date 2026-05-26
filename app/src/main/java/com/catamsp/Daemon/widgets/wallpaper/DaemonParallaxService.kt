package com.catamsp.Daemon.widgets.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import android.view.Choreographer
import kotlinx.coroutines.*
import kotlin.math.abs

class DaemonParallaxService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine(), SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener, Choreographer.FrameCallback {
        
        private var sensorManager: SensorManager? = null
        private var rotationSensor: Sensor? = null
        private var backgroundBitmap: Bitmap? = null
        
        private val paint = Paint().apply { 
            isFilterBitmap = true
            isAntiAlias = true
        }
        
        private val engineJob = Job()
        private val engineScope = CoroutineScope(Dispatchers.Main + engineJob)

        // Rendering state
        private val scaleFactor = 1.15f // 15% larger to allow shifting
        private var currentOffsetX = 0f
        private var currentOffsetY = 0f
        private var targetOffsetX = 0f
        private var targetOffsetY = 0f
        private val maxOffset = 100f 
        private val lerpFactor = 0.2f // Increased for snappier, less "slow" feel

        // Pre-allocated objects for zero-allocation draw loop
        private val drawMatrix = Matrix()
        private var baseScale = 1f
        private var baseCenterX = 0f
        private var baseCenterY = 0f
        private var canvasWidth = 0
        private var canvasHeight = 0

        // Sensor state
        private val rotationMatrix = FloatArray(9)
        private val orientationValues = FloatArray(3)
        private var basePitch = 0f
        private var baseRoll = 0f
        private var isFirstSensorReading = true
        private var isParallaxEnabled = true

        // Transition Animation State
        private var transitionOldBitmap: Bitmap? = null
        private var transitionProgress = 1f
        private var isTransitioning = false
        private val transitionPath = Path()

        // Animation Loop
        private var isAnimating = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            
            val prefs = getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            isParallaxEnabled = prefs.getBoolean("is_parallax", true)
            loadBitmapSafe(prefs.getString("parallax_uri", null))
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                isFirstSensorReading = true
                if (isParallaxEnabled) {
                    rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                }
                startAnimation()
            } else {
                sensorManager?.unregisterListener(this)
                stopAnimation()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            canvasWidth = width
            canvasHeight = height
            recalculateBaseTransforms()
            drawFrame()
        }

        private fun recalculateBaseTransforms() {
            val bmp = backgroundBitmap ?: return
            if (canvasWidth == 0 || canvasHeight == 0) return

            val canvasW = canvasWidth.toFloat()
            val canvasH = canvasHeight.toFloat()
            val bmpW = bmp.width.toFloat()
            val bmpH = bmp.height.toFloat()

            // If static, we don't need the extra scale margin for shifting
            val currentScaleFactor = if (isParallaxEnabled) scaleFactor else 1.0f

            baseScale = Math.max(canvasW / bmpW, canvasH / bmpH) * currentScaleFactor
            baseCenterX = (canvasW - bmpW * baseScale) / 2f
            baseCenterY = (canvasH - bmpH * baseScale) / 2f
            
            if (!isParallaxEnabled) {
                targetOffsetX = 0f
                targetOffsetY = 0f
            }
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (!isParallaxEnabled) return
            
            if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationValues)

                val pitch = orientationValues[1]
                val roll = orientationValues[2]

                // Calibrate instantly on first read to avoid massive initial jump
                if (isFirstSensorReading) {
                    basePitch = pitch
                    baseRoll = roll
                    isFirstSensorReading = false
                } else {
                    // Slowly adapt the baseline to the current holding angle over time
                    // This fixes the issue where holding the phone at a slant permanently clips the vertical axis
                    basePitch += (pitch - basePitch) * 0.02f
                    baseRoll += (roll - baseRoll) * 0.02f
                }

                val deltaPitch = pitch - basePitch
                val deltaRoll = roll - baseRoll

                // Set targets based on relative movement
                targetOffsetX = (deltaRoll * 250f).coerceIn(-maxOffset, maxOffset)
                targetOffsetY = (deltaPitch * 250f).coerceIn(-maxOffset, maxOffset)
                
                startAnimation()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        
        private fun startAnimation() {
            if (!isAnimating && isVisible) {
                isAnimating = true
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        
        private fun stopAnimation() {
            isAnimating = false
            Choreographer.getInstance().removeFrameCallback(this)
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!isAnimating || !isVisible) return
            
            // Linear Interpolation (Lerp) towards the target
            currentOffsetX += (targetOffsetX - currentOffsetX) * lerpFactor
            currentOffsetY += (targetOffsetY - currentOffsetY) * lerpFactor
            
            // Drive transition progress
            if (isTransitioning) {
                transitionProgress += 0.03f // Controls animation speed
                if (transitionProgress >= 1f) {
                    transitionProgress = 1f
                    isTransitioning = false
                    transitionOldBitmap?.recycle()
                    transitionOldBitmap = null
                }
            }
            
            drawFrame()
            
            val dx = abs(targetOffsetX - currentOffsetX)
            val dy = abs(targetOffsetY - currentOffsetY)
            
            if (dx > 0.1f || dy > 0.1f || isTransitioning) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                isAnimating = false
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK) // Clear

                    // 1. Draw old bitmap if transitioning
                    val oldBmp = transitionOldBitmap
                    if (isTransitioning && oldBmp != null && !oldBmp.isRecycled) {
                        drawMatrix.setScale(baseScale, baseScale)
                        drawMatrix.postTranslate(baseCenterX + currentOffsetX, baseCenterY + currentOffsetY)
                        canvas.drawBitmap(oldBmp, drawMatrix, paint)
                    }

                    val bmp = backgroundBitmap
                    if (bmp != null && !bmp.isRecycled) {
                        drawMatrix.setScale(baseScale, baseScale)
                        drawMatrix.postTranslate(baseCenterX + currentOffsetX, baseCenterY + currentOffsetY)
                        
                        if (isTransitioning) {
                            canvas.save()
                            transitionPath.reset()
                            // Max radius is distance from center to corner
                            val maxRadius = Math.hypot((canvasWidth / 2.0), (canvasHeight / 2.0)).toFloat()
                            val currentRadius = maxRadius * transitionProgress
                            // Hyprland-style easing (Out Quart)
                            val easedProgress = 1f - (1f - transitionProgress) * (1f - transitionProgress) * (1f - transitionProgress) * (1f - transitionProgress)
                            val easedRadius = maxRadius * easedProgress
                            
                            transitionPath.addCircle(canvasWidth / 2f, canvasHeight / 2f, easedRadius, Path.Direction.CW)
                            canvas.clipPath(transitionPath)
                            canvas.drawBitmap(bmp, drawMatrix, paint)
                            canvas.restore()
                        } else {
                            canvas.drawBitmap(bmp, drawMatrix, paint)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DaemonParallax", "Drawing error", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("DaemonParallax", "Unlock error", e)
                    }
                }
            }
        }

        private fun loadBitmapSafe(uriString: String?) {
            if (uriString.isNullOrEmpty()) return
            engineScope.launch {
                try {
                    val uri = Uri.parse(uriString)
                    val newBitmap = withContext(Dispatchers.IO) {
                        val metrics = resources.displayMetrics
                        decodeSampledBitmapFromUri(uri, metrics.widthPixels, metrics.heightPixels)
                    }
                    if (newBitmap != null) {
                        if (backgroundBitmap != null) {
                            // Start transition
                            transitionOldBitmap?.recycle()
                            transitionOldBitmap = backgroundBitmap
                            isTransitioning = true
                            transitionProgress = 0f
                            startAnimation()
                        }
                        backgroundBitmap = newBitmap
                        recalculateBaseTransforms()
                        drawFrame()
                    }
                } catch (e: Exception) {
                    Log.e("DaemonParallax", "Bitmap load failed", e)
                }
            }
        }

        private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
            val cr = baseContext.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            return cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
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

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "parallax_uri") {
                loadBitmapSafe(sharedPreferences?.getString(key, null))
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            engineJob.cancel()
            sensorManager?.unregisterListener(this)
            backgroundBitmap?.recycle()
            backgroundBitmap = null
            getSharedPreferences("DaemonWallpaperPrefs", Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this)
        }
    }
}