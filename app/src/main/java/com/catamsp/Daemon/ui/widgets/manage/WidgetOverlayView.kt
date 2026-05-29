package com.catamsp.Daemon.ui.widgets.manage

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.graphics.toRectF
import com.catamsp.Daemon.R
import com.catamsp.Daemon.widgets.Widget
import com.catamsp.Daemon.widgets.updateWidget


private const val HANDLE_SIZE = 100
private const val HANDLE_EDGE_SIZE = (1.2 * HANDLE_SIZE).toInt()

/**
 * An overlay to show configuration options for a widget in [WidgetManagerView]
 */
class WidgetOverlayView : ViewGroup {

    private val paint = Paint()
    private val handlePaint = Paint()
    private val selectedHandlePaint = Paint()

    private val popupAnchor = View(context)

    var mode: WidgetManagerView.EditMode? = null

    class Handle(val mode: WidgetManagerView.EditMode, val position: Rect)

    init {
        addView(popupAnchor)
        setWillNotDraw(false)
        handlePaint.style = Paint.Style.FILL
        handlePaint.color = Color.WHITE
        handlePaint.alpha = 200
        handlePaint.setShadowLayer(8f, 0f, 4f, Color.argb(100, 0, 0, 0))

        selectedHandlePaint.style = Paint.Style.FILL
        selectedHandlePaint.color = Color.parseColor("#4285F4")
        selectedHandlePaint.setShadowLayer(8f, 0f, 4f, Color.argb(100, 0, 0, 0))

        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    private var preview: Drawable? = null
    var widgetId: Int = -1
        set(newId) {
            field = newId
            preview = Widget.byId(widgetId, context)?.getPreview(context)
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = 20f
        getHandles().forEach {
            val handleRect = it.position.toRectF()
            val insetX = handleRect.width() * 0.3f
            val insetY = handleRect.height() * 0.3f
            val visualRect = RectF(
                handleRect.left + insetX,
                handleRect.top + insetY,
                handleRect.right - insetX,
                handleRect.bottom - insetY
            )

            if (it.mode == mode) {
                canvas.drawRoundRect(visualRect, cornerRadius, cornerRadius, selectedHandlePaint)
            } else {
                canvas.drawRoundRect(visualRect, cornerRadius, cornerRadius, handlePaint)
            }
        }

        if (mode == null) {
            return
        }
        //preview?.bounds = bounds
        //preview?.draw(canvas)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        popupAnchor.layout(0, 0, 0, 0)
    }

    fun showPopupMenu() {
        val widget = Widget.byId(widgetId, context) ?: return
        val menu = PopupMenu(context, popupAnchor)
        menu.menu.let {
            it.add(
                context.getString(R.string.widget_menu_remove)
            ).setOnMenuItemClickListener { _ ->
                Widget.byId(widgetId, context)?.delete(context)
                return@setOnMenuItemClickListener true
            }
            it.add(
                if (widget.allowInteraction) {
                    context.getString(R.string.widget_menu_disable_interaction)
                } else {
                    context.getString(R.string.widget_menu_enable_interaction)
                }
            ).setOnMenuItemClickListener { _ ->
                widget.allowInteraction = !widget.allowInteraction
                updateWidget(widget)
                return@setOnMenuItemClickListener true
            }

            val alignMenu = it.addSubMenu("Align Widget")
            val alignments = mapOf(
                "Fill" to android.view.Gravity.FILL,
                "Center" to android.view.Gravity.CENTER,
                "Top Left" to (android.view.Gravity.TOP or android.view.Gravity.LEFT),
                "Top Center" to (android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL),
                "Top Right" to (android.view.Gravity.TOP or android.view.Gravity.RIGHT),
                "Center Left" to (android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.LEFT),
                "Center Right" to (android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.RIGHT),
                "Bottom Left" to (android.view.Gravity.BOTTOM or android.view.Gravity.LEFT),
                "Bottom Center" to (android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL),
                "Bottom Right" to (android.view.Gravity.BOTTOM or android.view.Gravity.RIGHT)
            )

            alignments.forEach { (label, gravity) ->
                alignMenu.add(label).setOnMenuItemClickListener {
                    widget.alignment = gravity
                    updateWidget(widget)
                    (parent as? View)?.requestLayout()
                    return@setOnMenuItemClickListener true
                }
            }
        }
        (context as? com.catamsp.Daemon.ui.UIObject)?.applyFontToMenu(context, menu.menu)
        menu.show()
    }

    fun getHandles(): List<Handle> {
        val THICKNESS = 60
        val dynamicEdgeSize = if (width < 300 || height < 300) 40 else 100
        
        return listOf(
            Handle(
                WidgetManagerView.EditMode.TOP,
                Rect(dynamicEdgeSize, -THICKNESS, width - dynamicEdgeSize, 0)
            ),
            Handle(
                WidgetManagerView.EditMode.BOTTOM,
                Rect(dynamicEdgeSize, height, width - dynamicEdgeSize, height + THICKNESS)
            ),
            Handle(
                WidgetManagerView.EditMode.LEFT,
                Rect(-THICKNESS, dynamicEdgeSize, 0, height - dynamicEdgeSize)
            ),
            Handle(
                WidgetManagerView.EditMode.RIGHT,
                Rect(width, dynamicEdgeSize, width + THICKNESS, height - dynamicEdgeSize)
            )
        )

    }

    private fun getBounds(): Rect {
        return Rect(0, 0, width, height)
    }
}