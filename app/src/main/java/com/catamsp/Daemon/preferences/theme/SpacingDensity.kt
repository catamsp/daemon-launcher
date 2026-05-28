package com.catamsp.Daemon.preferences.theme

/**
 * Text spacing density options for the launcher UI.
 * Changes here must also be added to @array/settings_theme_spacing_values
 */
@Suppress("unused")
enum class SpacingDensity(val multiplier: Float) {
    COMPACT(0.8f),
    DEFAULT(1.0f),
    SPACIOUS(1.2f),
    ;

    companion object {
        fun fromValue(value: String): SpacingDensity {
            return when (value) {
                "compact" -> COMPACT
                "spacious" -> SPACIOUS
                else -> DEFAULT
            }
        }
    }
}