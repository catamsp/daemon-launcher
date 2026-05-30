package com.catamsp.Daemon.ui.settings

import android.graphics.drawable.Drawable

sealed class SettingsItem(val key: String) {

    class Header(itemKey: String, val title: String) : SettingsItem(itemKey) {
        override fun equals(other: Any?): Boolean {
            return other is Header && key == other.key && title == other.title
        }
        override fun hashCode() = key.hashCode() * 31 + title.hashCode()
    }

    class Toggle(
        itemKey: String,
        val title: String,
        val description: String?,
        val icon: Drawable?,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingsItem(itemKey) {
        override fun equals(other: Any?): Boolean {
            return other is Toggle && key == other.key && isChecked == other.isChecked
        }
        override fun hashCode() = key.hashCode() * 31 + isChecked.hashCode()
    }

    class Slider(
        itemKey: String,
        val title: String,
        val description: String?,
        val min: Int,
        val max: Int,
        val value: Int,
        val onValueChange: (Int) -> Unit
    ) : SettingsItem(itemKey) {
        override fun equals(other: Any?): Boolean {
            return other is Slider && key == other.key && value == other.value && min == other.min && max == other.max
        }
        override fun hashCode() = key.hashCode() * 31 + value.hashCode()
    }

    class Clickable(
        itemKey: String,
        val title: String,
        val description: String?,
        val icon: Drawable? = null,
        val onRemove: (() -> Unit)? = null,
        val onClick: () -> Unit
    ) : SettingsItem(itemKey) {
        override fun equals(other: Any?): Boolean {
            return other is Clickable && key == other.key && title == other.title && description == other.description
        }
        override fun hashCode() = key.hashCode() * 31 + title.hashCode()
    }
}
