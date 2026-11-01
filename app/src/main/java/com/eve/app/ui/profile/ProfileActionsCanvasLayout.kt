/*
 * Self-profile action Canvas renderer, isolated from Telegram account/network modules.
 * Source: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java:
 * 116-141, 176-224, 247-337, 383-464, 510-587, 1027-1034, 1214-1234.
 * Copyright Nikolai Kudashov / Telegram contributors; GPL v2 or later.
 */
package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Real Canvas paint path for Telegram MODE_MY_PROFILE's three actions.
 * EVE retains invisible native LinearLayout children as hit/keyboard/TalkBack
 * targets, because Telegram virtual nodes cannot own app-specific callbacks.
 * The source action area and its independent alpha/scale stages are drawn here,
 * not through three XML pill backgrounds.
 *
 * EXACT: mode keys/order, 14/7/8/12/54dp geometry, 16dp radius, two alpha
 * curves, icon size, text placement formula, brightness threshold and 0.04
 * ButtonBounce visual scale; original six icon assets.
 * ADAPTED: Android TextView/StaticLayout measurement, EVE nested scroll height,
 * native click/accessibility wrappers, theme and single-photo expansion.
 * Not a port of other Telegram action modes, account animations or RenderNode.
 */
class ProfileActionsCanvasLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    private val pressScale = floatArrayOf(1f, 1f, 1f)
    private val filledRes = intArrayOf(
        R.drawable.telegram_profile_photo,
        R.drawable.telegram_profile_edit,
        R.drawable.telegram_profile_settings
    )
    private val outlineRes = intArrayOf(
        R.drawable.telegram_profile_outline_photo,
        R.drawable.telegram_profile_outline_edit,
        R.drawable.telegram_profile_outline_settings
    )
    private val filled by lazy { filledRes.map { ContextCompat.getDrawable(context, it)?.mutate() } }
    private val outline by lazy { outlineRes.map { ContextCompat.getDrawable(context, it)?.mutate() } }
    private var state = ProfileActionsCollapse.atScroll(0f, density)
    private var surfaceColor = ContextCompat.getColor(context, R.color.category_pill_unselected_bg)
    private var parentExpanded = 0f

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    internal fun setCollapseState(next: ProfileActionsCollapse.State) {
        if (state == next) return
        state = next
        invalidate()
    }

    internal val renderedState: ProfileActionsCollapse.State get() = state

    fun setActionColor(value: Int) {
        if (surfaceColor == value) return
        surfaceColor = value
        invalidate()
    }

    fun setParentExpanded(value: Float) {
        val f = value.coerceIn(0f, 1f)
        if (parentExpanded == f) return
        parentExpanded = f
        invalidate()
    }

    internal fun setPressScale(target: View, scale: Float) {
        val index = (0 until min(childCount, 3)).firstOrNull { getChildAt(it) === target } ?: return
        val next = scale.coerceIn(0.85f, 1.1f)
        if (pressScale[index] != next) {
            pressScale[index] = next
            invalidate()
        }
    }

    private fun iconMix(): Float {
        // ProfileActionsView.java:412-418: dark surfaces use filled white;
        // light surfaces blend to white when their parent photo expands.
        if (!ProfileActionIcons.isLightButtonColor(surfaceColor)) return 1f
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0f
        return ((parentExpanded - .75f) / .25f).coerceIn(0f, 1f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (childCount < 3 || state.backgroundAlpha <= 0f) return
        val mix = iconMix()
        val textColor = ColorUtils.blendARGB(Color.BLACK, Color.WHITE, mix)
        val itemHeight = max(0f, dp(54f) * state.fraction)
        val radius = dp(16f)
        val first = filled
        val second = outline
        val n = min(childCount, 3)
        for (index in 0 until n) {
            val host = getChildAt(index) as? LinearLayout ?: continue
            if (host.visibility != View.VISIBLE || host.width <= 0) continue
            val left = host.left.toFloat()
            val right = host.right.toFloat()
            val top = dp(8f)
            rect.set(left, top, right, top + itemHeight)
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val scale = pressScale[index]
            val checkpoint = canvas.save()
            canvas.scale(scale, scale, centerX, centerY)

            surfacePaint.color = surfaceColor
            surfacePaint.alpha = (Color.alpha(surfaceColor) * state.backgroundAlpha).roundToInt().coerceIn(0, 255)
            canvas.drawRoundRect(rect, radius, radius, surfacePaint)

            if (state.contentAlpha > 0f) {
                val img = host.getChildAt(0) as? ImageView
                val label = host.getChildAt(1) as? TextView
                if (img != null && label != null) {
                    paintContent(canvas, index, left, right, top, itemHeight,
                        mix, textColor, label, first[index], second[index])
                }
            }
            canvas.restoreToCount(checkpoint)
        }
    }

    private fun paintContent(
        canvas: Canvas, index: Int, left: Float, right: Float, top: Float,
        itemHeight: Float, mix: Float, textColor: Int,
        label: TextView, filledIcon: android.graphics.drawable.Drawable?,
        outlineIcon: android.graphics.drawable.Drawable?
    ) {
        val contentAlpha = state.contentAlpha
        val centerX = (left + right) / 2f
        val labelPaint = TextPaint(label.paint).apply {
            color = textColor
            isAntiAlias = true
        }
        val available = (right - left - dp(2f)).toInt().coerceAtLeast(1)
        val labelText = label.text
        val textLayout = StaticLayout.Builder.obtain(labelText, 0, labelText.length, labelPaint, available)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(3)
            .build()
        val textScale = when {
            textLayout.lineCount >= 3 -> .75f
            textLayout.lineCount >= 2 -> .85f
            else -> 1f
        }
        // ProfileActionsView.updateBounds() positions the icon relative to the
        // 54dp target, not a height that shrinks during scroll.
        val iconTop = max(0f, (dp(54f) - textLayout.height * textScale) / 3f + dp(1.33f))
        val iconSize = dp(24f)
        val iconLeft = centerX - iconSize / 2f
        val textY = (iconTop + iconSize) + iconTop -
            textLayout.height * textScale / 2f - dp(4.66f)
        val clip = canvas.save()
        canvas.scale(state.fraction, state.fraction, centerX, top + itemHeight / 2f)
        canvas.clipRect(left, top, right, top + itemHeight)
        val t = canvas.save()
        canvas.scale(textScale, textScale, centerX, textY + textLayout.height * textScale / 2f)
        canvas.translate(centerX - textLayout.width / 2f, textY)
        labelPaint.alpha = (Color.alpha(textColor) * contentAlpha).roundToInt().coerceIn(0, 255)
        textLayout.draw(canvas)
        canvas.restoreToCount(t)

        fun paintIcon(icon: android.graphics.drawable.Drawable?, opacity: Float) {
            if (icon == null || opacity <= 0f) return
            icon.bounds = android.graphics.Rect(iconLeft.toInt(), iconTop.toInt(),
                (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt())
            icon.colorFilter = PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            icon.alpha = (255f * contentAlpha * opacity).roundToInt().coerceIn(0, 255)
            icon.draw(canvas)
        }
        paintIcon(outlineIcon, 1f - mix)
        paintIcon(filledIcon, mix)
        canvas.restoreToCount(clip)
    }
}
