package com.catamsp.Daemon.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var currentHue = 0f
    private var currentSaturation = 1f
    private var currentValue = 1f

    var onColorChangeListener: ((hue: Float, saturation: Float) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = (min(w, h) / 2f) - 20f

        val colors = intArrayOf(
            Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, 
            Color.GREEN, Color.YELLOW, Color.RED
        )
        val sweepGradient = SweepGradient(centerX, centerY, colors, null)
        colorPaint.shader = sweepGradient
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the color wheel
        canvas.drawCircle(centerX, centerY, radius, colorPaint)

        // Draw the indicator ring
        val angle = Math.toRadians(currentHue.toDouble())
        val x = centerX + radius * currentSaturation * cos(angle).toFloat()
        val y = centerY + radius * currentSaturation * sin(angle).toFloat()
        canvas.drawCircle(x, y, 20f, indicatorPaint)
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSaturation = hsv[1]
        currentValue = hsv[2]
        invalidate()
    }

    fun setValue(value: Float) {
        currentValue = value
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        val x = event.x - centerX
        val y = event.y - centerY
        val d = sqrt(x * x + y * y)

        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // Calculate Hue (0-360)
            currentHue = (Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat() + 360f) % 360f
            
            // Calculate Saturation (0-1) based on distance from center
            currentSaturation = (d / radius).coerceIn(0f, 1f)

            onColorChangeListener?.invoke(currentHue, currentSaturation)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
