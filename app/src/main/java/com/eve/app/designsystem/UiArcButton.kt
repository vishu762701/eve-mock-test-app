package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.eve.app.R
import com.eve.app.util.FontManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * UIArc Button — Feedback Submission Component.
 * Faithfully implements the UIArc Button specification (https://uiarc.dev/components/button):
 * - Snappy spring press scale (~0.985 for wide, 0.97 standard) with spring release
 * - Integrated loading spinner replacing duplicate visual indicators
 * - Truthful pending label morph ("Send Feedback" -> "Sending…") with vertical shift and crossfade
 * - Duplicate click prevention while pending
 * - Accessible focus, talkback state, and reduced-motion support
 */
class UiArcButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var idleText: CharSequence = "Send Feedback"
    private var isBusy: Boolean = false
    private var activePressAnimator: ValueAnimator? = null

    private val spinner: CircularProgressIndicator
    private val labelContainer: FrameLayout
    private val currentLabel: AppCompatTextView
    private val outgoingLabel: AppCompatTextView

    var isLoading: Boolean
        get() = isBusy
        set(value) = setLoading(value)

    init {
        isClickable = true
        isFocusable = true
        clipChildren = false
        clipToPadding = false

        // Parse optional XML text attribute
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
            val textAttr = a.getText(0)
            if (!textAttr.isNullOrEmpty()) {
                idleText = textAttr
            }
            a.recycle()
        }

        // Apply canonical capsule shape and theme tokens
        val shape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Eve_Token_Capsule, 0).build()
        val surfaceDrawable = MaterialShapeDrawable(shape).apply {
            fillColor = ContextCompat.getColorStateList(context, R.color.design_button_fill)
        }
        val rippleColor = ContextCompat.getColorStateList(context, R.color.track) ?: ColorStateList.valueOf(Color.TRANSPARENT)
        val maskDrawable = MaterialShapeDrawable(shape)
        background = RippleDrawable(rippleColor, surfaceDrawable, maskDrawable)

        val minHeightPx = resources.getDimensionPixelSize(R.dimen.design_touch_min)
        minimumHeight = minHeightPx

        // Content row (centered)
        val contentRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            val pillPadding = resources.getDimensionPixelSize(R.dimen.design_pill_padding)
            setPadding(pillPadding, 0, pillPadding, 0)
        }

        // Integrated loading spinner
        val spinnerSize = (18f * density).toInt()
        val spinnerStroke = (2.5f * density).toInt()
        spinner = CircularProgressIndicator(context).apply {
            layoutParams = LinearLayout.LayoutParams(spinnerSize, spinnerSize).apply {
                marginEnd = (8f * density).toInt()
            }
            isIndeterminate = true
            indicatorSize = spinnerSize
            trackThickness = spinnerStroke
            setIndicatorColor(ContextCompat.getColor(context, R.color.design_button_text))
            visibility = View.GONE
            alpha = 0f
            scaleX = 0.6f
            scaleY = 0.6f
        }
        contentRow.addView(spinner)

        // Label Slot for in-place crossfade morph
        labelContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            clipChildren = false
            clipToPadding = false
        }

        val typeSize = resources.getDimension(R.dimen.design_type_button)
        val buttonTextColor = ContextCompat.getColor(context, R.color.design_button_text)

        currentLabel = AppCompatTextView(context).apply {
            id = android.R.id.text1
            text = idleText
            typeface = FontManager.typeface(context, true)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeSize)
            setTextColor(buttonTextColor)
            includeFontPadding = false
            isSingleLine = true
            gravity = Gravity.CENTER
        }
        labelContainer.addView(currentLabel)

        outgoingLabel = AppCompatTextView(context).apply {
            typeface = FontManager.typeface(context, true)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeSize)
            setTextColor(buttonTextColor)
            includeFontPadding = false
            isSingleLine = true
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        labelContainer.addView(outgoingLabel)

        contentRow.addView(labelContainer)

        val rowParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        addView(contentRow, rowParams)

        contentDescription = idleText
        TouchTargets.ensure(this)

        ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Button"
                info.isClickable = !isBusy && isEnabled
                if (isBusy) {
                    info.stateDescription = "Sending in progress"
                }
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        if (isBusy) {
            // Swallow touches while submission is pending to prevent duplicate requests
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                animatePress(isPressed = true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animatePress(isPressed = false)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animatePress(isPressed: Boolean) {
        if (!UiArcMotion.isMotionEnabled(context)) {
            scaleX = 1f
            scaleY = 1f
            return
        }

        activePressAnimator?.cancel()
        val targetScale = if (isPressed) UiArcMotion.buttonPressScale(width, density) else 1.0f
        val duration = if (isPressed) UiArcMotion.Duration.INSTANT else 360L
        val interpolator = if (isPressed) UiArcMotion.Ease.STANDARD else UiArcMotion.Spring.SNAPPY

        activePressAnimator = ValueAnimator.ofFloat(scaleX, targetScale).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                scaleX = v
                scaleY = v
            }
            start()
        }
    }

    /**
     * Sets button submission loading state with UIArc label morph and integrated spinner.
     */
    fun setLoading(loading: Boolean, pendingLabel: CharSequence = "Sending…") {
        if (isBusy == loading) return
        isBusy = loading

        val reduced = !UiArcMotion.isMotionEnabled(context)
        val incoming = if (loading) pendingLabel else idleText

        contentDescription = incoming

        if (reduced) {
            currentLabel.text = incoming
            outgoingLabel.visibility = View.GONE
            spinner.visibility = if (loading) View.VISIBLE else View.GONE
            spinner.alpha = if (loading) 1f else 0f
            spinner.scaleX = 1f
            spinner.scaleY = 1f
            return
        }

        // Animate integrated spinner
        if (loading) {
            spinner.visibility = View.VISIBLE
            spinner.animate().cancel()
            spinner.alpha = 0f
            spinner.scaleX = 0.6f
            spinner.scaleY = 0.6f
            spinner.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(UiArcMotion.Duration.FAST)
                .setInterpolator(UiArcMotion.Spring.SNAPPY)
                .start()
        } else {
            spinner.animate().cancel()
            spinner.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(UiArcMotion.Duration.FAST)
                .setInterpolator(UiArcMotion.Ease.STANDARD)
                .withEndAction { spinner.visibility = View.GONE }
                .start()
        }

        // Animate label crossfade & shift
        outgoingLabel.text = currentLabel.text
        outgoingLabel.visibility = View.VISIBLE
        outgoingLabel.alpha = 1f
        outgoingLabel.translationY = 0f
        outgoingLabel.animate().cancel()
        val shiftOutPx = -3f * density
        outgoingLabel.animate()
            .translationY(shiftOutPx)
            .alpha(0f)
            .setDuration(UiArcMotion.Duration.FAST)
            .setInterpolator(UiArcMotion.Ease.STANDARD)
            .withEndAction { outgoingLabel.visibility = View.GONE }
            .start()

        currentLabel.text = incoming
        currentLabel.alpha = 0f
        val shiftInPx = 4f * density
        currentLabel.translationY = shiftInPx
        currentLabel.animate().cancel()
        currentLabel.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(UiArcMotion.Duration.STANDARD)
            .setInterpolator(UiArcMotion.Ease.ENTER)
            .start()
    }

    fun setText(text: CharSequence) {
        idleText = text
        if (!isBusy) {
            currentLabel.text = text
            contentDescription = text
        }
    }

    fun getText(): CharSequence = if (isBusy) idleText else currentLabel.text

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.52f
    }
}
