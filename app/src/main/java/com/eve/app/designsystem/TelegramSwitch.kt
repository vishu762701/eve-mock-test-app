/*
 * Derived from Telegram Android Switch.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.annotation.Keep
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram Switch component ported for EVE Mission 01.
 * Geometry ported from Switch.java:380-384, 448-450, 497:
 *   - Track width: 31dp (Switch.java:380)
 *   - Track height: 14dp (Switch.java:383, 448)
 *   - Thumb backplate radius: 10dp (diameter 20dp) (Switch.java:381, 450)
 *   - Thumb inner radius: 8dp (Switch.java:497)
 *   - Thumb travel distance: 17dp (Switch.java:384)
 *   - Animation: 200ms ObjectAnimator on "progress" (Switch.java:237-238)
 *   - Standalone cell layout bounds: 37x20dp (TextCheckCell.java:126)
 * Colors:
 *   - Day track off: 0xFFA6ADB3 (ThemeColors.java:115 key_switchTrack)
 *   - Day track on:  0xFF229AF0 (ThemeColors.java:116 TELEGRAM_COLOR)
 *   - Night track off: 0xFF606060 (night.attheme:31 switchTrack=-10461088)
 *   - Night track on:  0xFF459CE3 (night.attheme:366 switchTrackChecked=-12215069)
 *   - Thumb: 0xFFFFFFFF (Theme.key_windowBackgroundWhite)
 */
class TelegramSwitch @JvmOverloads constructor(
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

    // Colors matching Telegram Android ThemeColors.java and night.attheme
    private val DAY_TRACK_OFF = 0xFFA6ADB3.toInt()        // ThemeColors.java:115 key_switchTrack
    private val DAY_TRACK_ON = 0xFF229AF0.toInt()         // ThemeColors.java:116 key_switchTrackChecked (TELEGRAM_COLOR)
    private val NIGHT_TRACK_OFF = 0xFF606060.toInt()      // night.attheme:31 switchTrack=-10461088
    private val NIGHT_TRACK_ON = 0xFF459CE3.toInt()       // night.attheme:366 switchTrackChecked=-12215069
    private val THUMB_COLOR = 0xFFFFFFFF.toInt()          // Theme.key_windowBackgroundWhite

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
        playSoundEffect(SoundEffectConstants.CLICK)
        return true
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        onCheckedChangeListener = listener
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

        // Interpolate track color
        val r1 = Color.red(trackOff)
        val r2 = Color.red(trackOn)
        val g1 = Color.green(trackOff)
        val g2 = Color.green(trackOn)
        val b1 = Color.blue(trackOff)
        val b2 = Color.blue(trackOn)
        val a1 = Color.alpha(trackOff)
        val a2 = Color.alpha(trackOn)

        val red = (r1 + (r2 - r1) * _progress).toInt()
        val green = (g1 + (g2 - g1) * _progress).toInt()
        val blue = (b1 + (b2 - b1) * _progress).toInt()
        val alpha = (a1 + (a2 - a1) * _progress).toInt()
        val currentTrackColor = Color.argb(alpha, red, green, blue)

        trackPaint.color = currentTrackColor

        // Geometry verbatim from Telegram Switch.java:380-450
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

        // Draw thumb
        thumbPaint.color = THUMB_COLOR
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
