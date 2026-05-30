package com.catamsp.Daemon.ui.settings

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R
import com.catamsp.Daemon.apps.IconPackManager
import com.catamsp.Daemon.preferences.LauncherPreferences
import com.catamsp.Daemon.preferences.theme.Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IconPackPickerDialog(
    private val onIconSelected: (String) -> Unit
) : DialogFragment() {

    private data class IconItem(
        val drawableName: String,
        var thumbnail: Drawable? = null
    )

    private var items: List<IconItem> = emptyList()
    private lateinit var adapter: IconGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_icon_pack_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.picker_title)
        val grid = view.findViewById<RecyclerView>(R.id.icon_grid)

        val fontName = LauncherPreferences.theme().font()
        title.typeface = Font.getTypeface(requireContext(), fontName)

        val packManager = try {
            IconPackManager.getInstance(requireContext())
        } catch (_: Exception) { null }

        if (packManager == null || !packManager.isLoaded()) {
            dismiss()
            return
        }

        // Load drawable names on IO thread
        viewLifecycleOwner.lifecycleScope.launch {
            val icons = withContext(Dispatchers.IO) {
                packManager.getAvailableIcons()
            }
            if (!isAdded) return@launch

            items = icons.map { IconItem(it.drawableName) }
            adapter = IconGridAdapter(items) { item ->
                onIconSelected(item.drawableName)
                dismiss()
            }
            grid.layoutManager = GridLayoutManager(context, 4)
            grid.adapter = adapter

            // Load first batch of thumbnails
            loadThumbnails(0, 20, packManager)
        }
    }

    private fun loadThumbnails(start: Int, count: Int, packManager: IconPackManager) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (i in start until minOf(start + count, items.size)) {
                    if (items[i].thumbnail == null) {
                        val drawable = packManager.getDrawableByName(items[i].drawableName)
                        items[i].thumbnail = drawable
                        withContext(Dispatchers.Main) {
                            if (isAdded && ::adapter.isInitialized) {
                                adapter.notifyItemChanged(i)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private inner class IconGridAdapter(
        private val items: List<IconItem>,
        private val onClick: (IconItem) -> Unit
    ) : RecyclerView.Adapter<IconGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val thumb: ImageView = view.findViewById(R.id.icon_thumb)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_icon_pack_icon, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            if (item.thumbnail != null) {
                holder.thumb.setImageDrawable(item.thumbnail)
            } else {
                holder.thumb.setImageResource(android.R.drawable.ic_menu_gallery)
                // Load this thumbnail
                val packManager = try {
                    IconPackManager.getInstance(holder.itemView.context)
                } catch (_: Exception) { null }
                if (packManager != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val drawable = withContext(Dispatchers.IO) {
                            packManager.getDrawableByName(item.drawableName)
                        }
                        if (drawable != null) {
                            item.thumbnail = drawable
                            val pos = holder.bindingAdapterPosition
                            if (pos != RecyclerView.NO_POSITION && pos >= 0 && pos < items.size) {
                                notifyItemChanged(pos)
                            }
                        }
                    }
                }
            }
            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos >= 0 && pos < items.size) {
                    onClick(items[pos])
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
