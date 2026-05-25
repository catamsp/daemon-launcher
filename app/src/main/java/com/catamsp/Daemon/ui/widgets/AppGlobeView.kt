package com.catamsp.Daemon.ui.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.apps.AbstractDetailedAppInfo
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.PinnedShortcutInfo
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.widgets.WidgetPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class AppGlobeView(
    context: Context,
    attrs: AttributeSet? = null,
    val appWidgetId: Int,
    val panelId: Int
) : View(context, attrs) {

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        WidgetPanel.HOME.id,
        -1
    )

    private var apps: List<AbstractDetailedAppInfo> = emptyList()
    private var points: MutableList<Point3D> = mutableListOf()
    private var iconBitmaps: MutableMap<String, Bitmap> = mutableMapOf()
    private var iconColors: MutableMap<String, Int> = mutableMapOf()
    private var notifiedPackages: Set<String> = emptySet()
    private var relevantNotifiedPackages: Set<String> = emptySet()

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val grayscaleFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cam = android.graphics.Camera()
    private val mMatrix = Matrix()

    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private var isDragging = false
    private var isViewVisible = true
    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val sphereRadius = 0.8f 
    private var baseIconSizePx = 0f
    private var loadIconsJob: kotlinx.coroutines.Job? = null

    data class Point3D(var x: Float, var y: Float, var z: Float, val app: AbstractDetailedAppInfo)

    private fun updateRelevantNotifications() {
        val currentAppPackages = apps.mapNotNull { 
            val info = it.getRawInfo()
            when (info) {
                is AppInfo -> info.packageName
                is PinnedShortcutInfo -> info.packageName
                else -> null
            }
        }.toSet()
        relevantNotifiedPackages = notifiedPackages.intersect(currentAppPackages)
    }

    private val appsObserver = Observer<List<AbstractDetailedAppInfo>> {
        apps = it
        updateRelevantNotifications()
        
        // Step 1: Generate geometry immediately on the Main Thread
        generatePoints()
        invalidate() // Draw fallbacks instantly

        // Step 2: Lazy load icons in the background progressively
        loadIconsJob?.cancel()
        loadIconsJob = CoroutineScope(Dispatchers.Default).launch {
            val size = (40f * resources.displayMetrics.density).toInt()
            
            for (app in it) {
                if (!isActive) break
                val key = app.getRawInfo().toString()
                
                // Skip if already loaded
                if (iconBitmaps.containsKey(key)) continue
                
                try {
                    val drawable = app.getIcon(context)
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)
                    
                    // Extract dominant color for the holographic glow
                    val palette = Palette.from(bitmap).generate()
                    val dominantColor = palette.getDominantColor(0xFF00FFFF.toInt())
                    
                    withContext(Dispatchers.Main) {
                        iconBitmaps[key] = bitmap
                        iconColors[key] = dominantColor
                        // Redraw as each icon arrives for a "pop-in" effect
                        invalidate()
                    }
                } catch (e: Exception) {
                    Log.e("AppGlobeView", "Lazy load failed for $key", e)
                }
            }
        }
    }

    private val notificationsObserver = Observer<Set<String>> {
        notifiedPackages = it
        updateRelevantNotifications()
        invalidate()
    }

    init {
        baseIconSizePx = 40f * resources.displayMetrics.density
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val app = context.applicationContext as? Application
        app?.apps?.observeForever(appsObserver)
        app?.activeNotifications?.observeForever(notificationsObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val app = context.applicationContext as? Application
        app?.apps?.removeObserver(appsObserver)
        app?.activeNotifications?.removeObserver(notificationsObserver)
        loadIconsJob?.cancel()
        iconBitmaps.values.forEach { it.recycle() }
        iconBitmaps.clear()
        iconColors.clear()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        isViewVisible = visibility == VISIBLE
        if (!isViewVisible) {
            velocityX = 0f
            velocityY = 0f
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isViewVisible = visibility == VISIBLE
        if (!isViewVisible) {
            velocityX = 0f
            velocityY = 0f
        }
    }

    private fun generatePoints() {
        points.clear()
        if (apps.isEmpty()) return

        val n = apps.size
        // Tweak: Further lower the scale factor (from 9f to 7.5f) to ensure no overlaps
        val scaleFactor = 7.5f / max(3.16f, sqrt(n.toFloat())) 
        baseIconSizePx = (40f * resources.displayMetrics.density * scaleFactor).coerceIn(
            16f * resources.displayMetrics.density, 
            64f * resources.displayMetrics.density
        )

        val goldenRatio = (1 + sqrt(5f)) / 2
        val angleIncrement = 2 * PI * goldenRatio

        for (i in 0 until n) {
            // Use (i + 0.5) for more uniform distribution at the poles
            val t = (i + 0.5f) / n
            val phi = acos(1 - 2 * t)
            val theta = angleIncrement * i
            points.add(Point3D(
                (sin(phi) * cos(theta)).toFloat(),
                (sin(phi) * sin(theta)).toFloat(),
                cos(phi).toFloat(),
                apps[i]
            ))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                velocityX = 0f
                velocityY = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                velocityX = dx
                velocityY = dy
                rotateY(-dx * 0.005f)
                rotateX(dy * 0.005f)
                lastX = event.x
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                // Distinguish between a drag/spin and a tap
                val totalDist = hypot(event.x - downX, event.y - downY)
                if (totalDist < touchSlop) {
                    findClickedApp(event.x, event.y)
                }
            }
        }
        return true
    }

    private fun rotateY(rad: Float) {
        val cos = cos(rad)
        val sin = sin(rad)
        for (p in points) {
            val nx = p.x * cos - p.z * sin
            val nz = p.x * sin + p.z * cos
            p.x = nx
            p.z = nz
        }
    }

    private fun rotateX(rad: Float) {
        val cos = cos(rad)
        val sin = sin(rad)
        for (p in points) {
            val ny = p.y * cos + p.z * sin
            val nz = -p.y * sin + p.z * cos
            p.y = ny
            p.z = nz
        }
    }

    private fun findClickedApp(tx: Float, ty: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * sphereRadius
        val usePerspective = LauncherPreferences.globe().perspective()
        val cameraDistance = 4.8f

        var closestApp: AbstractDetailedAppInfo? = null
        var maxZ = -2f

        for (p in points) {
            if (p.z < 0) continue 
            
            // Match the projection from onDraw
            val x2d: Float
            val y2d: Float
            if (usePerspective) {
                val perspective = cameraDistance / (cameraDistance - p.z)
                x2d = centerX + (p.x * radius * perspective)
                y2d = centerY + (p.y * radius * perspective)
            } else {
                x2d = centerX + (p.x * radius)
                y2d = centerY + (p.y * radius)
            }
            
            val scale = (p.z + 1) / 2f * 0.8f + 0.2f
            val currentSize = baseIconSizePx * scale
            val rect = RectF(x2d - currentSize / 2, y2d - currentSize / 2, x2d + currentSize / 2, y2d + currentSize / 2)
            if (rect.contains(tx, ty) && p.z > maxZ) {
                maxZ = p.z
                closestApp = p.app
            }
        }
        closestApp?.let { it.getAction().invoke(context as Activity) }
    }

    private val drawRect = RectF()
    private val glowRect = RectF()

    override fun onDraw(canvas: Canvas) {
        if (!isViewVisible) return
        super.onDraw(canvas)

        if (!isDragging && (abs(velocityX) > 0.1f || abs(velocityY) > 0.1f)) {
            rotateY(-velocityX * 0.005f)
            rotateX(velocityY * 0.005f)
            velocityX *= 0.92f // Increased friction for faster stop
            velocityY *= 0.92f
            invalidate()
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * sphereRadius
        
        // Globe Customization Preferences
        val usePerspective = LauncherPreferences.globe().perspective()
        val showGlow = LauncherPreferences.globe().showGlow()
        val glowOpacity = LauncherPreferences.globe().glowOpacity()
        val cameraDistance = 4.8f 

        // Performance: Back-to-front drawing
        val sortedPoints = points.sortedBy { it.z }
        
        val time = System.currentTimeMillis()
        val hasAnyNotification = relevantNotifiedPackages.isNotEmpty()
        
        val forcePulse = false 

        for (p in sortedPoints) {
            if (p.z < -0.85f) continue 

            // Coordinate Projection
            val x2d: Float
            val y2d: Float
            if (usePerspective) {
                val perspective = cameraDistance / (cameraDistance - p.z)
                x2d = centerX + (p.x * radius * perspective)
                y2d = centerY + (p.y * radius * perspective)
            } else {
                x2d = centerX + (p.x * radius)
                y2d = centerY + (p.y * radius)
            }
            
            val scale = (p.z + 1) / 2f * 0.8f + 0.2f
            var alpha = ((p.z + 1) / 2f * 200 + 55).toInt()
            val currentSize = baseIconSizePx * scale
            
            val info = p.app.getRawInfo()
            val appKey = info.toString()
            // 1. Correct Package Name Extraction
            val packageName = when (val raw = p.app.getRawInfo()) {
                is AppInfo -> raw.packageName
                is PinnedShortcutInfo -> raw.packageName
                else -> ""
            }
            
            // 2. Notification Detection
            val hasNotification = showGlow && ((packageName.isNotEmpty() && relevantNotifiedPackages.contains(packageName)) || (forcePulse && p == sortedPoints.last()))
            val bitmap = iconBitmaps[appKey]
            
            // Reset pulse paint for each loop to prevent alpha leaking
            pulsePaint.shader = null
            pulsePaint.alpha = 255

            if (bitmap == null) {
                // Fallback: Draw a stylish placeholder dot while the icon loads
                val baseColor = iconColors[appKey] ?: 0x44FFFFFF.toInt()
                pulsePaint.color = baseColor
                
                if (hasNotification) {
                    // 3D-Aware Aura for Placeholder Dots
                    val glowRadius = (currentSize / 4f) * 2.2f 
                    val iconColor = (iconColors[appKey] ?: 0xFF00FFFF.toInt()) or 0xFF000000.toInt()
                    val colors = intArrayOf(iconColor, (iconColor and 0x00FFFFFF.toInt()) or (glowOpacity shl 24), 0)
                    val stops = floatArrayOf(0f, 0.4f, 1f)
                    
                    pulsePaint.shader = RadialGradient(x2d, y2d, glowRadius, colors, stops, Shader.TileMode.CLAMP)
                    canvas.drawCircle(x2d, y2d, glowRadius, pulsePaint)
                    pulsePaint.shader = null
                    
                    pulsePaint.alpha = 255
                    pulsePaint.color = iconColor
                    canvas.drawCircle(x2d, y2d, currentSize / 4f, pulsePaint)
                } else {
                    pulsePaint.alpha = (alpha * 0.8f).toInt()
                    canvas.drawCircle(x2d, y2d, currentSize / 8f, pulsePaint)
                }
            } else {
                // Dynamic Focus: Dim non-notified apps (only if glow is enabled)
                if (showGlow && hasAnyNotification && !hasNotification) {
                    iconPaint.colorFilter = grayscaleFilter
                    alpha = (alpha * 0.5f).toInt()
                } else {
                    iconPaint.colorFilter = null
                }
                
                iconPaint.alpha = alpha

                // Apply 3D Rotation Matrix for tangent foreshortening
                cam.save()
                val rotX = Math.toDegrees(asin(p.y.toDouble())).toFloat()
                val rotY = Math.toDegrees(asin(-p.x.toDouble())).toFloat()
                cam.rotateX(rotX)
                cam.rotateY(rotY)
                cam.getMatrix(mMatrix)
                cam.restore()

                mMatrix.preTranslate(-currentSize / 2, -currentSize / 2)
                mMatrix.postTranslate(x2d, y2d)

                canvas.save()
                canvas.concat(mMatrix)

                // 3. Draw holographic glow INSIDE the matrix (Visible Aura Fix)
                if (hasNotification && p.z > -0.6f) {
                    val visibilityFactor = ((p.z + 0.6f) / 1.6f).coerceIn(0f, 1f)
                    val iconColor = (iconColors[appKey] ?: 0xFF00FFFF.toInt()) or 0xFF000000.toInt() // Force opaque
                    
                    val glowRadius = currentSize * 1.3f // Medium expansion
                    val baseGlowAlpha = (glowOpacity * visibilityFactor).toInt()
                    val outerGlowAlpha = (glowOpacity * 0.3f * visibilityFactor).toInt()
                    
                    val colors = intArrayOf(
                        iconColor, // Opaque center
                        (iconColor and 0x00FFFFFF.toInt()) or (baseGlowAlpha shl 24), // Edge
                        (iconColor and 0x00FFFFFF.toInt()) or (outerGlowAlpha shl 24),  // Outer bleed
                        0 // Fade out
                    )
                    val stops = floatArrayOf(0f, 0.45f, 0.7f, 1f) 
                    
                    pulsePaint.shader = RadialGradient(currentSize/2, currentSize/2, glowRadius, colors, stops, Shader.TileMode.CLAMP)
                    canvas.drawCircle(currentSize/2, currentSize/2, glowRadius, pulsePaint)
                    pulsePaint.shader = null
                }

                drawRect.set(0f, 0f, currentSize, currentSize)
                canvas.drawBitmap(bitmap, null, drawRect, iconPaint)
                canvas.restore()
            }
        }
    }

    // Removed getAppIconBitmap as everything is pre-cached now
}