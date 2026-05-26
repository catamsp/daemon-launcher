package com.catamsp.Daemon.preferences.theme

import android.content.res.Resources
import com.catamsp.Daemon.R

import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

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

    fun getTypeface(context: android.content.Context): Typeface {
        return when (this) {
            HACK -> ResourcesCompat.getFont(context, R.font.hack)!!
            SYSTEM_DEFAULT -> Typeface.DEFAULT
            SANS_SERIF -> Typeface.SANS_SERIF
            SERIF -> Typeface.SERIF
            MONOSPACE -> Typeface.MONOSPACE
            SERIF_MONOSPACE -> Typeface.create("serif-monospace", Typeface.NORMAL)
            SPACE_GROTESK -> ResourcesCompat.getFont(context, R.font.space_grotesk)!!
            MICHROMA -> ResourcesCompat.getFont(context, R.font.michroma)!!
        }
    }
}