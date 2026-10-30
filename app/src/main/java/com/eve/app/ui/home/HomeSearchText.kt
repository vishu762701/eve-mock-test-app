package com.eve.app.ui.home

import java.util.Locale

/** Locale-independent search matching; do not modify Home's selected category. */
internal object HomeSearchText {
    fun matches(query: String, vararg fields: String): Boolean {
        val needle = java.text.Normalizer.normalize(query.trim(), java.text.Normalizer.Form.NFC).lowercase(Locale.ROOT)
        return needle.isNotEmpty() && fields.any { field ->
            java.text.Normalizer.normalize(field, java.text.Normalizer.Form.NFC).lowercase(Locale.ROOT).contains(needle)
        }
    }
}
