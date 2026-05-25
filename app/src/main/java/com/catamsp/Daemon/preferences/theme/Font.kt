package com.catamsp.Daemon.preferences.theme

import android.content.res.Resources
import com.catamsp.Daemon.R

/**
 * Changes here must also be added to @array/settings_theme_font_values
 */

@Suppress("unused")
enum class Font(val id: Int) {
    HACK(R.style.fontHack),
    SYSTEM_DEFAULT(R.style.fontSystemDefault),
    SANS_SERIF(R.style.fontSansSerif),
    SERIF(R.style.fontSerif),
    MONOSPACE(R.style.fontMonospace),
    SERIF_MONOSPACE(R.style.fontSerifMonospace),
    SPACE_GROTESK(R.style.fontSpaceGrotesk),
    MICHROMA(R.style.fontMichroma),
    ;

    fun applyToTheme(theme: Resources.Theme) {
        theme.applyStyle(id, true)
    }
}