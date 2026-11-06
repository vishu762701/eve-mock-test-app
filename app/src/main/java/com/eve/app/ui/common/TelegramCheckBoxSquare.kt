/*
 * Derived from Telegram Android CheckBoxSquare.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.common

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Authentic Telegram square checkbox component.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBoxSquare.java:24-193
 * - Box geometry: 18x18dp (CheckBoxSquare.java:62)
 * - Corner radii: 4dp outer roundRect, 3dp inner clear cutout (CheckBoxSquare.java:168, 173)
 * - Checkmark vertex: (7dp, 13dp) with 3dp left wing and 7dp right wing (CheckBoxSquare.java:180-186)
 * - Check stroke: 2dp round cap / stroke join (Theme.java:checkboxSquare_checkPaint)
 * - Motion: 300ms ObjectAnimator with bounce progress curve (CheckBoxSquare.java:96, 150-158)
 */
class TelegramCheckBoxSquare @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(view: TelegramCheckBoxSquare, isChecked: Boolean)
    }

    private val density: Float = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    private val rectF = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private var drawBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null

    private var _progress: Float = 0f
    private var checkAnimator: ObjectAnimator? = null
    private var attachedToWindow = false
    private var isCheckedState = false
    private var isDisabled = false
    private var isAlert = false

    var colorUnchecked: Int = ContextCompat.getColor(context, R.color.text_secondary)
    var colorBackground: Int = ContextCompat.getColor(context, R.color.accent)
    var colorCheck: Int = ContextCompat.getColor(context, R.color.on_accent)
    var colorDisabled: Int = ContextCompat.getColor(context, R.color.text_secondary)

    var onCheckedChangeListener: OnCheckedChangeListener? = null

    init {
        val side = dp(18).coerceAtLeast(1)
        drawBitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(drawBitmap!!)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TelegramCheckBoxSquare)
            val checked = a.getBoolean(R.styleable.TelegramCheckBoxSquare_android_checked, false)
            a.recycle()
            setChecked(checked, animated = false)
        }
        updatePalette()
    }

    private fun motionEnabled(): Boolean =
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f && (android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled())
        } catch (_: Exception) {
            true
        }

    fun setColors(unchecked: Int, checked: Int, check: Int) {
        colorUnchecked = unchecked
        colorBackground = checked
        colorCheck = check
        invalidate()
    }

    fun setDisabled(disabled: Boolean) {
        if (isDisabled != disabled) {
            isDisabled = disabled
            invalidate()
        }
    }

    @Keep
    fun setProgress(value: Float) {
        if (_progress == value) return
        _progress = value
        invalidate()
    }

    @Keep
    fun getProgress(): Float = _progress

    private fun cancelCheckAnimator() {
        checkAnimator?.cancel()
        checkAnimator = null
    }

    private fun animateToCheckedState(newCheckedState: Boolean) {
        cancelCheckAnimator()
        val target = if (newCheckedState) 1f else 0f
        checkAnimator = ObjectAnimator.ofFloat(this, "progress", target).apply {
            duration = 300
            start()
        }
    }

    override fun isChecked(): Boolean = isCheckedState

    override fun setChecked(checked: Boolean) {
        setChecked(checked, animated = motionEnabled())
    }

    fun setChecked(checked: Boolean, animated: Boolean) {
        if (checked == isCheckedState) return
        isCheckedState = checked
        if (attachedToWindow && animated && motionEnabled()) {
            animateToCheckedState(checked)
        } else {
            cancelCheckAnimator()
            setProgress(if (checked) 1f else 0f)
        }
        onCheckedChangeListener?.onCheckedChanged(this, checked)
    }

    override fun toggle() {
        if (isEnabled && !isDisabled) {
            setChecked(!isCheckedState)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        updatePalette()
    }

    override fun onDetachedFromWindow() {
        cancelCheckAnimator()
        attachedToWindow = false
        super.onDetachedFromWindow()
    }

    fun updatePalette() {
        try {
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            colorBackground = palette.accent
            colorUnchecked = palette.textSecondary
            colorCheck = palette.onAccent
            invalidate()
        } catch (_: Exception) {
            // Context without theme engine
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = dp(18)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (wMode) {
            MeasureSpec.EXACTLY -> wSize
            MeasureSpec.AT_MOST -> minOf(desired, wSize)
            else -> desired
        }
        val height = when (hMode) {
            MeasureSpec.EXACTLY -> hSize
            MeasureSpec.AT_MOST -> minOf(desired, hSize)
            else -> desired
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (visibility != VISIBLE) return
        val bitmap = drawBitmap ?: return
        val bitmapCanvas = drawCanvas ?: return

        val checkProgress: Float
        val bounceProgress: Float
        val uncheckedColor = colorUnchecked
        val color = colorBackground

        if (_progress <= 0.5f) {
            bounceProgress = _progress / 0.5f
            checkProgress = bounceProgress
            val rD = ((Color.red(color) - Color.red(uncheckedColor)) * checkProgress).toInt()
            val gD = ((Color.green(color) - Color.green(uncheckedColor)) * checkProgress).toInt()
            val bD = ((Color.blue(color) - Color.blue(uncheckedColor)) * checkProgress).toInt()
            val c = Color.rgb(
                Color.red(uncheckedColor) + rD,
                Color.green(uncheckedColor) + gD,
                Color.blue(uncheckedColor) + bD
            )
            backgroundPaint.color = c
        } else {
            bounceProgress = 2.0f - _progress / 0.5f
            checkProgress = 1.0f
            backgroundPaint.color = color
        }

        if (isDisabled) {
            backgroundPaint.color = colorDisabled
        }

        val bounce = dp(1f) * bounceProgress
        rectF.set(bounce, bounce, dp(18f) - bounce, dp(18f) - bounce)

        bitmap.eraseColor(0)
        bitmapCanvas.drawRoundRect(rectF, dp(4f), dp(4f), backgroundPaint)

        if (checkProgress != 1.0f) {
            val rad = minOf(dp(7f), dp(7f) * checkProgress + bounce)
            rectF.set(dp(1.33f) + rad, dp(1.33f) + rad, dp(16.66f) - rad, dp(16.66f) - rad)
            bitmapCanvas.drawRoundRect(rectF, dp(3f), dp(3f), eraserPaint)
        }

        if (_progress > 0.5f) {
            checkPaint.color = colorCheck

            val endX1 = dp(7f) - dp(3f) * (1.0f - bounceProgress)
            val endY1 = dp(13f) - dp(3f) * (1.0f - bounceProgress)
            bitmapCanvas.drawLine(dp(7f), dp(13f), endX1, endY1, checkPaint)

            val endX2 = dp(7f) + dp(7f) * (1.0f - bounceProgress)
            val endY2 = dp(13f) - dp(7f) * (1.0f - bounceProgress)
            bitmapCanvas.drawLine(dp(7f), dp(13f), endX2, endY2, checkPaint)
        }

        val left = (width - bitmap.width) / 2f
        val top = (height - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, null)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.CheckBox"
        info.isCheckable = true
        info.isChecked = isCheckedState
        info.isEnabled = isEnabled && !isDisabled
    }
}
