package com.catamsp.Daemon.apps

object SearchRouter {
    const val PREFIX_CONTACT = "@"
    const val PREFIX_FILE = "~"
    const val PREFIX_SETTING = "#"
    const val PREFIX_MATH = "="

    enum class SearchType {
        APP, CONTACT, FILE, SETTING, MATH
    }

    fun getSearchType(query: String): SearchType {
        if (query.isEmpty()) return SearchType.APP

        return when {
            query.startsWith(PREFIX_CONTACT) -> SearchType.CONTACT
            query.startsWith(PREFIX_FILE) -> SearchType.FILE
            query.startsWith(PREFIX_SETTING) -> SearchType.SETTING
            query.startsWith(PREFIX_MATH) -> SearchType.MATH
            else -> SearchType.APP
        }
    }
    
    fun getCleanQuery(query: String, type: SearchType): String {
        if (type == SearchType.APP) return query
        return if (query.length > 1) query.substring(1) else ""
    }
}
