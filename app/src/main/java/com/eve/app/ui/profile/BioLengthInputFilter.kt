/*
 * Telegram Android codepoint-filter semantics combined with EVE's existing
 * 160-UTF-16-unit Worker contract. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.text.InputFilter
import android.text.Spanned

/**
 * EVE-adapted bio length filter. Telegram's `CodepointsLengthInputFilter`
 * counts Unicode code points (so emoji count once), but the existing EVE
 * Worker validates `body.bio.length <= 160`, which is UTF-16 units. Enforcing
 * both keeps Telegram's surrogate-safe behavior without allowing a bio the
 * deployed server will reject.
 */
class BioLengthInputFilter(
    private val maxCodepoints: Int = MAX_CODEPOINTS,
    private val maxUnits: Int = MAX_UTF16_UNITS
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        if (source.isEmpty() || start == end) return null
        val before = dest.subSequence(0, dstart)
        val after = dest.subSequence(dend, dest.length)

        fun fits(candidate: CharSequence): Boolean {
            val text = StringBuilder(before.length + candidate.length + after.length)
                .append(before).append(candidate).append(after)
            return text.length <= maxUnits &&
                Character.codePointCount(text, 0, text.length) <= maxCodepoints
        }

        val inserted = source.subSequence(start, end)
        if (fits(inserted)) return null
        // Keep the longest prefix that still fits both limits. Subtract whole
        // code points so a surrogate pair is never split.
        var cut = end
        while (cut > start) {
            cut -= Character.charCount(Character.codePointBefore(source, cut))
            val candidate = source.subSequence(start, cut)
            if (fits(candidate)) return candidate
        }
        return ""
    }

    companion object {
        const val MAX_CODEPOINTS = 160
        const val MAX_UTF16_UNITS = 160

        /** Counter value matching the stricter of the source and server limits. */
        fun remaining(text: CharSequence): Int {
            val codepointsLeft = MAX_CODEPOINTS - Character.codePointCount(text, 0, text.length)
            val unitsLeft = MAX_UTF16_UNITS - text.length
            return minOf(codepointsLeft, unitsLeft).coerceAtLeast(0)
        }

        fun fits(text: CharSequence): Boolean =
            text.length <= MAX_UTF16_UNITS &&
                Character.codePointCount(text, 0, text.length) <= MAX_CODEPOINTS
    }
}
