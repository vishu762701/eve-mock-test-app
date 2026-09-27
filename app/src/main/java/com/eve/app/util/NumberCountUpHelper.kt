package com.eve.app.util

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Telegram-style Number Count-Up Animation (matching NumberTextView.java pattern).
 * Animates numbers counting up from 0 to target value each frame using AccelerateDecelerateInterpolator.
 */
object NumberCountUpHelper {

    fun animateScoreCountUp(
        textView: TextView,
        targetScore: Double,
        total: Int,
        durationMs: Long = 1000L,
        onFinished: (() -> Unit)? = null
    ) {
        val isWhole = (targetScore % 1.0 == 0.0)
        val startVal = 0f
        val endVal = targetScore.toFloat()

        ValueAnimator.ofFloat(startVal, endVal).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                val text = if (isWhole) {
                    v.roundToInt().toString()
                } else {
                    String.format("%.2f", v)
                }
                textView.text = "$text / $total"
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    val finalStr = if (isWhole) targetScore.toInt().toString() else String.format("%.2f", targetScore)
                    textView.text = "$finalStr / $total"
                    onFinished?.invoke()
                }
            })
            start()
        }
    }

    fun animateStatsCountUp(
        textView: TextView,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        durationMs: Long = 800L
    ) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val progress = va.animatedValue as Float
                val c = (correct * progress).roundToInt()
                val w = (wrong * progress).roundToInt()
                val u = (unattempted * progress).roundToInt()
                textView.text = "Correct: $c   Wrong: $w   Unattempted: $u"
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    textView.text = "Correct: $correct   Wrong: $wrong   Unattempted: $unattempted"
                }
            })
            start()
        }
    }
}
