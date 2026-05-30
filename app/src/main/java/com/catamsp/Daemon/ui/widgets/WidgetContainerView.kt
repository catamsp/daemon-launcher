package com.catamsp.Daemon.ui.widgets

import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import androidx.core.graphics.contains
import androidx.core.view.size
import com.catamsp.Daemon.widgets.Widget
import com.catamsp.Daemon.widgets.WidgetPanel
import com.catamsp.Daemon.widgets.WidgetPosition
import kotlin.math.max


/**
 * This only works in an Activity, not AppCompatActivity
 */
open class WidgetContainerView(
    var widgetPanelId: Int,
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {
    constructor(context: Context, attrs: AttributeSet) : this(WidgetPanel.HOME.id, context, attrs)

    var widgetViewById = HashMap<Int, View>()
    protected var gridAreaWidth: Int = 0
    protected var gridAreaHeight: Int = 0

    open fun updateWidgets(activity: Activity, widgets: Collection<Widget>?) {
        synchronized(widgetViewById) {
            if (widgets == null) {
                return
            }
            Log.i("WidgetContainer", "updating ${activity.localClassName}")
            widgetViewById.forEach { removeView(it.value) }
            widgetViewById.clear()
            widgets.filter { it.panelId == widgetPanelId }.sortedBy { it.zIndex }.forEach { widget ->
                widget.createView(activity)?.let {
                    val lp = LayoutParams(widget.position)
                    lp.alignment = widget.alignment
                    addView(it, lp)
                    widgetViewById[widget.id] = it
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return false
        }
        val position = PointF(ev.x, ev.y)

        val intercepted = synchronized(widgetViewById) {
            widgetViewById.filter {
                RectF(
                    it.value.x,
                    it.value.y,
                    it.value.x + it.value.width,
                    it.value.y + it.value.height
                ).contains(position) == true
            }.any {
                Widget.byId(it.key, context)?.allowInteraction == false
            }
        }
        return intercepted
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        var maxHeight = suggestedMinimumHeight
        var maxWidth = suggestedMinimumWidth

        var mWidth = MeasureSpec.getSize(widthMeasureSpec)
        var mHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Fallback to absolute display metrics if inside a ScrollView (UNSPECIFIED bounds)
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED || mWidth == 0) {
            mWidth = context.resources.displayMetrics.widthPixels
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED || mHeight == 0) {
            mHeight = context.resources.displayMetrics.heightPixels
        }

        gridAreaWidth = mWidth
        gridAreaHeight = mHeight

        (0..<size).map { getChildAt(it) }.forEach {
            val lp = it.layoutParams as LayoutParams
            val position = lp.position.getAbsoluteRect(gridAreaWidth, gridAreaHeight)
            
            if (lp.alignment == Gravity.FILL) {
                it.measure(
                    makeMeasureSpec(position.width(), MeasureSpec.EXACTLY),
                    makeMeasureSpec(position.height(), MeasureSpec.EXACTLY)
                )
            } else {
                it.measure(
                    makeMeasureSpec(position.width(), MeasureSpec.AT_MOST),
                    makeMeasureSpec(position.height(), MeasureSpec.AT_MOST)
                )
            }
        }

        // Find rightmost and bottom-most child
        (0..<size).map { getChildAt(it) }.filter { it.visibility != GONE }.forEach {
            val position =
                (it.layoutParams as LayoutParams).position.getAbsoluteRect(gridAreaWidth, gridAreaHeight)
            maxWidth = max(maxWidth, position.left + it.measuredWidth)
            maxHeight = max(maxHeight, position.top + it.measuredHeight)
        }

        setMeasuredDimension(
            resolveSizeAndState(maxWidth.toInt(), widthMeasureSpec, 0),
            resolveSizeAndState(maxHeight.toInt(), heightMeasureSpec, 0)
        )
    }

    /**
     * Returns a set of layout parameters with a width of
     * [ViewGroup.LayoutParams.WRAP_CONTENT],
     * a height of [ViewGroup.LayoutParams.WRAP_CONTENT]
     * and with the coordinates (0, 0).
     */
    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(WidgetPosition(0, 0, 1, 1))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0..<size) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            // FIXED: Anchor layout rendering to the stable screen-derived grid area
            val position = lp.position.getAbsoluteRect(gridAreaWidth, gridAreaHeight)
            
            if (lp.alignment == Gravity.FILL) {
                child.layout(position.left, position.top, position.right, position.bottom)
            } else {
                val outRect = Rect()
                Gravity.apply(lp.alignment, child.measuredWidth, child.measuredHeight, position, outRect)
                child.layout(outRect.left, outRect.top, outRect.right, outRect.bottom)
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    // Override to allow type-checking of LayoutParams.
    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    companion object {
        class LayoutParams : ViewGroup.LayoutParams {
            var position = WidgetPosition(0, 0, 4, 4)
            var alignment = Gravity.FILL


            constructor(position: WidgetPosition) : super(WRAP_CONTENT, WRAP_CONTENT) {
                this.position = position
            }

            constructor(c: Context, attrs: AttributeSet?) : super(c, attrs)
            constructor(source: ViewGroup.LayoutParams?) : super(source)

        }
    }
}
