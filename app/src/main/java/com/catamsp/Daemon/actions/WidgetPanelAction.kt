package com.catamsp.Daemon.actions

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R
import com.catamsp.Daemon.ui.widgets.WidgetPanelActivity
import com.catamsp.Daemon.ui.widgets.manage.EXTRA_PANEL_ID
import com.catamsp.Daemon.ui.widgets.manage.WidgetPanelsRecyclerAdapter
import com.catamsp.Daemon.widgets.WidgetPanel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("action:panel")
class WidgetPanelAction(val widgetPanelId: Int) : Action {

    override fun invoke(context: Context, rect: Rect?, opts: android.os.Bundle?): Boolean {

        if (context is WidgetPanelActivity) {
            context.finish()
            return true
        }

        if (WidgetPanel.byId(this.widgetPanelId) == null) {
            Toast.makeText(context, R.string.alert_widget_panel_not_found, Toast.LENGTH_LONG).show()
        } else {
            context.startActivity(Intent(context, WidgetPanelActivity::class.java).also {
                it.putExtra(EXTRA_PANEL_ID, this.widgetPanelId)
                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }, opts)
        }
        return true
    }

    override fun label(context: Context): String {
        return WidgetPanel.byId(widgetPanelId)?.label
            ?: context.getString(R.string.list_other_open_widget_panel)
    }

    override fun isAvailable(context: Context): Boolean {
        return WidgetPanel.byId(widgetPanelId) != null
    }

    override fun canReachSettings(): Boolean {
        return false
    }

    override fun getIcon(context: Context): Drawable? {
        return ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.baseline_widgets_24,
            context.theme
        )
    }

    override fun showConfigurationDialog(context: Context, onSuccess: (Action) -> Unit) {
        AlertDialog.Builder(context, R.style.AlertDialogCustom).apply {
            setTitle(R.string.dialog_select_widget_panel_title)
            setNegativeButton(R.string.dialog_cancel) { _, _ -> }
            setView(R.layout.dialog_select_widget_panel)
        }.create().also { it.show() }.also { alertDialog ->
            val infoTextView =
                alertDialog.findViewById<TextView>(R.id.dialog_select_widget_panel_info)
            alertDialog.findViewById<RecyclerView>(R.id.dialog_select_widget_panel_recycler)
                ?.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(alertDialog.context)
                    adapter =
                        WidgetPanelsRecyclerAdapter(alertDialog.context, false) { widgetPanel ->
                            onSuccess(WidgetPanelAction(widgetPanel.id))
                            alertDialog.dismiss()
                        }
                    if (adapter?.itemCount == 0) {
                        infoTextView?.visibility = View.VISIBLE
                    }
                }
        }
        true
    }
}
