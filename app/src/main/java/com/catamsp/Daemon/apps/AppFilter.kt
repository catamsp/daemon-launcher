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
    // Optimization Cache
    @Volatile private var lastQuery: String? = null
    @Volatile private var normalizedQueryCache: String? = null
    @Volatile private var disallowedCharsRegex: Regex? = null

    operator fun invoke(apps: List<AbstractDetailedAppInfo>): List<AbstractDetailedAppInfo> {
        val hidden = LauncherPreferences.apps().hidden() ?: emptySet()
        val favorites = LauncherPreferences.apps().favorites() ?: emptySet()

        // Optimize: Early filter by visibility before expensive normalization
        var filteredApps = apps.filter { info ->
            favoritesVisibility.predicate(favorites, info)
                    && hiddenVisibility.predicate(hidden, info)
                    && (!info.isPrivate() || privateSpaceVisibility == AppSetVisibility.VISIBLE)
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

        // Performance: Re-use compiled Regex and normalized string if query didn't change
        if (query != lastQuery) {
            lastQuery = query
            val rawNormalized = unicodeNormalize(query)
            val allowedSpecialCharacters = rawNormalized
                .toCharArray()
                .distinct()
                .filter { c -> !c.isLetter() }
                .map { c -> kotlin.text.Regex.Companion.escape(c.toString()) }
                .fold("") { x, y -> x + y }
            
            disallowedCharsRegex = "[^\\p{L}$allowedSpecialCharacters]".toRegex()
            normalizedQueryCache = rawNormalized.replace(disallowedCharsRegex!!, "")
        }

        val q = normalizedQueryCache ?: ""
        if (q.isEmpty()) return filteredApps

        val primary: MutableList<AbstractDetailedAppInfo> = ArrayList()
        val secondary: MutableList<AbstractDetailedAppInfo> = ArrayList()
        val regex = disallowedCharsRegex!!
        
        for (item in filteredApps) {
            // item.getNormalizedLabel(context) should be fast (already normalized in AbstractDetailedAppInfo)
            val itemLabel = item.getNormalizedLabel(context).replace(regex, "")

            if (itemLabel.startsWith(q)) {
                primary.add(item)
            } else if (itemLabel.contains(q)) {
                secondary.add(item)
            }
        }
        primary.addAll(secondary)

        return primary
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