/*
 * Derived from Telegram Android Switch.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.StateSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.annotation.Keep
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Telegram Switch (Components/Switch.java) port, re-verified against the official source.
 *
 * Geometry (Switch.java onDraw): track 31x14dp, track radius 7dp, backplate circle r=10dp,
 * thumb circle r=8dp, thumb travel 17dp, x offset 7dp, 200ms "progress" animation.
 * Standalone cell bounds are 37x20dp (TextCheckCell.java:126).
 *
 * Colours as used by TextCheckCell.java:125 setColors(switchTrack, switchTrackChecked,
 * windowBackgroundWhite, windowBackgroundWhite):
 *   Day   (ThemeColors.java:115-116, 79): track 0xFFA6ADB3, checked 0xFF229AF0, thumb 0xFFFFFFFF
 *   Night (night.attheme:31, 366, 235):  track 0xFF606060, checked 0xFF459CE3, thumb 0xFF181819
 *   Press ripple (switchTrackBlueSelector / ...Checked, Switch.java setDrawRipple):
 *   Day 0x17404A53 / 0x21024781, Night 0x19B9DDFA / 0x328CD7FF; 18dp radius around the thumb.
 *
 * Not ported (not used by Eve): override-colour reveal bitmaps, icon drawables, drawIconType.
 * Text mode below is an Eve addition and is not part of the Telegram source.
 */
