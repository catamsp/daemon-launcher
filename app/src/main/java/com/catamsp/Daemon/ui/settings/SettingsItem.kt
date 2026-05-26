package com.catamsp.Daemon.ui.settings

import android.graphics.drawable.Drawable

sealed class SettingsItem(val key: String) {
    data class Header(val itemKey: String, val title: String) : SettingsItem(itemKey)
    
    data class Toggle(
        val itemKey: String,
        val title: String,
        val description: String?,
        val icon: Drawable?,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingsItem(itemKey)

    data class Slider(
        val itemKey: String,
        val title: String,
        val description: String?,
        val min: Int,
        val max: Int,
        val value: Int,
        val onValueChange: (Int) -> Unit
    ) : SettingsItem(itemKey)

    data class Clickable(
        val itemKey: String,
        val title: String,
        val description: String?,
        val icon: Drawable? = null,
        val onRemove: (() -> Unit)? = null,
        val onClick: () -> Unit
    ) : SettingsItem(itemKey)
}