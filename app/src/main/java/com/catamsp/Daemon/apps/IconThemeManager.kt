package com.catamsp.Daemon.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.IconTheme
import kotlin.math.cos
import kotlin.math.sin

object IconThemeManager {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun transform(context: Context, icon: Drawable): Drawable {
        val theme = try {
            LauncherPreferences.icons().iconTheme()
        } catch (_: Exception) {
            IconTheme.NONE
        }
        if (theme == IconTheme.NONE) return icon

        val iconBitmap = drawableToBitmap(icon)
        val dominantColor = try {
            Palette.from(iconBitmap).generate().getDominantColor(0xFF424242.toInt())
        } catch (_: Exception) {
            0xFF424242.toInt()
        }

        val size = iconBitmap.width.coerceAtLeast(iconBitmap.height).coerceAtLeast(1)

        // Create output bitmap
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Draw shape background at full size
        bgPaint.color = dominantColor
        val bgPath = createShapePath(theme, size)
        canvas.drawPath(bgPath, bgPaint)

        // 2. Draw original icon centered at 65% size
        val iconSize = (size * 0.65f).toInt().coerceAtLeast(1)
        val left = (size - iconSize) / 2
        val top = (size - iconSize) / 2
        icon.setBounds(left, top, left + iconSize, top + iconSize)
        icon.draw(canvas)

        return BitmapDrawable(context.resources, result)
    }

    private fun createShapePath(theme: IconTheme, size: Int): Path {
        val path = Path()
        val w = size.toFloat()
        val h = size.toFloat()

        when (theme) {
            IconTheme.CIRCLE -> {
                path.addCircle(w / 2f, h / 2f, w / 2f, Path.Direction.CW)
            }
            IconTheme.SQUIRCLE -> {
                // iOS-style superellipse approximation using cubic bezier curves
                val cx = w / 2f
                val cy = h / 2f
                val rx = w / 2f
                val ry = h / 2f
                val k = 0.5522847498f
                val ox = rx * k
                val oy = ry * k

                path.moveTo(cx, 0f)
                path.cubicTo(cx + ox, 0f, w, cy - oy, w, cy)
                path.cubicTo(w, cy + oy, cx + ox, h, cx, h)
                path.cubicTo(cx - ox, h, 0f, cy + oy, 0f, cy)
                path.cubicTo(0f, cy - oy, cx - ox, 0f, cx, 0f)
                path.close()
            }
            IconTheme.ROUNDED_SQUARE -> {
                val r = w * 0.22f
                path.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)
            }
            IconTheme.DIAMOND -> {
                path.moveTo(w / 2f, 0f)
                path.lineTo(w, h / 2f)
                path.lineTo(w / 2f, h)
                path.lineTo(0f, h / 2f)
                path.close()
            }
            IconTheme.TEARDROP -> {
                // Circle in top-left + pointed tip to bottom-right
                val cx = w * 0.42f
                val cy = h * 0.42f
                val r = w * 0.42f
                path.addCircle(cx, cy, r, Path.Direction.CW)
                // Triangle tip
                path.moveTo(cx + r * 0.7f, cy + r * 0.7f)
                path.lineTo(w * 0.95f, h * 0.95f)
                path.lineTo(cx + r * 0.7f, cy + r * 0.2f)
                path.close()
            }
            IconTheme.HEXAGON -> {
                val cx = w / 2f
                val cy = h / 2f
                val radius = w / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 30.0)
                    val x = cx + (radius * cos(angle)).toFloat()
                    val y = cy + (radius * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            IconTheme.NONE -> {
                // Should never reach here due to early return
                path.addRect(0f, 0f, w, h, Path.Direction.CW)
            }
        }

        return path
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
