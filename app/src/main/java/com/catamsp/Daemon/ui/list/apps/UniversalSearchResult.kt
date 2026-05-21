package com.catamsp.Daemon.ui.list.apps

import android.graphics.drawable.Drawable

data class UniversalSearchResult(
    val id: String, // Added ID for DiffUtil
    val title: String,
    val subtitle: String? = null,
    val icon: Drawable? = null,
    val endIconRes: Int? = null, // Resource ID for the end icon
    val endAction: (() -> Unit)? = null,
    val action: () -> Unit
)