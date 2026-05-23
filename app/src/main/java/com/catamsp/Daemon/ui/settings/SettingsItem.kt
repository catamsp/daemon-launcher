package com.catamsp.Daemon.ui.settings

sealed class SettingsItem {
    abstract val key: String

    data class Header(
        override val key: String,
        val title: String
    ) : SettingsItem()

    data class Toggle(
        override val key: String,
        val title: String,
        val description: String? = null,
        val icon: android.graphics.drawable.Drawable? = null,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingsItem()

    data class Slider(
        override val key: String,
        val title: String,
        val description: String? = null,
        val value: Int,
        val min: Int,
        val max: Int,
        val onValueChange: (Int) -> Unit
    ) : SettingsItem()

    data class Clickable(
        override val key: String,
        val title: String,
        val description: String? = null,
        val icon: android.graphics.drawable.Drawable? = null,
        val onRemove: (() -> Unit)? = null,
        val onClick: () -> Unit
    ) : SettingsItem()
}
