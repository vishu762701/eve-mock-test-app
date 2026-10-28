package com.eve.app.ui.home

/** Data-only Home filter state transition; presentation and Telegram styling live elsewhere. */
internal object HomeCategorySelection {
    fun resolve(selected: String, available: List<String>, fallback: String): String =
        if (selected in available) selected else fallback

    fun select(current: String, requested: String, available: List<String>): String =
        if (requested in available) requested else current
}
