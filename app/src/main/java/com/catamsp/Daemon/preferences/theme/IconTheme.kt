package com.catamsp.Daemon.preferences.theme

import com.catamsp.Daemon.R

enum class IconTheme(val labelRes: Int) {
    NONE(R.string.icon_theme_none),
    CIRCLE(R.string.icon_theme_circle),
    SQUIRCLE(R.string.icon_theme_squircle),
    ROUNDED_SQUARE(R.string.icon_theme_rounded_square),
    DIAMOND(R.string.icon_theme_diamond),
    TEARDROP(R.string.icon_theme_teardrop),
    HEXAGON(R.string.icon_theme_hexagon);

    fun getLabel(context: android.content.Context): String {
        return context.getString(labelRes)
    }
}
