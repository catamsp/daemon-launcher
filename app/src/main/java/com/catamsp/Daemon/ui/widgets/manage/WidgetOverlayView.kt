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
import com.catamsp.Daemon.preferences.LauncherPreferences
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
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE
        paint.alpha = 200
        paint.setShadowLayer(8f, 0f, 4f, Color.argb(100, 0, 0, 0))

        selectedHandlePaint.style = Paint.Style.STROKE
        selectedHandlePaint.strokeWidth = 6f
        selectedHandlePaint.color = Color.parseColor("#4285F4")
        selectedHandlePaint.setShadowLayer(10f, 0f, 4f, Color.argb(120, 0, 0, 0))
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

        val bounds = getBounds().toRectF()
        val cornerRadius = 30f

        if (mode != null) {
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, selectedHandlePaint)
        } else {
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint)
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

            val layerMenu = it.addSubMenu("Layering")
            layerMenu.add("Bring to Front").setOnMenuItemClickListener { _ ->
                val allWidgets = LauncherPreferences.widgets().widgets() ?: emptySet()
                val maxZ = allWidgets.filter { w -> w.panelId == widget.panelId }.maxOfOrNull { w -> w.zIndex } ?: 0
                widget.zIndex = maxZ + 1
                updateWidget(widget)
                (parent as? View)?.requestLayout()
                return@setOnMenuItemClickListener true
            }
            layerMenu.add("Send to Back").setOnMenuItemClickListener { _ ->
                val allWidgets = LauncherPreferences.widgets().widgets() ?: emptySet()
                val minZ = allWidgets.filter { w -> w.panelId == widget.panelId }.minOfOrNull { w -> w.zIndex } ?: 0
                widget.zIndex = minZ - 1
                updateWidget(widget)
                (parent as? View)?.requestLayout()
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
        val CORNER_SIZE = 120
        val dynamicEdgeSize = if (width < 300 || height < 300) 80 else 150
        
        return listOf(
            // Corners first for priority
            Handle(
                WidgetManagerView.EditMode.TOP_LEFT,
                Rect(-CORNER_SIZE, -CORNER_SIZE, dynamicEdgeSize, dynamicEdgeSize)
            ),
            Handle(
                WidgetManagerView.EditMode.TOP_RIGHT,
                Rect(width - dynamicEdgeSize, -CORNER_SIZE, width + CORNER_SIZE, dynamicEdgeSize)
            ),
            Handle(
                WidgetManagerView.EditMode.BOTTOM_LEFT,
                Rect(-CORNER_SIZE, height - dynamicEdgeSize, dynamicEdgeSize, height + CORNER_SIZE)
            ),
            Handle(
                WidgetManagerView.EditMode.BOTTOM_RIGHT,
                Rect(width - dynamicEdgeSize, height - dynamicEdgeSize, width + CORNER_SIZE, height + CORNER_SIZE)
            ),
            
            // Edges
            Handle(
                WidgetManagerView.EditMode.TOP,
                Rect(dynamicEdgeSize, -THICKNESS, width - dynamicEdgeSize, dynamicEdgeSize)
            ),
            Handle(
                WidgetManagerView.EditMode.BOTTOM,
                Rect(dynamicEdgeSize, height - dynamicEdgeSize, width - dynamicEdgeSize, height + THICKNESS)
            ),
            Handle(
                WidgetManagerView.EditMode.LEFT,
                Rect(-THICKNESS, dynamicEdgeSize, dynamicEdgeSize, height - dynamicEdgeSize)
            ),
            Handle(
                WidgetManagerView.EditMode.RIGHT,
                Rect(width - dynamicEdgeSize, dynamicEdgeSize, width + THICKNESS, height - dynamicEdgeSize)
            )
        )

    }

    private fun getBounds(): Rect {
        return Rect(0, 0, width, height)
    }
}