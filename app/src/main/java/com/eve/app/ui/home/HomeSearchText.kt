package com.eve.app.ui.home

import java.util.Locale

/** Locale-independent search matching; do not modify Home's selected category. */
internal object HomeSearchText {
    fun matches(query: String, vararg fields: String): Boolean {
        val needle = query.trim().lowercase(Locale.ROOT)
        return needle.isNotEmpty() && fields.any { field ->
            field.lowercase(Locale.ROOT).contains(needle)
        }
    }
}
