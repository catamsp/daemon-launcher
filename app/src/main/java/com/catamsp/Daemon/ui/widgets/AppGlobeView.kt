package com.catamsp.Daemon.ui.widgets

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import com.catamsp.Daemon.Application
import com.catamsp.Daemon.apps.AbstractDetailedAppInfo
import com.catamsp.Daemon.widgets.WidgetPanel
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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var lastX = 0f
    private var lastY = 0f
    private var velocityX = 0f
    private var velocityY = 0f
    private var isDragging = false

    private val sphereRadius = 0.8f // Relative to view size
    private var baseIconSizePx = 0f

    data class Point3D(var x: Float, var y: Float, var z: Float, val app: AbstractDetailedAppInfo)

    private val appsObserver = Observer<List<AbstractDetailedAppInfo>> {
        apps = it
        iconBitmaps.clear() // Clear cache when app list changes
        generatePoints()
        invalidate()
    }

    init {
        // Use a more conservative default, will be recalculated in generatePoints
        baseIconSizePx = 48f * resources.displayMetrics.density
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (context.applicationContext as? Application)?.apps?.observeForever(appsObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (context.applicationContext as? Application)?.apps?.removeObserver(appsObserver)
        // Clean up bitmaps
        iconBitmaps.values.forEach { it.recycle() }
        iconBitmaps.clear()
    }

    private fun generatePoints() {
        points.clear()
        if (apps.isEmpty()) return

        val n = apps.size
        
        // DYNAMIC SIZING: Scale base size by sqrt of count
        // For 100 apps, this makes them roughly 1/3 the size of a single app
        val scaleFactor = 10f / max(3.16f, sqrt(n.toFloat())) 
        baseIconSizePx = (48f * resources.displayMetrics.density * scaleFactor).coerceIn(
            16f * resources.displayMetrics.density, 
            64f * resources.displayMetrics.density
        )

        val goldenRatio = (1 + sqrt(5f)) / 2
        val angleIncrement = 2 * PI * goldenRatio

        for (i in 0 until n) {
            val t = i.toFloat() / n
            val phi = acos(1 - 2 * t)
            val theta = angleIncrement * i

            val x = sin(phi) * cos(theta)
            val y = sin(phi) * sin(theta)
            val z = cos(phi)

            points.add(Point3D(x.toFloat(), y.toFloat(), z.toFloat(), apps[i]))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
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
                // Check for click if movement was minimal
                if (abs(velocityX) < 2 && abs(velocityY) < 2) {
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

        var closestApp: AbstractDetailedAppInfo? = null
        var maxZ = -2f

        for (p in points) {
            if (p.z < 0) continue // Only front side

            val x2d = centerX + p.x * radius
            val y2d = centerY + p.y * radius
            val scale = (p.z + 1) / 2f + 0.3f
            val currentSize = baseIconSizePx * scale

            val rect = RectF(x2d - currentSize / 2, y2d - currentSize / 2, x2d + currentSize / 2, y2d + currentSize / 2)
            if (rect.contains(tx, ty)) {
                if (p.z > maxZ) {
                    maxZ = p.z
                    closestApp = p.app
                }
            }
        }

        closestApp?.let {
            it.getAction().invoke(context as Activity)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isDragging && (abs(velocityX) > 0.1f || abs(velocityY) > 0.1f)) {
            // Apply Momentum
            rotateY(-velocityX * 0.005f)
            rotateX(velocityY * 0.005f)
            velocityX *= 0.95f
            velocityY *= 0.95f
            invalidate()
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f * sphereRadius

        // Sort points by Z to draw back to front (O(n log n))
        val sortedPoints = points.sortedBy { it.z }

        for (p in sortedPoints) {
            // CULLING: Don't draw apps hidden too far in the back to save battery
            if (p.z < -0.85f) continue 

            val x2d = centerX + p.x * radius
            val y2d = centerY + p.y * radius

            // Project 3D to 2D
            // Scale and Alpha based on Z
            val scale = (p.z + 1) / 2f * 0.7f + 0.3f // Range 0.3 to 1.0
            val alpha = ((p.z + 1) / 2f * 200 + 55).toInt() // Range 55 to 255

            val currentSize = baseIconSizePx * scale
            
            val bitmap = getAppIconBitmap(p.app)
            val rect = RectF(
                x2d - currentSize / 2,
                y2d - currentSize / 2,
                x2d + currentSize / 2,
                y2d + currentSize / 2
            )
            
            iconPaint.alpha = alpha
            canvas.drawBitmap(bitmap, null, rect, iconPaint)
        }
    }

    private fun getAppIconBitmap(app: AbstractDetailedAppInfo): Bitmap {
        val key = app.getRawInfo().toString()
        return iconBitmaps.getOrPut(key) {
            val drawable = app.getIcon(context)
            // Render to bitmap at a standard size for optimization
            val size = (48f * resources.displayMetrics.density).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        }
    }
}