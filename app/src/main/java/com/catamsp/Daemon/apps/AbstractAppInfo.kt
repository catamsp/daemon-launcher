package com.catamsp.Daemon.apps

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * This interface is implemented by [AppInfo] and [PinnedShortcutInfo].
 */
@Serializable
sealed interface AbstractAppInfo {
    fun serialize(): String {
        return Json.encodeToString(this)
    }

    companion object {
        const val INVALID_USER = -1

        private val json = Json { ignoreUnknownKeys = true }

        fun deserialize(serialized: String): AbstractAppInfo {
            return json.decodeFromString(serialized)
        }
    }
}