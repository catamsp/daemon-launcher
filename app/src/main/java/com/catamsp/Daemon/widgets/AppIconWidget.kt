package com.catamsp.Daemon.widgets

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import coil.load
import coil.request.CachePolicy
import com.catamsp.Daemon.R
import com.catamsp.Daemon.apps.AbstractAppInfo
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.DetailedAppInfo
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.list.apps.openSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("widget:app_icon")
class AppIconWidget(
    val appInfo: AppInfo,
    override var position: WidgetPosition,
    override val panelId: Int,
    override var id: Int,
    override var allowInteraction: Boolean = true
) : Widget() {

    override fun createView(activity: Activity): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.widget_app_icon, null)
        val iconView = view.findViewById<ImageView>(R.id.app_icon)
        val labelView = view.findViewById<TextView>(R.id.app_label)

        // Load app icon
        val detailedInfo = DetailedAppInfo.fromAppInfo(appInfo, activity)
        iconView.load(detailedInfo ?: appInfo) {
            crossfade(true)
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
        }

        // Set label
        labelView.text = detailedInfo?.getLabel() ?: appInfo.packageName

        // Click to launch
        view.setOnClickListener {
            val action = com.catamsp.Daemon.actions.AppAction(appInfo)
            action.invoke(activity)
        }

        // Long click for context menu
        view.setOnLongClickListener { v ->
            showContextMenu(v, activity)
            true
        }

        return view
    }

    private fun showContextMenu(anchor: View, activity: Activity) {
        val popup = PopupMenu(activity, anchor)
        popup.menu.add(0, 1, 0, "Remove")
        popup.menu.add(0, 2, 1, "App Info")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    delete(activity)
                    // Refresh home screen
                    val app = activity.applicationContext as com.catamsp.Daemon.Application
                    val container = activity.findViewById<com.catamsp.Daemon.ui.widgets.WidgetContainerView>(
                        com.catamsp.Daemon.R.id.home_widget_container
                    )
                    container?.updateWidgets(activity, LauncherPreferences.widgets().widgets())
                    true
                }
                2 -> {
                    appInfo.openSettings(activity)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun findView(views: Sequence<View>): View? {
        return views.firstOrNull { it.findViewById<TextView>(R.id.app_label)?.text == appInfo.packageName }
    }

    override fun getPreview(context: Context): Drawable? {
        return DetailedAppInfo.fromAppInfo(appInfo, context)?.getIcon(context)
    }

    override fun getIcon(context: Context): Drawable? {
        return DetailedAppInfo.fromAppInfo(appInfo, context)?.getIcon(context)
    }

    override fun isConfigurable(context: Context): Boolean = false

    override fun configure(activity: Activity, requestCode: Int) {}
}
