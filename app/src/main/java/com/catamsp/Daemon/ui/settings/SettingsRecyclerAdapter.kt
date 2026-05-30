package com.catamsp.Daemon.ui.settings

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.catamsp.Daemon.R

class SettingsRecyclerAdapter : ListAdapter<SettingsItem, RecyclerView.ViewHolder>(SettingsDiffCallback()) {

    private var cachedTypeface: android.graphics.Typeface? = null
    private var cachedFontName: String? = null
    private var cachedSpacingMultiplier: Float = 1.0f
    private var cachedSpacingPref: String? = null

    fun invalidateCache() {
        cachedTypeface = null
        cachedFontName = null
        cachedSpacingPref = null
    }

    private fun getTypeface(context: android.content.Context): android.graphics.Typeface {
        val fontName = com.catamsp.Daemon.preferences.LauncherPreferences.theme().font()
        if (fontName != cachedFontName || cachedTypeface == null) {
            cachedFontName = fontName
            cachedTypeface = com.catamsp.Daemon.preferences.theme.Font.getTypeface(context, fontName)
        }
        return cachedTypeface!!
    }

    private fun getSpacingMultiplier(): Float {
        val spacingPref = com.catamsp.Daemon.preferences.LauncherPreferences.theme().spacingDensity()
        if (spacingPref != cachedSpacingPref) {
            cachedSpacingPref = spacingPref
            cachedSpacingMultiplier = when (spacingPref) {
                "compact" -> 0.85f
                "spacious" -> 1.15f
                else -> 1.0f
            }
        }
        return cachedSpacingMultiplier
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TOGGLE = 1
        const val TYPE_SLIDER = 2
        const val TYPE_CLICKABLE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SettingsItem.Header -> TYPE_HEADER
            is SettingsItem.Toggle -> TYPE_TOGGLE
            is SettingsItem.Slider -> TYPE_SLIDER
            is SettingsItem.Clickable -> TYPE_CLICKABLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_settings_header, parent, false))
            TYPE_TOGGLE -> ToggleViewHolder(inflater.inflate(R.layout.item_settings_toggle, parent, false))
            TYPE_SLIDER -> SliderViewHolder(inflater.inflate(R.layout.item_settings_slider, parent, false))
            TYPE_CLICKABLE -> ClickableViewHolder(inflater.inflate(R.layout.item_settings_clickable, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as SettingsItem.Header)
            is ToggleViewHolder -> holder.bind(item as SettingsItem.Toggle)
            is SliderViewHolder -> holder.bind(item as SettingsItem.Slider)
            is ClickableViewHolder -> holder.bind(item as SettingsItem.Clickable)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("FONT_UPDATE") || payloads.contains("SPACING_UPDATE")) {
            if (payloads.contains("FONT_UPDATE")) cachedTypeface = null
            if (payloads.contains("SPACING_UPDATE")) cachedSpacingPref = null
            val item = getItem(position)
            when (holder) {
                is HeaderViewHolder -> holder.bind(item as SettingsItem.Header)
                is ToggleViewHolder -> holder.bind(item as SettingsItem.Toggle)
                is SliderViewHolder -> holder.bind(item as SettingsItem.Slider)
                is ClickableViewHolder -> holder.bind(item as SettingsItem.Clickable)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.settings_item_header_title)
        fun bind(item: SettingsItem.Header) {
            title.text = item.title
            title.typeface = getTypeface(itemView.context)
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * getSpacingMultiplier())
        }
    }

inner class ToggleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.settings_item_toggle_title)
        private val desc: TextView = view.findViewById(R.id.settings_item_toggle_desc)
        private val icon: ImageView = view.findViewById(R.id.settings_item_toggle_icon)
        private val switch: SwitchCompat = view.findViewById(R.id.settings_item_toggle_switch)

        fun bind(item: SettingsItem.Toggle) {
            title.text = item.title
            desc.text = item.description
            desc.isVisible = item.description != null
            
            val tf = getTypeface(itemView.context)
            val multiplier = getSpacingMultiplier()
            title.typeface = tf
            desc.typeface = tf
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * multiplier)
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * multiplier)
            
            if (item.icon != null) {
                icon.setImageDrawable(item.icon)
                icon.isVisible = true
            } else {
                icon.isVisible = false
            }
            
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = item.isChecked
            switch.setOnCheckedChangeListener { _, isChecked -> item.onToggle(isChecked) }
            
            itemView.setOnClickListener { switch.isChecked = !switch.isChecked }
        }
    }

    inner class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.settings_item_slider_title)
        private val desc: TextView = view.findViewById(R.id.settings_item_slider_desc)
        private val valueText: TextView = view.findViewById(R.id.settings_item_slider_value)
        private val seekBar: SeekBar = view.findViewById(R.id.settings_item_slider_seekbar)

        fun bind(item: SettingsItem.Slider) {
            title.text = item.title
            desc.text = item.description
            desc.isVisible = item.description != null
            
            val tf = getTypeface(itemView.context)
            val multiplier = getSpacingMultiplier()
            title.typeface = tf
            desc.typeface = tf
            valueText.typeface = tf
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * multiplier)
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * multiplier)
            valueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * multiplier)
            
            valueText.text = item.value.toString()
            seekBar.max = item.max - item.min
            seekBar.progress = item.value - item.min

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val actualValue = progress + item.min
                    valueText.text = actualValue.toString()
                    if (fromUser) {
                        item.onValueChange(actualValue)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

inner class ClickableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.settings_item_clickable_title)
        private val desc: TextView = view.findViewById(R.id.settings_item_clickable_desc)
        private val icon: ImageView = view.findViewById(R.id.settings_item_clickable_icon)
        private val removeBtn: ImageView = view.findViewById(R.id.settings_item_clickable_remove)
        private val arrow: ImageView = view.findViewById(R.id.settings_item_clickable_arrow)

        fun bind(item: SettingsItem.Clickable) {
            title.text = item.title
            desc.text = item.description
            desc.isVisible = item.description != null
            
            val tf = getTypeface(itemView.context)
            val multiplier = getSpacingMultiplier()
            title.typeface = tf
            desc.typeface = tf
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * multiplier)
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * multiplier)
            
            if (item.icon != null) {
                icon.setImageDrawable(item.icon)
                icon.isVisible = true
            } else {
                icon.isVisible = false
            }

            // Always ensure the arrow is visible for clickable items
            arrow.isVisible = true 
            
            if (item.onRemove != null) {
                removeBtn.isVisible = true
                removeBtn.setOnClickListener { item.onRemove.invoke() }
                
                // If it's a font setting, use an 'add' icon instead of 'close'
                if (item.key.contains("font")) {
                    removeBtn.setImageResource(R.drawable.baseline_add_24)
                } else {
                    removeBtn.setImageResource(R.drawable.baseline_close_24)
                }
            } else {
                removeBtn.isVisible = false
            }

            itemView.setOnClickListener { item.onClick() }
        }
    }
}

class SettingsDiffCallback : DiffUtil.ItemCallback<SettingsItem>() {
    override fun areItemsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
        return oldItem.key == newItem.key
    }
    override fun areContentsTheSame(oldItem: SettingsItem, newItem: SettingsItem): Boolean {
        return oldItem == newItem
    }
}
