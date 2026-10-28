package com.eve.app.ui.home

/** Content-only card label. Caller supplies already-localized exam metadata/status. */
internal object HomeExamAccessibility {
    fun describe(name: CharSequence?, details: CharSequence?, pinnedStatus: CharSequence?): String =
        listOfNotNull(name, details, pinnedStatus)
            .map { it.toString().trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
}
