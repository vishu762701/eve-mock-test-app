package com.eve.app.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.eve.app.designsystem.DesignMotion

/**
 * Requirement R04: Owner-authored greeting name tracking-expand intro.
 *
 * Treatment: ADAPTED (EVE owner-authored greeting animation, not original Telegram).
 * Performs a subtle ONE-TIME tracking/letter-spacing expansion on new session launch.
 * Starts slightly condensed (-0.02f em) and settles into natural spacing (0f em) with subtle alpha (0.85f -> 1f).
 * If the greeting contains characters from scripts where non-zero letterSpacing degrades ligatures or
 * conjunct shaping (Devanagari, Indic, Arabic, Hebrew, etc.), or if reduced motion is enabled,
 * tracking expansion is safely bypassed with a clean fade/translation fallback (keeping letterSpacing = 0f).
 */
object HomeGreetingMotion {

    const val KEY_HAS_ANIMATED_GREETING = "key_has_animated_greeting"
    const val CONDENSED_TRACKING_EMS = -0.02f
    const val NATURAL_TRACKING_EMS = 0f
    const val ANIMATION_DURATION_MS = 350L

    /**
     * Determines whether letter-spacing / tracking manipulation is safe for the given text.
     * Returns false if the text contains characters from scripts where inter-character spacing breaks
     * ligatures, conjunct consonants, virama connections, or RTL cursive shaping (Devanagari, Indic,
     * Arabic, Hebrew, Thai, etc.).
     */
    fun isTrackingSafeForText(text: CharSequence?): Boolean {
        if (text.isNullOrEmpty()) return true
        for (i in 0 until text.length) {
            val c = text[i]
            val code = c.code
            // Devanagari and Indic scripts
            if (code in 0x0900..0x0DFF || // Devanagari, Bengali, Gurmukhi, Gujarati, Oriya, Tamil, Telugu, Kannada, Malayalam, Sinhala
                code in 0x0590..0x08FF || // Hebrew, Arabic, Syriac, Thaana
                code in 0x0E00..0x109F || // Thai, Lao, Tibetan, Myanmar
                code in 0x1780..0x17FF || // Khmer
                code in 0x1CD0..0x1CFF || // Vedic Extensions
                code in 0xA8E0..0xA8FF || // Devanagari Extended
                code in 0xFB1D..0xFEFF    // Arabic/Hebrew Presentation Forms
            ) {
                return false
            }
            val dir = Character.getDirectionality(c)
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Resets the greeting view to its natural stationary state.
     */
    fun settleNatural(textView: TextView) {
        textView.letterSpacing = NATURAL_TRACKING_EMS
        textView.alpha = 1f
        textView.translationY = 0f
    }

    /**
     * Performs the one-time greeting intro motion if motion is enabled and tracking is safe.
     * If tracking is unsafe (e.g. Devanagari or RTL), performs a subtle fade/translation keeping
     * letterSpacing strictly at 0f to preserve grapheme cluster integrity.
     */
    fun animateIntro(textView: TextView, onEnd: (() -> Unit)? = null): Animator? {
        val context = textView.context
        if (!DesignMotion.enabled(context)) {
            settleNatural(textView)
            onEnd?.invoke()
            return null
        }

        val text = textView.text
        if (isTrackingSafeForText(text)) {
            textView.letterSpacing = CONDENSED_TRACKING_EMS
            textView.alpha = 0.85f
            val animator = ValueAnimator.ofFloat(CONDENSED_TRACKING_EMS, NATURAL_TRACKING_EMS).apply {
                duration = ANIMATION_DURATION_MS
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    val fraction = va.animatedFraction
                    val value = va.animatedValue as Float
                    textView.letterSpacing = value
                    textView.alpha = 0.85f + (0.15f * fraction)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        settleNatural(textView)
                        onEnd?.invoke()
                    }
                    override fun onAnimationCancel(animation: Animator) {
                        settleNatural(textView)
                    }
                })
            }
            animator.start()
            return animator
        } else {
            // Complex script fallback: preserve grapheme clusters and ligatures completely
            textView.letterSpacing = NATURAL_TRACKING_EMS
            textView.alpha = 0.70f
            val density = textView.resources.displayMetrics.density
            val startY = 4f * density
            textView.translationY = startY
            textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    settleNatural(textView)
                    onEnd?.invoke()
                }
                .start()
            return null
        }
    }
}
