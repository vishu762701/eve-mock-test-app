package com.eve.app.ui.admin

/** Pure pagination mapping, shared with the native animated strip and tested without Android. */
internal object AdminPageWindow {
    fun pages(page: Int, totalPages: Int): IntRange {
        if (totalPages <= 0) return IntRange.EMPTY
        val slots = minOf(totalPages, 5)
        val first = (page.coerceIn(1, totalPages) - slots / 2)
            .coerceIn(1, totalPages - slots + 1)
        return first until (first + slots)
    }
}
