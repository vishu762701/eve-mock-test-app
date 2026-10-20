package com.eve.app.designsystem

import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.animation.PathInterpolator
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Authoritative UIArc Motion Tokens & Physics Specification.
 * Genuine native Kotlin translation of official UIArc sources:
 * - Registry: https://uiarc.dev/r/arc-motion-tokens.json
 * - Motion specification: https://uiarc.dev/docs/motion
 */
object UiArcMotion {

    /** Durations in milliseconds matching UIArc's duration tokens (0.12s, 0.16s, 0.18s, 0.24s, 0.48s, 1.80s). */
    object Duration {
        const val INSTANT = 120L
        const val FAST = 160L
        const val EXIT = 180L
        const val STANDARD = 240L
        const val CONSIDERED = 480L
        const val SHIMMER_SWEEP = 1800L
        const val SHIMMER_PAUSE = 576L // 1.8s * 0.32
    }

    /** Exact cubic-bezier easings from UIArc motion tokens. */
    object Ease {
        val ENTER = PathInterpolator(0.16f, 1.0f, 0.30f, 1.0f)
        val EXIT = PathInterpolator(0.70f, 0.0f, 0.84f, 0.0f)
        val STANDARD = PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)
        val IN_OUT = PathInterpolator(0.65f, 0.0f, 0.35f, 1.0f)
    }

    val Easing get() = Ease

    /** Stagger steps in milliseconds. */
    object Stagger {
        const val CHAR_MS = 16L
        const val WORD_MS = 40L
        const val LINE_MS = 80L
        const val ITEM_MS = 35L
    }

    /** Text and icon crossfade blur radii in pixels. */
    object Blur {
        const val SUBTLE_PX = 2f
        const val SOFT_PX = 4f
        const val TEXT_PX = 8f
    }

    /**
     * Damped harmonic oscillator closed-form solution.
     * Accurately converts visualDuration and bounce (per Framer Motion / Motion specification)
     * into exact physical spring equations:
     *   dampingRatio = 1 - bounce
     *   k = (2 * pi / (1.2 * visualDuration))^2
     */
    class SpringCurve(
        val visualDuration: Float,
        val bounce: Float,
        val settlingDurationMs: Long
    ) : TimeInterpolator {
        private val zeta: Float = (1f - bounce).coerceIn(0.01f, 1.0f)
        private val k: Float = ((2.0 * Math.PI) / (1.2 * visualDuration)).let { (it * it).toFloat() }
        private val wn: Float = sqrt(k)
        private val wd: Float = if (zeta < 1f) wn * sqrt(1f - zeta * zeta) else 0f
        private val durationSeconds: Float = settlingDurationMs / 1000f

        override fun getInterpolation(input: Float): Float {
            if (input <= 0f) return 0f
            if (input >= 1f) return 1f
            val t = input * durationSeconds
            return if (zeta < 1f) {
                val decay = exp(-zeta * wn * t)
                val osc = cos(wd * t) + (zeta / sqrt(1f - zeta * zeta)) * sin(wd * t)
                1f - (decay * osc)
            } else {
                1f - exp(-wn * t) * (1f + wn * t)
            }
        }
    }

    /** Authoritative spring presets defined in UIArc motion tokens. */
    object Spring {
        // visualDuration: 0.26s, bounce: 0.12 (zeta = 0.88, k = 405.5) -> settles ~360ms
        val SNAPPY = SpringCurve(visualDuration = 0.26f, bounce = 0.12f, settlingDurationMs = 360L)
        // visualDuration: 0.40s, bounce: 0.00 (zeta = 1.00, k = 171.3) -> settles ~500ms
        val SMOOTH = SpringCurve(visualDuration = 0.40f, bounce = 0.00f, settlingDurationMs = 500L)
        // visualDuration: 0.42s, bounce: 0.16 (zeta = 0.84, k = 155.4) -> settles ~550ms
        val MORPH = SpringCurve(visualDuration = 0.42f, bounce = 0.16f, settlingDurationMs = 550L)
        // visualDuration: 0.48s, bounce: 0.00 (reveals on first view) -> settles ~600ms
        val REVEAL = SpringCurve(visualDuration = 0.48f, bounce = 0.00f, settlingDurationMs = 600L)
        // explicit stiffness: 520, damping: 38 (zeta = 0.833) -> settles ~260ms
        val RESPONSIVE = SpringCurve(visualDuration = 0.23f, bounce = 0.167f, settlingDurationMs = 260L)
    }

    /**
     * Button press scale calculation from UIArc button:
     * width > 220px: 0.985; width <= 48px: 0.96; default: 0.97.
     */
    fun buttonPressScale(widthPx: Int, density: Float): Float {
        val widthDp = if (density > 0f) widthPx / density else widthPx.toFloat()
        return when {
            widthDp > 220f -> 0.985f
            widthDp in 1f..48f -> 0.96f
            else -> 0.97f
        }
    }

    /** Returns true if motion animations are enabled by system accessibility and animator settings. */
    fun isMotionEnabled(context: Context): Boolean = DesignMotion.enabled(context)
}
