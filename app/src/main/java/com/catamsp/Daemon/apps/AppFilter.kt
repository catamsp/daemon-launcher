package com.catamsp.Daemon.apps

import android.content.Context
import android.icu.text.Normalizer2
import android.os.Build
import com.catamsp.Daemon.actions.Action
import com.catamsp.Daemon.actions.AppAction
import com.catamsp.Daemon.actions.Gesture
import com.catamsp.Daemon.actions.ShortcutAction
import com.catamsp.Daemon.preferences.LauncherPreferences
import java.util.Locale
import kotlin.text.Regex.Companion.escape

class AppFilter(
    var context: Context,
    var query: String,
    var favoritesVisibility: AppSetVisibility = AppSetVisibility.VISIBLE,
    var hiddenVisibility: AppSetVisibility = AppSetVisibility.HIDDEN,
    var privateSpaceVisibility: AppSetVisibility = AppSetVisibility.VISIBLE
) {

    operator fun invoke(apps: List<AbstractDetailedAppInfo>): List<AbstractDetailedAppInfo> {
        var filteredApps = apps

        val hidden = LauncherPreferences.apps().hidden() ?: setOf()
        val favorites = LauncherPreferences.apps().favorites() ?: setOf()
        val private = apps.filter { it.isPrivate() }
            .map { it.getRawInfo() }.toSet()

        filteredApps = filteredApps.filter { info ->
            favoritesVisibility.predicate(favorites, info)
                    && hiddenVisibility.predicate(hidden, info)
                    && privateSpaceVisibility.predicate(private, info)
        }

        if (LauncherPreferences.apps().hideBoundApps()) {
            val boundApps = Gesture.entries
                .filter(Gesture::isEnabled)
                .mapNotNull { g -> Action.forGesture(g) }
                .mapNotNull {
                    (it as? AppAction)?.app
                        ?: (it as? ShortcutAction)?.shortcut
                }
                .toSet()
            filteredApps = filteredApps.filterNot { info -> boundApps.contains(info.getRawInfo()) }
        }

        if (query.isEmpty()) {
            return filteredApps
        }

        // normalize text for search
        val normalizedQuery = unicodeNormalize(query)
        val allowedSpecialCharacters = normalizedQuery
            .toCharArray()
            .distinct()
            .filter { c -> !c.isLetter() }
            .map { c -> escape(c.toString()) }
            .fold("") { x, y -> x + y }
        val disallowedCharsRegex = "[^\\p{L}$allowedSpecialCharacters]".toRegex()

        fun normalize(text: String): String {
            return text.replace(disallowedCharsRegex, "")
        }

        val r: MutableList<AbstractDetailedAppInfo> = ArrayList()
        val appsSecondary: MutableList<AbstractDetailedAppInfo> = ArrayList()
        val finalNormalizedQuery: String = normalize(normalizedQuery)
        
        for (item in filteredApps) {
            val itemLabel: String = normalize(item.getNormalizedLabel(context))

            if (itemLabel.startsWith(finalNormalizedQuery)) {
                r.add(item)
            } else if (itemLabel.contains(finalNormalizedQuery)) {
                appsSecondary.add(item)
            }
        }
        r.addAll(appsSecondary)

        return r
    }

    companion object {
        enum class AppSetVisibility(
            val predicate: (set: Set<AbstractAppInfo>, AbstractDetailedAppInfo) -> Boolean
        ) {
            VISIBLE({ _, _ -> true }),
            HIDDEN({ set, appInfo -> !set.contains(appInfo.getRawInfo()) }),
            EXCLUSIVE({ set, appInfo -> set.contains(appInfo.getRawInfo()) }),
            ;
        }

        private fun unicodeNormalize(s: String): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val normalizer = Normalizer2.getNFKDInstance()
                return normalizer.normalize(s.lowercase(Locale.ROOT))
            }
            return s.lowercase(Locale.ROOT)
        }
    }
}