package com.catamsp.Daemon.ui.list.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R

class UniversalResultAdapter : ListAdapter<UniversalSearchResult, UniversalResultAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<UniversalSearchResult>() {
        override fun areItemsTheSame(oldItem: UniversalSearchResult, newItem: UniversalSearchResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UniversalSearchResult, newItem: UniversalSearchResult): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.list_apps_row_name)
        val iconImage: ImageView = itemView.findViewById(R.id.list_apps_row_icon)
        val actionIconImage: ImageView = itemView.findViewById(R.id.list_apps_row_action_icon)

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition).action.invoke()
                }
            }
            actionIconImage.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition).endAction?.invoke()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_apps_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        // CRITICAL FIX: Manually apply the selected typeface to search results
        val fontName = com.catamsp.Daemon.preferences.LauncherPreferences.theme().font()
        holder.titleText.typeface = com.catamsp.Daemon.preferences.theme.Font.getTypeface(holder.itemView.context, fontName)

        if (item.subtitle != null) {
            holder.titleText.text = "${item.title}\n${item.subtitle}"
        } else {
            holder.titleText.text = item.title
        }
        
        if (item.icon != null) {
            holder.iconImage.setImageDrawable(item.icon)
            holder.iconImage.visibility = View.VISIBLE
        } else {
            holder.iconImage.visibility = View.INVISIBLE
        }

        if (item.endIconRes != null) {
            holder.actionIconImage.setImageResource(item.endIconRes)
            holder.actionIconImage.visibility = View.VISIBLE
        } else {
            holder.actionIconImage.visibility = View.GONE
        }
    }
}