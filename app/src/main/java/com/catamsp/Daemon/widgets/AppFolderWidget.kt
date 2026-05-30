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
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.DetailedAppInfo
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.ui.settings.FolderDialogFragment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("widget:app_folder")
class AppFolderWidget(
    val apps: MutableList<AppInfo>,
    var label: String,
    override var position: WidgetPosition,
    override val panelId: Int,
    override var id: Int,
    override var allowInteraction: Boolean = true
) : Widget() {

    override fun createView(activity: Activity): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.widget_app_folder, null)

        // Show first 4 app icons in the folder preview
        val iconViews = listOf(
            view.findViewById<ImageView>(R.id.folder_icon_1),
            view.findViewById<ImageView>(R.id.folder_icon_2),
            view.findViewById<ImageView>(R.id.folder_icon_3),
            view.findViewById<ImageView>(R.id.folder_icon_4)
        )

        for (i in 0 until minOf(4, apps.size)) {
            val detailedInfo = DetailedAppInfo.fromAppInfo(apps[i], activity)
            iconViews[i].load(detailedInfo ?: apps[i]) {
                crossfade(true)
                diskCachePolicy(CachePolicy.ENABLED)
                memoryCachePolicy(CachePolicy.ENABLED)
            }
        }

        // Fill empty icon slots with a placeholder
        for (i in apps.size until 4) {
            iconViews[i].setImageResource(android.R.drawable.ic_menu_gallery)
            iconViews[i].alpha = 0.3f
        }

        // Set label
        val labelView = view.findViewById<TextView>(R.id.folder_label)
        labelView.text = label

        // Click to open folder
        view.setOnClickListener {
            val fragmentManager = (activity as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager ?: return@setOnClickListener
            val dialog = FolderDialogFragment(this)
            dialog.show(fragmentManager, "FolderDialog")
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
        popup.menu.add(0, 2, 1, "Rename")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    delete(activity)
                    val container = activity.findViewById<com.catamsp.Daemon.ui.widgets.WidgetContainerView>(
                        R.id.home_widget_container
                    )
                    container?.updateWidgets(activity, LauncherPreferences.widgets().widgets())
                    true
                }
                2 -> {
                    // Show rename dialog
                    val editText = android.widget.EditText(activity).apply {
                        setText(label)
                        setSelection(label.length)
                    }
                    android.app.AlertDialog.Builder(activity, R.style.AlertDialogCustom)
                        .setTitle("Rename Folder")
                        .setView(editText)
                        .setPositiveButton("OK") { _, _ ->
                            label = editText.text.toString()
                            updateWidget(this@AppFolderWidget)
                            val container = activity.findViewById<com.catamsp.Daemon.ui.widgets.WidgetContainerView>(
                                R.id.home_widget_container
                            )
                            container?.updateWidgets(activity, LauncherPreferences.widgets().widgets())
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun findView(views: Sequence<View>): View? {
        return views.mapNotNull { it as? TextView }.firstOrNull { it.text == label }
    }

    override fun getPreview(context: Context): Drawable? {
        return null
    }

    override fun getIcon(context: Context): Drawable? {
        return null
    }

    override fun isConfigurable(context: Context): Boolean = false

    override fun configure(activity: Activity, requestCode: Int) {}
}
