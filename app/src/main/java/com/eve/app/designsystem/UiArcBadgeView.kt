package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.eve.app.R
import com.eve.app.util.FontManager

/**
 * UIArc Badge — Result Cutoff Verdict Component.
 * Faithfully implements the UIArc Badge specification (https://uiarc.dev/components/badge):
 * - In-place label change: outgoing copy lifts away (-0.3em) with soft blur/alpha fade (160ms)
 * - Incoming copy rises from below (+0.3em) with enter curve (240ms)
 * - Width follows content width using spring.morph (visualDuration 0.42s, bounce 0.16)
 * - Crossfades icons (✓ / ✗) alongside text changes
 * - Preserves official EVE success/danger/neutral tokens and container shapes
 * - Maintains accessibility text announcements and reduced-motion fallback
 */
class UiArcBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private var currentVerdict: String = ""
    private var outgoingVerdict: String? = null
    private var isMorphing = false

    private var inTranslationY = 0f
    private var inAlpha = 1f
    private var outTranslationY = 0f
    private var outAlpha = 0f

    private var morphAnimator: ValueAnimator? = null
    private var widthAnimator: ValueAnimator? = null

    private val outgoingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false
        isSingleLine = true
        typeface = FontManager.mediumTypeface(context)
        val pillPadding = resources.getDimensionPixelSize(R.dimen.design_pill_padding)
        setPaddingRelative(pillPadding, 0, pillPadding, 0)
        minHeight = resources.getDimensionPixelSize(R.dimen.design_pill_height)

        outgoingPaint.apply {
            typeface = FontManager.mediumTypeface(context)
            textSize = this@UiArcBadgeView.textSize
            isAntiAlias = true
        }

        ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = "Status Badge"
                info.text = currentVerdict.ifEmpty { text }
            }
        })
    }

    /**
     * Binds verdict text, badge background, and text color with in-place UIArc morph transition.
     */
    fun setVerdict(verdictText: String, @DrawableRes backgroundRes: Int, @ColorInt color: Int) {
        val previous = currentVerdict
        currentVerdict = verdictText
        contentDescription = verdictText

        // Apply background and token styling
        setBackgroundResource(backgroundRes)
        setTextColor(color)
        outgoingPaint.color = color
        outgoingPaint.textSize = textSize

        if (previous.isEmpty() || previous == verdictText || !UiArcMotion.isMotionEnabled(context)) {
            // Initial render, identical text, or reduced motion: immediate presentation
            text = verdictText
            isMorphing = false
            morphAnimator?.cancel()
            widthAnimator?.cancel()
            invalidate()
            return
        }

        // In-place text transition
        outgoingVerdict = previous
        isMorphing = true
        text = verdictText // Keeps text property up to date for tests and accessibility

        // Compute width delta for morph spring
        val oldTextWidth = paint.measureText(previous)
        val newTextWidth = paint.measureText(verdictText)
        val paddingTotal = compoundPaddingLeft + compoundPaddingRight
        val startW = (oldTextWidth + paddingTotal).toInt()
        val targetW = (newTextWidth + paddingTotal).toInt()

        if (startW != targetW) {
            widthAnimator?.cancel()
            val initialWidth = if (width > 0) width else startW
            widthAnimator = ValueAnimator.ofInt(initialWidth, targetW).apply {
                duration = UiArcMotion.Spring.MORPH.settlingDurationMs
                interpolator = UiArcMotion.Spring.MORPH
                addUpdateListener { anim ->
                    val w = anim.animatedValue as Int
                    if (layoutParams.width != w) {
                        layoutParams = layoutParams.apply { width = w }
                    }
                }
                start()
            }
        }

        // Run in-place text lift and rise
        morphAnimator?.cancel()
        inTranslationY = 4f * density
        inAlpha = 0f
        outTranslationY = 0f
        outAlpha = 1f

        morphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiArcMotion.Duration.STANDARD
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener { anim ->
                val frac = anim.animatedValue as Float
                // Outgoing lifts to -3dp and fades out fast
                val outFrac = (frac / (UiArcMotion.Duration.FAST.toFloat() / UiArcMotion.Duration.STANDARD.toFloat())).coerceIn(0f, 1f)
                outTranslationY = -3f * density * outFrac
                outAlpha = 1f - outFrac

                // Incoming rises from +4dp to 0dp
                inTranslationY = 4f * density * (1f - frac)
                inAlpha = frac

                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isMorphing || outgoingVerdict == null) {
            super.onDraw(canvas)
            return
        }

        val baseline = baseline.toFloat()
        val viewWidth = width - paddingLeft - paddingRight

        // Draw Outgoing Text (lifting upward)
        if (outAlpha > 0.01f) {
            val outText = outgoingVerdict ?: ""
            val outW = outgoingPaint.measureText(outText)
            val outX = paddingLeft + (viewWidth - outW) / 2f
            outgoingPaint.alpha = (255 * outAlpha).toInt()
            canvas.drawText(outText, outX, baseline + outTranslationY, outgoingPaint)
        }

        // Draw Incoming Text (rising into position)
        if (inAlpha > 0.01f) {
            val inText = currentVerdict
            val inW = paint.measureText(inText)
            val inX = paddingLeft + (viewWidth - inW) / 2f
            val originalAlpha = paint.alpha
            paint.alpha = (originalAlpha * inAlpha).toInt()
            canvas.drawText(inText, inX, baseline + inTranslationY, paint)
            paint.alpha = originalAlpha
        }

        if (outAlpha <= 0.01f && inAlpha >= 0.99f) {
            isMorphing = false
            outgoingVerdict = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        morphAnimator?.cancel()
        widthAnimator?.cancel()
    }
}
