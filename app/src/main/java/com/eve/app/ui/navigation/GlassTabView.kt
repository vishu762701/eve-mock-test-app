package com.eve.app.ui.navigation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.theme.TelegramThemeEngine
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * Single tab item in the Telegram-style floating bottom navigation pill.
 * Direct adaptation of Telegram's org.telegram.ui.Components.glass.GlassTabView.
 * Preserves decelerate selection curve, rounded capsule selector math, and alpha/color blending.
 */
class GlassTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface TabListener {
        fun onTabSelected(tab: GlassTabView)
        fun onTabReselected(tab: GlassTabView)
    }

    val textView: TextView
    val iconView: ImageView
    val lottieView: LottieAnimationView
    val avatarView: ShapeableImageView

    private val paintSelector = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tmpRectF = RectF()
    private val defaultTextPaint: TextPaint

    private var selectedFactor = 0f
    private var isTabSelected = false
    private var isSelectedAnimator: ValueAnimator? = null

    private var colorSelected: Int = 0xFF1A91E6.toInt()
    private var colorSelectedText: Int = 0xFF0D7FCF.toInt()
    private var colorDefault: Int = 0xFF1A1D21.toInt()

    private var hasVisualWidth = false
    private var visualWidth = 0f
    var attachScale = 1f
    private val decelerateInterpolator = DecelerateInterpolator()

    var isAvatarTab = false
        private set
    var isLottieTab = false
        private set

    init {
        setWillNotDraw(false)

        // Standard icon view (24x24dp, top-centered)
        iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val iconSize = dp(24)
        val iconLp = LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = dp(4)
        }
        addView(iconView, iconLp)

        // Lottie animation view for Settings tab
        lottieView = LottieAnimationView(context).apply {
            visibility = GONE
        }
        val lottieLp = LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = dp(4)
        }
        addView(lottieView, lottieLp)

        // Shapeable circular avatar view for Profile tab (22x22dp, top-centered, radius 11dp)
        val avatarSize = dp(22)
        avatarView = ShapeableImageView(context).apply {
            visibility = GONE
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(dp(11).toFloat())
                .build()
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val avatarLp = LayoutParams(avatarSize, avatarSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = dp(5)
        }
        addView(avatarView, avatarLp)

        // Text label (12sp, bold, centered, top offset 28.33dp)
        textView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            isSingleLine = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            typeface = com.eve.app.util.FontManager.telegramMediumTypeface(context)
            includeFontPadding = true // Match the source TextView default despite EVE theme overrides.
            gravity = Gravity.CENTER
        }
        defaultTextPaint = TextPaint(textView.paint)
        val textLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            topMargin = dp(28.33f)
        }
        addView(textView, textLp)
    }

    fun configureAsIcon(iconResId: Int, label: String) {
        isAvatarTab = false
        isLottieTab = false
        iconView.visibility = VISIBLE
        lottieView.visibility = GONE
        avatarView.visibility = GONE
        iconView.setImageResource(iconResId)
        textView.text = label
        contentDescription = label
        updateColors()
    }

    fun configureAsLottie(rawResId: Int, fallbackIconResId: Int, label: String) {
        isAvatarTab = false
        isLottieTab = true
        iconView.visibility = GONE
        lottieView.visibility = VISIBLE
        avatarView.visibility = GONE
        try {
            lottieView.setAnimation(rawResId)
        } catch (_: Exception) {
            iconView.visibility = VISIBLE
            lottieView.visibility = GONE
            iconView.setImageResource(fallbackIconResId)
        }
        textView.text = label
        contentDescription = label
        updateColors()
    }

    fun configureAsAvatar(label: String) {
        isAvatarTab = true
        isLottieTab = false
        iconView.visibility = GONE
        lottieView.visibility = GONE
        avatarView.visibility = VISIBLE
        textView.text = label
        contentDescription = label
        updateColors()
    }

    fun setVisualWidth(width: Float) {
        hasVisualWidth = true
        if (visualWidth != width) {
            visualWidth = width
            checkVisualWidth()
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        checkVisualWidth()
    }

    private fun checkVisualWidth() {
        if (hasVisualWidth) {
            val offset = (visualWidth - measuredWidth) / 2f
            iconView.translationX = offset
            lottieView.translationX = offset
            avatarView.translationX = offset
            textView.translationX = offset
        }
    }

    fun setPalette(palette: TelegramThemeEngine.ResolvedPalette) {
        colorDefault = palette.glassTabUnselected
        colorSelected = palette.glassTabSelected
        colorSelectedText = palette.glassTabSelectedText
        updateColors()
        invalidate()
    }

    fun setSelectedTab(selected: Boolean, animated: Boolean) {
        if (isTabSelected == selected && isSelectedAnimator?.isRunning != true) {
            return
        }
        isTabSelected = selected
        isSelected = selected
        isSelectedAnimator?.cancel()

        if (animated) {
            val start = selectedFactor
            val target = if (selected) 1f else 0f
            isSelectedAnimator = ValueAnimator.ofFloat(start, target).apply {
                duration = 320L
                interpolator = decelerateInterpolator
                addUpdateListener { anim ->
                    selectedFactor = anim.animatedValue as Float
                    updateColors()
                    invalidate()
                }
                start()
            }
        } else {
            selectedFactor = if (selected) 1f else 0f
            updateColors()
            invalidate()
        }

        // GlassTabView.java:237 + AndroidUtilities.java:262-270: bold() is Medium (500).
        // Selected weight is 800; EVE keeps the existing system ExtraBold substitute.
        textView.typeface = if (!selected) {
            com.eve.app.util.FontManager.telegramMediumTypeface(context)
        } else if (android.os.Build.VERSION.SDK_INT >= 28) {
            Typeface.create(Typeface.DEFAULT, 800, false)
        } else {
            Typeface.DEFAULT_BOLD
        }
        if (selected && isLottieTab && lottieView.visibility == VISIBLE) {
            lottieView.playAnimation()
        }
    }

    fun isTabSelected(): Boolean = isTabSelected

    private fun updateColors() {
        val colorIcon = ColorUtils.blendARGB(colorDefault, colorSelected, selectedFactor)
        val colorText = ColorUtils.blendARGB(colorDefault, colorSelectedText, selectedFactor)

        val filter = PorterDuffColorFilter(colorIcon, PorterDuff.Mode.SRC_IN)
        if (!isAvatarTab) {
            iconView.colorFilter = filter
            if (isLottieTab && lottieView.visibility == VISIBLE) {
                lottieView.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR_FILTER
                ) { filter }
            }
        }
        textView.setTextColor(colorText)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isSelectedAnimator?.cancel()
        if (isLottieTab) {
            lottieView.cancelAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Selection capsule drawing from Telegram GlassTabView.java:150-164
        if (selectedFactor > 0f) {
            val alpha = decelerateInterpolator.getInterpolation(selectedFactor)
            paintSelector.color = TelegramThemeEngine.multAlpha(colorSelected, 0.09f * alpha)
            tmpRectF.set(0f, 0f, if (hasVisualWidth) visualWidth else width.toFloat(), height.toFloat())
            val r = Math.min(tmpRectF.width(), tmpRectF.height()) / 2f
            val s = lerp(0.6f, 1f, selectedFactor) * MathUtils.clamp(attachScale, 0f, 1f)

            canvas.save()
            canvas.scale(s, s, tmpRectF.centerX(), tmpRectF.centerY())
            canvas.drawRoundRect(tmpRectF, r, r, paintSelector)
            canvas.restore()
        }
        super.onDraw(canvas)
    }

    fun measureTextWidth(): Float {
        return defaultTextPaint.measureText(textView.text.toString())
    }

    fun setTextSizeDp(textSizeDp: Float) {
        val px = dp(textSizeDp).toFloat()
        if (textView.textSize != px) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp)
            defaultTextPaint.textSize = px
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    private fun dp(value: Float): Int {
        return kotlin.math.ceil((value * resources.displayMetrics.density).toDouble()).toInt()
    }

    private fun dp(value: Int): Int {
        return kotlin.math.ceil((value * resources.displayMetrics.density).toDouble()).toInt()
    }
}