open class TelegramSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(view: TelegramSwitch, isChecked: Boolean)
    }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Int = (v * density).toInt()

    private val rectF = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var _progress: Float = 0f
    private var checkAnimator: ObjectAnimator? = null
    private var isCheckedState: Boolean = false
    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var textValue: CharSequence? = null

    private var drawRipple = false
    private var rippleDrawable: RippleDrawable? = null
    private var rippleColorSet = 0
    private val pressedState = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)

    private companion object {
        const val DAY_TRACK_OFF = 0xFFA6ADB3.toInt()      // ThemeColors.java:115 key_switchTrack
        const val DAY_TRACK_ON = 0xFF229AF0.toInt()       // ThemeColors.java:116 TELEGRAM_COLOR
        const val DAY_THUMB = 0xFFFFFFFF.toInt()          // ThemeColors.java:79 windowBackgroundWhite
        const val DAY_RIPPLE_OFF = 0x17404A53             // ThemeColors.java:121
        const val DAY_RIPPLE_ON = 0x21024781              // ThemeColors.java:122
        const val NIGHT_TRACK_OFF = 0xFF606060.toInt()    // night.attheme:31 switchTrack=-10461088
        const val NIGHT_TRACK_ON = 0xFF459CE3.toInt()     // night.attheme:366 switchTrackChecked=-12215069
        const val NIGHT_THUMB = 0xFF181819.toInt()        // night.attheme:235 windowBackgroundWhite=-15198183
        const val NIGHT_RIPPLE_OFF = 0x19B9DDFA           // night.attheme:341 switchTrackBlueSelector
        const val NIGHT_RIPPLE_ON = 0x328CD7FF            // night.attheme:337 switchTrackBlueSelectorChecked
    }

    var text: CharSequence?
        get() = textValue
        set(value) {
            if (textValue != value) {
                textValue = value
                requestLayout()
                invalidate()
            }
        }

    init {
        isFocusable = true
        isClickable = true
        isHapticFeedbackEnabled = true
        textPaint.textSize = dp(16f)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TelegramSwitch)
            val checked = a.getBoolean(R.styleable.TelegramSwitch_android_checked, false)
            textValue = a.getText(R.styleable.TelegramSwitch_android_text)
            a.recycle()
            setChecked(checked, animated = false)
        }
    }

    @Keep
    fun getProgress(): Float = _progress

    @Keep
    fun setProgress(value: Float) {
        if (_progress == value) return
        _progress = value
        invalidate()
    }

    override fun isChecked(): Boolean = isCheckedState

    override fun setChecked(checked: Boolean) {
        setChecked(checked, animated = isAttachedToWindow)
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        if (checked != isCheckedState) {
            isCheckedState = checked
            if (isAttachedToWindow && animated) {
                animateToCheckedState(checked)
            } else {
                cancelCheckAnimator()
                setProgress(if (checked) 1.0f else 0.0f)
            }
            onCheckedChangeListener?.onCheckedChanged(this, checked)
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        }
    }

    /** Binding/restoration must not be interpreted as a user's change. */
    fun setCheckedSilently(checked: Boolean, animated: Boolean = false) {
        val listener = onCheckedChangeListener
        onCheckedChangeListener = null
        try { setChecked(checked, animated) } finally { onCheckedChangeListener = listener }
    }

    override fun toggle() {
        setChecked(!isCheckedState)
    }

    override fun performClick(): Boolean {
        if (!isEnabled) return false
        super.performClick()
        toggle()
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
        return true
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        onCheckedChangeListener = listener
    }

    /** Switch.java setDrawRipple: pressed-state selector ripple, 18dp radius around the thumb. */
    fun setDrawRipple(value: Boolean) {
        if (Build.VERSION.SDK_INT < 23 || value == drawRipple) return
        drawRipple = value
        val ripple = rippleDrawable ?: RippleDrawable(
            ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(0)), null, null
        ).also {
            it.radius = dp(18)
            it.callback = this
            rippleDrawable = it
        }
        if ((isCheckedState && rippleColorSet != 2) || (!isCheckedState && rippleColorSet != 1)) {
            val dark = TelegramThemeEngine.isDarkActive(context)
            val color = if (isCheckedState) {
                if (dark) NIGHT_RIPPLE_ON else DAY_RIPPLE_ON
            } else {
                if (dark) NIGHT_RIPPLE_OFF else DAY_RIPPLE_OFF
            }
            ripple.setColor(ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(color)))
            rippleColorSet = if (isCheckedState) 2 else 1
        }
        if (Build.VERSION.SDK_INT >= 28 && value) {
            ripple.setHotspot(if (isCheckedState) 0f else dp(100f), dp(18f))
        }
        ripple.state = if (value) pressedState else StateSet.NOTHING
        invalidate()
    }

    override fun setPressed(pressed: Boolean) {
        setDrawRipple(pressed)
        super.setPressed(pressed)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || (rippleDrawable != null && who === rippleDrawable)
    }

    override fun onDetachedFromWindow() {
        cancelCheckAnimator()
        setProgress(if (isCheckedState) 1f else 0f)
        super.onDetachedFromWindow()
    }

    private fun cancelCheckAnimator() {
        checkAnimator?.cancel()
        checkAnimator = null
    }

    private fun animateToCheckedState(newCheckedState: Boolean) {
        cancelCheckAnimator()
        checkAnimator = ObjectAnimator.ofFloat(this, "progress", if (newCheckedState) 1f else 0f).apply {
            duration = 200
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val switchW = dp(37)
        val switchH = dp(20)

        val currentText = textValue
        if (!currentText.isNullOrEmpty()) {
            val minHeight = dp(48)
            val textWidth = textPaint.measureText(currentText.toString())
            val padding = dp(8)
            val totalW = (textWidth + switchW + padding + paddingLeft + paddingRight).toInt()
            val w = when (widthMode) {
                MeasureSpec.EXACTLY -> widthSize
                MeasureSpec.AT_MOST -> Math.min(totalW, widthSize)
                else -> totalW
            }
            val h = when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> Math.min(minHeight, heightSize)
                else -> minHeight
            }
            setMeasuredDimension(w, h)
        } else {
            val w = if (widthMode == MeasureSpec.EXACTLY) widthSize else switchW
            val h = if (heightMode == MeasureSpec.EXACTLY) heightSize else switchH
            setMeasuredDimension(w, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return

        val currentText = textValue
        val isDark = TelegramThemeEngine.isDarkActive(context)

        if (!currentText.isNullOrEmpty()) {
            textPaint.color = if (isDark) Color.WHITE else 0xFF1A1D21.toInt()
            val textX = paddingLeft.toFloat()
            val fontMetrics = textPaint.fontMetrics
            val textY = (measuredHeight - fontMetrics.top - fontMetrics.bottom) / 2f
            canvas.drawText(currentText.toString(), textX, textY, textPaint)
        }

        val trackOff = if (isDark) NIGHT_TRACK_OFF else DAY_TRACK_OFF
        val trackOn = if (isDark) NIGHT_TRACK_ON else DAY_TRACK_ON

        val red = (Color.red(trackOff) + (Color.red(trackOn) - Color.red(trackOff)) * _progress).toInt()
        val green = (Color.green(trackOff) + (Color.green(trackOn) - Color.green(trackOff)) * _progress).toInt()
        val blue = (Color.blue(trackOff) + (Color.blue(trackOn) - Color.blue(trackOff)) * _progress).toInt()
        val alpha = (Color.alpha(trackOff) + (Color.alpha(trackOn) - Color.alpha(trackOff)) * _progress).toInt()
        trackPaint.color = Color.argb(alpha, red, green, blue)

        // Switch.java onDraw: 31x14dp track, centred; thumb x = 7dp + 17dp * progress.
        val width = dp(31f)
        val height = dp(14f)
        val x = if (!currentText.isNullOrEmpty()) {
            measuredWidth - width - paddingRight.toFloat() - dp(3f)
        } else {
            (measuredWidth - width) / 2f
        }
        val y = (measuredHeight - height) / 2f
        val tx = x + dp(7f) + dp(17f) * _progress
        val ty = measuredHeight / 2f

        rectF.set(x, y, x + width, y + height)
        canvas.drawRoundRect(rectF, dp(7f), dp(7f), trackPaint)
        canvas.drawCircle(tx, ty, dp(10f), trackPaint)

        // Ripple is drawn between the track backplate and the thumb (Switch.java onDraw).
        rippleDrawable?.let {
            it.setBounds(
                (tx - dp(18f)).toInt(), (ty - dp(18f)).toInt(),
                (tx + dp(18f)).toInt(), (ty + dp(18f)).toInt()
            )
            it.draw(canvas)
        }

        // Thumb = windowBackgroundWhite for both states (white day, 0xFF181819 night).
        thumbPaint.color = if (isDark) NIGHT_THUMB else DAY_THUMB
        canvas.drawCircle(tx, ty, dp(8f), thumbPaint)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = isCheckedState
        if (!textValue.isNullOrEmpty()) {
            info.text = textValue
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.checked = isCheckedState
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            setCheckedSilently(state.checked)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var checked: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            checked = parcel.readByte().toInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeByte(if (checked) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}
