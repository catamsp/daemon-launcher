package com.catamsp.Daemon.preferences;

import java.util.HashMap;
import java.util.Set;

import com.catamsp.Daemon.R;
import com.catamsp.Daemon.actions.lock.LockMethod;
import com.catamsp.Daemon.preferences.list.AppNameFormat;
import com.catamsp.Daemon.preferences.list.ListLayout;
import com.catamsp.Daemon.preferences.serialization.MapAbstractAppInfoStringPreferenceSerializer;
import com.catamsp.Daemon.preferences.serialization.SetAbstractAppInfoPreferenceSerializer;
import com.catamsp.Daemon.preferences.serialization.SetPinnedShortcutInfoPreferenceSerializer;
import com.catamsp.Daemon.preferences.serialization.SetWidgetPanelSerializer;
import com.catamsp.Daemon.preferences.serialization.SetWidgetSerializer;
import com.catamsp.Daemon.preferences.theme.Background;
import com.catamsp.Daemon.preferences.theme.ColorTheme;
import com.catamsp.Daemon.preferences.theme.Font;
import com.catamsp.Daemon.preferences.theme.TransitionAnimation;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;

@Preferences(
        name = "com.catamsp.Daemon.preferences.LauncherPreferences",
        makeFile = true,
        r = R.class,
        value = {
                @PreferenceGroup(name = "internal", prefix = "settings_internal_", suffix = "_key", value = {
                        // set after the user finished the tutorial
                        @Preference(name = "started", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "started_time", type = long.class),
                        // see PREFERENCE_VERSION in com.catamsp.Daemon.preferences.Preferences.kt
                        @Preference(name = "version_code", type = int.class, defaultValue = "-1"),
                }),
                @PreferenceGroup(name = "apps", prefix = "settings_apps_", suffix = "_key", value = {
                        @Preference(name = "favorites", type = Set.class, serializer = SetAbstractAppInfoPreferenceSerializer.class),
                        @Preference(name = "hidden", type = Set.class, serializer = SetAbstractAppInfoPreferenceSerializer.class),
                        @Preference(name = "distracting_apps", type = Set.class, serializer = SetAbstractAppInfoPreferenceSerializer.class),
                        @Preference(name = "pinned_shortcuts", type = Set.class, serializer = SetPinnedShortcutInfoPreferenceSerializer.class),
                        @Preference(name = "custom_names", type = HashMap.class, serializer = MapAbstractAppInfoStringPreferenceSerializer.class),
                        @Preference(name = "hide_bound_apps", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "hide_paused_apps", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "hide_private_space_apps", type = boolean.class, defaultValue = "false"),
                }),
                @PreferenceGroup(name = "list", prefix = "settings_list_", suffix = "_key", value = {
                        @Preference(name = "layout", type = ListLayout.class, defaultValue = "DEFAULT"),
                        @Preference(name = "reverse_layout", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "app_name_format", type = AppNameFormat.class, defaultValue = "DEFAULT")
                }),
                @PreferenceGroup(name = "gestures", prefix = "settings_gesture_", suffix = "_key", value = {
                }),
                @PreferenceGroup(name = "enabled_gestures", prefix = "settings_enabled_gestures_", suffix = "_key", value = {
                        @Preference(name = "double_swipe", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "edge_swipe", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "edge_swipe_edge_width", type = int.class, defaultValue = "15"),
                        @Preference(name = "diagonal_swipe", type = boolean.class, defaultValue = "false"),
                }),
                @PreferenceGroup(name = "general", prefix = "settings_general_", suffix = "_key", value = {
                        @Preference(name = "choose_home_screen", type = void.class)
                }),
                @PreferenceGroup(name = "theme", prefix = "settings_theme_", suffix = "_key", value = {
                        @Preference(name = "wallpaper", type = void.class),
                        @Preference(name = "color_theme", type = ColorTheme.class, defaultValue = "DEFAULT"),
                        @Preference(name = "background", type = Background.class, defaultValue = "DIM"),
                        @Preference(name = "font", type = Font.class, defaultValue = "SYSTEM_DEFAULT"),
                        @Preference(name = "text_shadow", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "monochrome_icons", type = boolean.class, defaultValue = "false"),
                }),
                @PreferenceGroup(name = "animations", prefix = "settings_anim_", suffix = "_key", value = {
                        @Preference(name = "master_toggle", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "swipe_up", type = TransitionAnimation.class, defaultValue = "BOTTOM_UP"),
                        @Preference(name = "swipe_down", type = TransitionAnimation.class, defaultValue = "TOP_DOWN"),
                        @Preference(name = "swipe_left", type = TransitionAnimation.class, defaultValue = "RIGHT_LEFT"),
                        @Preference(name = "swipe_right", type = TransitionAnimation.class, defaultValue = "LEFT_RIGHT"),
                        @Preference(name = "other", type = TransitionAnimation.class, defaultValue = "FADE")
                }),
                @PreferenceGroup(name = "clock", prefix = "settings_clock_", suffix = "_key", value = {
                        @Preference(name = "font", type = Font.class, defaultValue = "SYSTEM_DEFAULT"),
                        @Preference(name = "color", type = int.class, defaultValue = "0xffffffff"),
                        @Preference(name = "date_visible", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "time_visible", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "flip_date_time", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "localized", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "show_seconds", type = boolean.class, defaultValue = "true"),
                }),
                @PreferenceGroup(name = "display", prefix = "settings_display_", suffix = "_key", value = {
                        @Preference(name = "screen_timeout_disabled", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "hide_status_bar", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "hide_navigation_bar", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "rotate_screen", type = boolean.class, defaultValue = "true"),
                }),
                @PreferenceGroup(name = "functionality", prefix = "settings_functionality_", suffix = "_key", value = {
                        @Preference(name = "search_auto_launch", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "search_web", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "search_auto_open_keyboard", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "search_auto_close_keyboard", type = boolean.class, defaultValue = "false"),
                }),
                @PreferenceGroup(name = "actions", prefix = "settings_actions_", suffix = "_key", value = {
                        @Preference(name = "lock_method", type = LockMethod.class, defaultValue = "DEVICE_ADMIN"),
                }),
                @PreferenceGroup(name = "globe", prefix = "settings_globe_", suffix = "_key", value = {
                        @Preference(name = "perspective", type = boolean.class, defaultValue = "false"),
                        @Preference(name = "show_glow", type = boolean.class, defaultValue = "true"),
                        @Preference(name = "glow_opacity", type = int.class, defaultValue = "204")
                }),
                @PreferenceGroup(name = "widgets", prefix = "settings_widgets_", suffix = "_key", value = {
                        @Preference(name = "widgets", type = Set.class, serializer = SetWidgetSerializer.class),
                        @Preference(name = "custom_panels", type = Set.class, serializer = SetWidgetPanelSerializer.class)
                }),
        })
public final class LauncherPreferences$Config {
}
