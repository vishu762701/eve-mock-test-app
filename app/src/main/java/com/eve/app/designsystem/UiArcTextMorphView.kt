package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.roundToInt

/**
 * UIArc Text Morph Component.
 * Genuine native translation of official UIArc Text Morph specification:
 * - Registry: https://uiarc.dev/r/text-morph.json
 * - Character occurrence keying: "${char}-${occurrenceIndex}"
 * - Shared characters glide to new position with Spring.MORPH (550ms)
 * - Entering glyphs: scale 0.8 -> 1.0, Y: +0.14em -> 0, staggered fade-in (280ms, Ease.ENTER)
 * - Exiting glyphs: scale 1.0 -> 0.86, Y: 0 -> -0.10em, fade-out (180ms, Ease.STANDARD)
 * - Accessibility: announces complete text as a single coherent phrase
 * - Instant fallback when reduced motion is requested
 */
class UiArcTextMorphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val MORPH_DURATION_MS = 560L
        const val ENTER_DURATION_MS = 280L
        const val EXIT_DURATION_MS = 180L
        const val ENTER_BASE_DELAY_MS = 50L
        const val ENTER_STAGGER_MS = 32L
    }

    private data class GlyphLayout(
        val char: Char,
        val key: String,
        val x: Float,
        val width: Float
    )

    private class ActiveGlyph(
        val char: Char,
        val startX: Float,
        val targetX: Float,
        val isShared: Boolean,
        val isEntering: Boolean,
        val enterDelay: Long = 0L,
        var currentX: Float = 0f,
        var currentY: Float = 0f,
        var currentScale: Float = 1f,
        var currentAlpha: Float = 1f
    )

    private var currentTextString: String = ""
    private var isMorphing = false
    private var morphAnimator: ValueAnimator? = null
    private val activeGlyphs = mutableListOf<ActiveGlyph>()
    private val renderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    init {
        currentTextString = text?.toString() ?: ""
        contentDescription = currentTextString
    }

    fun morphToText(newText: CharSequence) {
        setText(newText, BufferType.NORMAL)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        val newTextStr = text?.toString() ?: ""

        // If not attached, same text, or motion disabled, bypass animation
        if (!isAttachedToWindow || newTextStr == currentTextString || currentTextString.isEmpty() || !UiArcMotion.isMotionEnabled(context)) {
            morphAnimator?.cancel()
            isMorphing = false
            activeGlyphs.clear()
            currentTextString = newTextStr
            super.setText(text, type ?: BufferType.NORMAL)
            contentDescription = newTextStr
            return
        }

        startMorph(currentTextString, newTextStr, type ?: BufferType.NORMAL)
    }

    private fun startMorph(oldStr: String, newStr: String, type: BufferType) {
        morphAnimator?.cancel()

        val textPaint = paint
        val emSize = textPaint.textSize

        val oldLayouts = computeGlyphLayouts(oldStr, textPaint)
        val newLayouts = computeGlyphLayouts(newStr, textPaint)

        val oldByKey = oldLayouts.associateBy { it.key }
        val newByKey = newLayouts.associateBy { it.key }

        activeGlyphs.clear()

        // 1. Shared characters & Entering characters
        var enterCount = 0
        for (newGlyph in newLayouts) {
            val oldGlyph = oldByKey[newGlyph.key]
            if (oldGlyph != null) {
                // Shared character
                activeGlyphs.add(
                    ActiveGlyph(
                        char = newGlyph.char,
                        startX = oldGlyph.x,
                        targetX = newGlyph.x,
                        isShared = true,
                        isEntering = false,
                        currentX = oldGlyph.x
                    )
                )
            } else {
                // Entering character
                val delay = ENTER_BASE_DELAY_MS + minOf(enterCount, 8) * ENTER_STAGGER_MS
                enterCount++
                activeGlyphs.add(
                    ActiveGlyph(
                        char = newGlyph.char,
                        startX = newGlyph.x,
                        targetX = newGlyph.x,
                        isShared = false,
                        isEntering = true,
                        enterDelay = delay,
                        currentX = newGlyph.x,
                        currentY = 0.14f * emSize,
                        currentScale = 0.80f,
                        currentAlpha = 0f
                    )
                )
            }
        }

        // 2. Exiting characters
        for (oldGlyph in oldLayouts) {
            if (!newByKey.containsKey(oldGlyph.key)) {
                activeGlyphs.add(
                    ActiveGlyph(
                        char = oldGlyph.char,
                        startX = oldGlyph.x,
                        targetX = oldGlyph.x,
                        isShared = false,
                        isEntering = false,
                        currentX = oldGlyph.x,
                        currentY = 0f,
                        currentScale = 1.0f,
                        currentAlpha = 1.0f
                    )
                )
            }
        }

        isMorphing = true
        currentTextString = newStr
        contentDescription = newStr

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = MORPH_DURATION_MS
            addUpdateListener { animator ->
                val elapsedMs = (animator.animatedFraction * MORPH_DURATION_MS).toLong()
                updateGlyphTransforms(elapsedMs, emSize)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isMorphing = false
                    activeGlyphs.clear()
                    super@UiArcTextMorphView.setText(newStr, type)
                    invalidate()
                    morphAnimator = null
                }
            })
        }
        morphAnimator = anim
        anim.start()
    }

    private fun updateGlyphTransforms(elapsedMs: Long, emSize: Float) {
        val morphProgress = UiArcMotion.Spring.MORPH.getInterpolation((elapsedMs / 550f).coerceIn(0f, 1f))

        for (g in activeGlyphs) {
            when {
                g.isShared -> {
                    g.currentX = g.startX + (g.targetX - g.startX) * morphProgress
                    g.currentY = 0f
                    g.currentScale = 1f
                    g.currentAlpha = 1f
                }
                g.isEntering -> {
                    val glyphElapsed = (elapsedMs - g.enterDelay).coerceAtLeast(0L)
                    val enterProgress = UiArcMotion.Ease.ENTER.getInterpolation(
                        (glyphElapsed / ENTER_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                    )
                    g.currentX = g.targetX
                    g.currentY = (1f - enterProgress) * (0.14f * emSize)
                    g.currentScale = 0.80f + (0.20f * enterProgress)
                    g.currentAlpha = enterProgress
                }
                else -> {
                    // Exiting glyph
                    val exitProgress = UiArcMotion.Ease.STANDARD.getInterpolation(
                        (elapsedMs / EXIT_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                    )
                    g.currentX = g.startX
                    g.currentY = -exitProgress * (0.10f * emSize)
                    g.currentScale = 1.0f - (0.14f * exitProgress)
                    g.currentAlpha = (1f - exitProgress).coerceAtLeast(0f)
                }
            }
        }
    }

    private fun computeGlyphLayouts(str: String, paint: Paint): List<GlyphLayout> {
        val layouts = mutableListOf<GlyphLayout>()
        val counts = mutableMapOf<Char, Int>()
        var curX = 0f

        for (i in str.indices) {
            val c = str[i]
            val occurrence = counts[c] ?: 0
            counts[c] = occurrence + 1
            val key = "$c-$occurrence"
            val charW = paint.measureText(str, i, i + 1)
            layouts.add(GlyphLayout(c, key, curX, charW))
            curX += charW
        }
        return layouts
    }

    override fun onDraw(canvas: Canvas) {
        if (!isMorphing || activeGlyphs.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        renderPaint.set(paint)
        val textPaint = renderPaint

        val availableH = height - paddingTop - paddingBottom
        val baseline = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.CENTER_VERTICAL -> paddingTop + availableH / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
            Gravity.BOTTOM -> height - paddingBottom + textPaint.descent()
            else -> paddingTop - textPaint.ascent()
        }

        val startX = paddingLeft.toFloat()

        for (glyph in activeGlyphs) {
            if (glyph.currentAlpha <= 0f) continue

            canvas.save()
            val glyphCenterX = startX + glyph.currentX
            canvas.translate(glyphCenterX, baseline + glyph.currentY)
            if (glyph.currentScale != 1f) {
                canvas.scale(glyph.currentScale, glyph.currentScale, 0f, 0f)
            }
            textPaint.alpha = (255 * glyph.currentAlpha.coerceIn(0f, 1f)).roundToInt()
            canvas.drawText(glyph.char.toString(), 0f, 0f, textPaint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        morphAnimator?.cancel()
        isMorphing = false
        activeGlyphs.clear()
    }
}
