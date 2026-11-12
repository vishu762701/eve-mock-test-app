/*
 * Derived from Telegram Android CodepointsLengthInputFilter.java,
 * Copyright Nikolai Kudashov / Telegram contributors.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.text.InputFilter
import android.text.Spanned

/**
 * Port of `CodepointsLengthInputFilter` (`CodepointsLengthInputFilter.java:7-30`).
 * Telegram limits the bio by Unicode code points rather than UTF-16 units, so
 * supplementary characters (emoji) count once. The filter stays pure; the bio
 * editor mirrors the source caller's rejection check
 * (`ChangeBioActivity.java:112-128`) to decide when to shake and vibrate.
 */
class CodepointsLengthInputFilter(private val max: Int) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val destAfter = Character.codePointCount(dest, 0, dest.length) -
            Character.codePointCount(dest, dstart, dend)
        val keep = max - destAfter
        if (keep <= 0) return ""
        if (keep >= Character.codePointCount(source, start, end)) return null
        var cut = keep + start
        if (Character.isHighSurrogate(source[cut - 1])) {
            cut--
            if (cut == start) return ""
        }
        return source.subSequence(start, cut)
    }
}
