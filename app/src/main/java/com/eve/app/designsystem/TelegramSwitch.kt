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
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.annotation.Keep
import com.eve.app.theme.TelegramThemeEngine

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

    private var _progress: Float = 0f
    private var checkAnimator: ObjectAnimator? = null
    private var isCheckedState: Boolean = false
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    // Colors matching Telegram Android ThemeColors.java and night.attheme
    private val DAY_TRACK_OFF = 0xFFA6ADB3.toInt()        // Theme.key_switchTrack
    private val DAY_TRACK_ON = 0xFF229AF0.toInt()         // Theme.key_switchTrackChecked (TELEGRAM_COLOR)
    private val NIGHT_TRACK_OFF = 0xFF606060.toInt()      // night.attheme:31 switchTrack=-10461088
    private val NIGHT_TRACK_ON = 0xFF4592E3.toInt()       // night.attheme:366 switchTrackChecked=-12215069
    private val THUMB_COLOR = 0xFFFFFFFF.toInt()          // Theme.key_windowBackgroundWhite

    init {
        isFocusable = true
        isClickable = true
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.checked))
            val checked = a.getBoolean(0, false)
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
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
        }
    }

    override fun toggle() {
        setChecked(!isCheckedState)
    }

    override fun performClick(): Boolean {
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

    fun setOnCheckedChangeListener(listener: (TelegramSwitch, Boolean) -> Unit) {
        onCheckedChangeListener = OnCheckedChangeListener { view, checked -> listener(view, checked) }
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
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val defaultWidth = dp(37)
        val defaultHeight = dp(24)

        val w = if (widthMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(widthMeasureSpec) else defaultWidth
        val h = if (heightMode == MeasureSpec.EXACTLY) MeasureSpec.getSize(heightMeasureSpec) else defaultHeight
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return

        val isDark = TelegramThemeEngine.isDarkActive(context)
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
        val x = (measuredWidth - width) / 2f
        val y = (measuredHeight - dp(14f)) / 2f
        val tx = x + dp(7f) + dp(17f) * _progress
        val ty = measuredHeight / 2f

        rectF.set(x, y, x + width, y + dp(14f))
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
    }
}
