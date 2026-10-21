package com.eve.app.designsystem

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.widget.AppCompatTextView
import com.eve.app.R

/**
 * UIArc In-View Title Component.
 * Genuine native translation of official UIArc In-View Title specification:
 * - Source: https://uiarc.dev/r/in-view-title.json
 * - Scroll-triggered section title reveal when 45% of heading is in viewport
 * - Coordinated reveal timing:
 *   - Opacity leads: 448ms (settle * 0.56, Ease.STANDARD)
 *   - Blur clears next: 624ms (settle * 0.78, API 31+ RenderEffect)
 *   - Transform settles last: 800ms (settle, Ease.ENTER) with subtle vertical rise
 * - Single trigger (once = true), never flickering on minor scroll movements
 * - Headings already scrolled past show immediately
 * - Full TalkBack accessibility and reduced-motion support
 */
class UiArcInViewTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val TRIGGER_AMOUNT = 0.45f
        const val SETTLE_MS = 800L
        const val OPACITY_MS = 448L // 800 * 0.56
        const val BLUR_MS = 624L    // 800 * 0.78
    }

    var inViewThreshold: Float = TRIGGER_AMOUNT
    var once: Boolean = true
    private var isRevealed = false
    private var hasEverBeenOutOfView = false

    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    init {
        // Initial state before reveal: visible by default so accessibility, TalkBack, and snapshots have full contrast
        alpha = 1f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!UiArcMotion.isMotionEnabled(context)) {
            revealInstant()
            return
        }

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            checkVisibilityAndTrigger()
        }
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        scrollListener = ViewTreeObserver.OnScrollChangedListener {
            checkVisibilityAndTrigger()
        }
        viewTreeObserver.addOnScrollChangedListener(scrollListener)

        // Post check on view attached
        post { checkVisibilityAndTrigger() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scrollListener?.let {
            if (viewTreeObserver.isAlive) viewTreeObserver.removeOnScrollChangedListener(it)
        }
        globalLayoutListener?.let {
            if (viewTreeObserver.isAlive) viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        scrollListener = null
        globalLayoutListener = null
    }

    fun checkVisibilityAndTrigger() {
        if (isRevealed && once) return

        val rect = Rect()
        val isVisible = getGlobalVisibleRect(rect)
        val h = height

        if (isVisible && h > 0) {
            val visibleFraction = rect.height().toFloat() / h.toFloat()
            if (visibleFraction >= inViewThreshold) {
                if (!hasEverBeenOutOfView) {
                    revealInstant()
                } else {
                    triggerReveal()
                }
            }
        } else {
            hasEverBeenOutOfView = true
            // Check if already above viewport
            val location = IntArray(2)
            getLocationOnScreen(location)
            if (location[1] < 0 && h > 0) {
                revealInstant()
            } else if (location[1] > 0 && h > 0) {
                alpha = 0f
            }
        }
    }

    fun revealInstant() {
        isRevealed = true
        alpha = 1f
        translationY = 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(null)
        }
    }

    fun triggerReveal() {
        if (isRevealed && once) return
        isRevealed = true

        if (!UiArcMotion.isMotionEnabled(context)) {
            revealInstant()
            return
        }

        val startOffset = resources.displayMetrics.density * 6f // ~0.2em subtle rise
        translationY = startOffset
        alpha = 0f

        // 1. Opacity leads (448ms, Ease.STANDARD)
        val alphaAnim = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).apply {
            duration = OPACITY_MS
            interpolator = UiArcMotion.Ease.STANDARD
        }

        // 2. Transform settles last (800ms, Ease.ENTER)
        val transAnim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, startOffset, 0f).apply {
            duration = SETTLE_MS
            interpolator = UiArcMotion.Ease.ENTER
        }

        // 3. Blur clears sooner (624ms) on Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurStart = UiArcMotion.Blur.TEXT_PX
            val blurAnim = ValueAnimator.ofFloat(blurStart, 0f).apply {
                duration = BLUR_MS
                interpolator = UiArcMotion.Ease.STANDARD
                addUpdateListener { va ->
                    val r = va.animatedValue as Float
                    if (r > 0.5f) {
                        setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP))
                    } else {
                        setRenderEffect(null)
                    }
                }
            }
            AnimatorSet().apply {
                playTogether(alphaAnim, transAnim, blurAnim)
                start()
            }
        } else {
            AnimatorSet().apply {
                playTogether(alphaAnim, transAnim)
                start()
            }
        }
    }
}
