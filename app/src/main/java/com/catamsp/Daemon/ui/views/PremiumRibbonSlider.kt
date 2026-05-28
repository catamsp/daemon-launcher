package com.catamsp.Daemon.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class PremiumRibbonSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var minValue = 20
    private var maxValue = 60
    private var currentValue = 20

    // The track background (thick, semi-transparent)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1AFFFFFF") // Subtle transparent white/grey
    }
    
    // The active fill
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0") // Solid premium accent color
    }
    
    private val rect = RectF()
    private val fillRect = RectF()
    
    // Subtle table-top corners
    private val cornerRadius = 12f 

    var onValueChangeListener: ((Int) -> Unit)? = null
    var onStopTrackingListener: ((Int) -> Unit)? = null

    var progress: Int
        get() = currentValue
        set(value) {
            currentValue = value.coerceIn(minValue, maxValue)
            invalidate()
        }

    fun setValues(min: Int, max: Int, current: Int) {
        minValue = min
        maxValue = max
        currentValue = current.coerceIn(min, max)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw empty track
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, trackPaint)

        // Draw solid fill tracking current value
        val fillRatio = if (maxValue > minValue) (currentValue - minValue).toFloat() / (maxValue - minValue) else 0f
        fillRect.set(0f, 0f, width * fillRatio, height.toFloat())
        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Disallow ViewPager from stealing the horizontal swipe
        parent.requestDisallowInterceptTouchEvent(true)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val ratio = (event.x / width).coerceIn(0f, 1f)
                currentValue = minValue + (ratio * (maxValue - minValue)).toInt()
                onValueChangeListener?.invoke(currentValue)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onStopTrackingListener?.invoke(currentValue)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
