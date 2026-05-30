package com.catamsp.Daemon.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.catamsp.Daemon.R
import com.catamsp.Daemon.apps.AppInfo
import com.catamsp.Daemon.apps.DetailedAppInfo
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import com.catamsp.Daemon.widgets.AppFolderWidget
import com.catamsp.Daemon.widgets.updateWidget

class FolderDialogFragment(
    private val folder: AppFolderWidget
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_folder_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.folder_title)
        val grid = view.findViewById<RecyclerView>(R.id.folder_grid)

        val fontName = LauncherPreferences.theme().font()
        title.typeface = Font.getTypeface(requireContext(), fontName)
        title.text = folder.label

        grid.layoutManager = GridLayoutManager(context, 4)
        grid.adapter = FolderAdapter(folder.apps) { appInfo ->
            // Launch the app
            val action = com.catamsp.Daemon.actions.AppAction(appInfo)
            action.invoke(requireActivity())
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private inner class FolderAdapter(
        private val apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val label: TextView = view.findViewById(R.id.app_label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.widget_app_icon, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appInfo = apps[position]
            val detailedInfo = DetailedAppInfo.fromAppInfo(appInfo, holder.itemView.context)

            holder.icon.load(detailedInfo ?: appInfo) {
                crossfade(true)
                diskCachePolicy(CachePolicy.ENABLED)
                memoryCachePolicy(CachePolicy.ENABLED)
            }
            holder.label.text = detailedInfo?.getLabel() ?: appInfo.packageName
            holder.itemView.setOnClickListener { onClick(appInfo) }
        }

        override fun getItemCount(): Int = apps.size
    }
}
