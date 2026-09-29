package com.eve.app.ui.result

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * Lightweight canvas confetti animation for high-performing test submissions.
 * Renders ~60 particles over 2000ms with gravity, rotation, and alpha fade.
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var rotation: Float,
        var vRot: Float,
        val color: Int,
        var alpha: Int = 255
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null

    private val colors = intArrayOf(
        Color.parseColor("#FFD700"), // Gold
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#FF5722"), // Orange
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#E91E63")  // Pink
    )

    init {
        isClickable = false
        isFocusable = false
        visibility = GONE
    }

    fun startConfetti() {
        animator?.cancel()
        particles.clear()
        visibility = VISIBLE

        post {
            val w = width.toFloat().coerceAtLeast(300f)
            val h = height.toFloat().coerceAtLeast(400f)

            for (i in 0 until 65) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * w,
                        y = -Random.nextFloat() * 100f,
                        vx = (Random.nextFloat() - 0.5f) * 6f,
                        vy = 4f + Random.nextFloat() * 8f,
                        size = 14f + Random.nextFloat() * 14f,
                        rotation = Random.nextFloat() * 360f,
                        vRot = (Random.nextFloat() - 0.5f) * 12f,
                        color = colors[Random.nextInt(colors.size)]
                    )
                )
            }

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                addUpdateListener { va ->
                    val progress = va.animatedFraction
                    val gravity = 0.25f

                    for (p in particles) {
                        p.x += p.vx
                        p.y += p.vy
                        p.vy += gravity
                        p.rotation += p.vRot
                        if (progress > 0.7f) {
                            val fadeProgress = (progress - 0.7f) / 0.3f
                            p.alpha = ((1f - fadeProgress) * 255).toInt().coerceIn(0, 255)
                        }
                    }
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        particles.clear()
                        visibility = GONE
                    }
                })
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty()) return

        for (p in particles) {
            paint.color = p.color
            paint.alpha = p.alpha
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.size / 2, -p.size / 4, p.size / 2, p.size / 4, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
