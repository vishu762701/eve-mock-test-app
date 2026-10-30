/*
 * Derived from Telegram Android TextCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram TextCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCell.java:90-150
 * - Height: 50dp
 * - Image offset: 16dp start, 24dp size
 * - Text offset: 58dp start
 * - Title: 16sp
 * - Subtitle: 13sp
 * - Value: 16sp right-aligned
 * - Divider: hairline 1px at bottom with 58dp start margin
 * - Counter badge pill (Telegram unread counter style)
 */
class TextCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val iconView: ImageView = ImageView(context)
    private val lottieView: LottieAnimationView = LottieAnimationView(context)
    private val textLayout: LinearLayout = LinearLayout(context)
    val textView: TextView = TextView(context)
    val subtitleView: TextView = TextView(context)
    val valueTextView: TextView = TextView(context)
    val badgeTextView: TextView = TextView(context)
    val arrowView: ImageView = ImageView(context)

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var needDivider: Boolean = true
        set(value) {
            field = value
            setWillNotDraw(!value)
            invalidate()
        }

    var offsetFromImage: Int = dp(58f)

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        dividerPaint.color = palette.hairline
        dividerPaint.strokeWidth = dp(0.75f).coerceAtLeast(1).toFloat()
        setWillNotDraw(false)

        // Ripple feedback
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        isFocusable = true
        isClickable = true
        minimumHeight = dp(50f)

        // 1. Static Icon
        iconView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        iconView.visibility = View.GONE
        addView(iconView, LayoutParams(dp(24f), dp(24f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(16f)
        })

        // 2. Animated Lottie Icon
        lottieView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        lottieView.visibility = View.GONE
        addView(lottieView, LayoutParams(dp(24f), dp(24f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(16f)
        })

        // 3. Text Column (Title + Subtitle)
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.gravity = Gravity.CENTER_VERTICAL

        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(palette.textPrimary)
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.maxLines = 1
        textLayout.addView(textView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        subtitleView.setTextColor(palette.textSecondary)
        subtitleView.ellipsize = TextUtils.TruncateAt.END
        subtitleView.maxLines = 1
        subtitleView.visibility = View.GONE
        textLayout.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val textLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = offsetFromImage
            marginEnd = dp(80f)
        }
        addView(textLayout, textLp)

        // 4. Value Text on right
        valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        valueTextView.setTextColor(palette.textSecondary)
        valueTextView.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        valueTextView.maxLines = 1
        valueTextView.visibility = View.GONE
        addView(valueTextView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(38f)
        })

        // 5. Unread counter badge pill
        badgeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        badgeTextView.setTextColor(0xFFFFFFFF.toInt())
        badgeTextView.gravity = Gravity.CENTER
        badgeTextView.includeFontPadding = false
        badgeTextView.setPadding(dp(7f), dp(2f), dp(7f), dp(2f))
        val badgeBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(11f).toFloat()
            setColor(palette.accent)
        }
        badgeTextView.background = badgeBg
        badgeTextView.visibility = View.GONE
        addView(badgeTextView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(38f)
        })

        // 6. Right Chevron Arrow
        arrowView.setImageResource(R.drawable.ic_chevron_right)
        arrowView.scaleType = ImageView.ScaleType.CENTER
        arrowView.setColorFilter(palette.textSecondary)
        addView(arrowView, LayoutParams(dp(16f), dp(16f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(16f)
        })
    }

    fun setTextAndIcon(text: CharSequence, iconResId: Int, needDivider: Boolean = true) {
        textView.text = text
        this.needDivider = needDivider
        iconView.visibility = View.VISIBLE
        iconView.setImageResource(iconResId)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        iconView.setColorFilter(palette.textSecondary)
        lottieView.visibility = View.GONE
        updateOffsets(hasIcon = true)
    }

    fun setTextAndLottie(text: CharSequence, rawLottieResId: Int, needDivider: Boolean = true) {
        textView.text = text
        this.needDivider = needDivider
        iconView.visibility = View.GONE
        lottieView.visibility = View.VISIBLE
        lottieView.setAnimation(rawLottieResId)
        lottieView.repeatCount = 0
        lottieView.playAnimation()
        updateOffsets(hasIcon = true)
    }

    fun setText(text: CharSequence, needDivider: Boolean = true) {
        textView.text = text
        this.needDivider = needDivider
        iconView.visibility = View.GONE
        lottieView.visibility = View.GONE
        updateOffsets(hasIcon = false)
    }

    fun setSubtitle(subtitle: CharSequence?) {
        if (!subtitle.isNullOrBlank()) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        } else {
            subtitleView.visibility = View.GONE
        }
    }

    fun setValue(value: CharSequence?) {
        if (!value.isNullOrBlank()) {
            valueTextView.text = value
            valueTextView.visibility = View.VISIBLE
            badgeTextView.visibility = View.GONE
        } else {
            valueTextView.visibility = View.GONE
        }
    }

    fun setBadge(count: Int) {
        if (count > 0) {
            badgeTextView.text = if (count > 99) "99+" else count.toString()
            badgeTextView.visibility = View.VISIBLE
            valueTextView.visibility = View.GONE
        } else {
            badgeTextView.visibility = View.GONE
        }
    }

    fun setShowArrow(show: Boolean) {
        arrowView.visibility = if (show) View.VISIBLE else View.GONE
        val margin = if (show) dp(38f) else dp(16f)
        (valueTextView.layoutParams as? LayoutParams)?.marginEnd = margin
        (badgeTextView.layoutParams as? LayoutParams)?.marginEnd = margin
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    fun setIconColor(color: Int) {
        iconView.setColorFilter(color)
    }

    fun makeRed() {
        val red = context.getColor(R.color.system_red)
        textView.setTextColor(red)
        iconView.setColorFilter(red)
        arrowView.visibility = View.GONE
    }

    private fun updateOffsets(hasIcon: Boolean) {
        offsetFromImage = if (hasIcon) dp(58f) else dp(20f)
        (textLayout.layoutParams as? LayoutParams)?.marginStart = offsetFromImage
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (needDivider) {
            val start = offsetFromImage.toFloat()
            val y = height - dividerPaint.strokeWidth
            canvas.drawLine(start, y, width.toFloat(), y, dividerPaint)
        }
    }
}
