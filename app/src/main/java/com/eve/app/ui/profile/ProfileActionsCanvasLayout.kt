/*
 * Self-profile action Canvas renderer, isolated from Telegram account/network modules.
 * Source: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java:
 * 116-141, 155-162, 176-224, 247-337, 341-373, 375-384, 383-464, 465-475, 510-587, 1027-1034, 1214-1234.
 * Copyright Nikolai Kudashov / Telegram contributors; GPL v2 or later.
 */
package com.eve.app.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
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
 * ButtonBounce visual scale; original six icon assets; radial gradient highlight
 * and light-mode shadow (ProfileActionsView.java:206-215, 311-325).
 * ADAPTED: Android TextView/StaticLayout measurement, EVE nested scroll height,
 * native click/accessibility wrappers, theme and single-photo expansion,
 * RenderNode avatar blur and loading lifecycle integration.
 */
class ProfileActionsCanvasLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private var radialGradient: RadialGradient? = null

    private val rect = RectF()
    private val pressScale = floatArrayOf(1f, 1f, 1f)
    private val loadingStates = booleanArrayOf(false, false, false)
    private val loadingDrawables = arrayOfNulls<LoadingDrawable>(3)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressRect = RectF()

    private var renderNode: RenderNode? = null
    private var avatarView: View? = null
    private var renderNodeScale: Float = 1f
    private var renderNodeTranslateY: Float = 0f
    private val clipPath = Path()
    private val clipAvatarPath = Path()
    private val locAvatar = IntArray(2)
    private val locActions = IntArray(2)

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
        updateRadialGradient(width)
        val isDark = ColorUtils.calculateLuminance(surfaceColor) < 0.5
        val baseColor = if (isDark) Color.WHITE else Color.BLACK
        for (ld in loadingDrawables) {
            ld?.setColors(
                TelegramThemeEngine.multAlpha(baseColor, 0.1f),
                TelegramThemeEngine.multAlpha(baseColor, 0.3f),
                TelegramThemeEngine.multAlpha(baseColor, 0.35f),
                TelegramThemeEngine.multAlpha(baseColor, 0.8f)
            )
        }
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

    fun startLoading(target: View) {
        val index = (0 until min(childCount, 3)).firstOrNull { getChildAt(it) === target } ?: return
        loadingStates[index] = true
        val ld = loadingDrawables[index] ?: LoadingDrawable(density).also {
            it.callback = this
            val isDark = ColorUtils.calculateLuminance(surfaceColor) < 0.5
            val baseColor = if (isDark) Color.WHITE else Color.BLACK
            it.setColors(
                TelegramThemeEngine.multAlpha(baseColor, 0.1f),
                TelegramThemeEngine.multAlpha(baseColor, 0.3f),
                TelegramThemeEngine.multAlpha(baseColor, 0.35f),
                TelegramThemeEngine.multAlpha(baseColor, 0.8f)
            )
            it.setAppearByGradient(true)
            it.strokePaint.strokeWidth = dp(1.25f)
            loadingDrawables[index] = it
        }
        if (ld.isDisappeared() || ld.isDisappearing()) {
            ld.reset()
            ld.resetDisappear()
        }
        invalidate()
    }

    fun stopLoading(target: View) {
        val index = (0 until min(childCount, 3)).firstOrNull { getChildAt(it) === target } ?: return
        loadingStates[index] = false
        loadingDrawables[index]?.disappear()
        invalidate()
    }

    fun isLoading(target: View): Boolean {
        val index = (0 until min(childCount, 3)).firstOrNull { getChildAt(it) === target } ?: return false
        return loadingStates[index] || (loadingDrawables[index]?.let { !it.isDisappeared() } ?: false)
    }

    override fun verifyDrawable(who: android.graphics.drawable.Drawable): Boolean {
        return super.verifyDrawable(who) || loadingDrawables.any { it === who }
    }

    override fun invalidateDrawable(who: android.graphics.drawable.Drawable) {
        invalidate()
    }

    fun setDrawingBlur(drawing: Boolean) {
        if (!drawing && (renderNode != null || avatarView != null)) {
            renderNode = null
            avatarView = null
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setDrawingBlur(node: RenderNode?, avatar: View?, scale: Float, dy: Float) {
        if (node == null || avatar == null) {
            setDrawingBlur(false)
            return
        }
        if (renderNode == node && avatarView == avatar && renderNodeScale == scale && renderNodeTranslateY == dy) return
        renderNode = node
        avatarView = avatar
        renderNodeScale = scale
        renderNodeTranslateY = dy
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadingStates.fill(false)
        loadingDrawables.forEach {
            it?.callback = null
            it?.resetDisappear()
            it?.reset()
        }
        loadingDrawables.fill(null)
        renderNode = null
        avatarView = null
        radialGradient = null
        shaderPaint.shader = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateRadialGradient(w)
    }

    private fun updateRadialGradient(w: Int) {
        if (surfaceColor == 0 || w <= 0) {
            radialGradient = null
            shaderPaint.shader = null
            return
        }
        val betweenPadding = dp(7f)
        val itemWidth = (w - betweenPadding * 2f - dp(28f)) / 3f
        if (itemWidth <= 0f) return
        val grad = RadialGradient(
            itemWidth / 2f,
            dp(54f) / 2f,
            itemWidth * 0.65f,
            ColorUtils.setAlphaComponent(surfaceColor, (Color.alpha(surfaceColor) * 0.8f).roundToInt()),
            surfaceColor,
            Shader.TileMode.CLAMP
        )
        radialGradient = grad
        shaderPaint.shader = grad
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
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val baseTextColor = if (palette.isDark) Color.WHITE else Color.BLACK
        val textColor = ColorUtils.blendARGB(baseTextColor, Color.WHITE, mix)
        val itemHeight = max(0f, dp(54f) * state.fraction)
        val radius = dp(16f)
        val first = filled
        val second = outline
        val n = min(childCount, 3)

        val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.72
        if (isLight && parentExpanded < 0.5f) {
            val shadowAlpha = ((state.backgroundAlpha * 0.1f) * 32).roundToInt().coerceIn(0, 255)
            surfacePaint.setShadowLayer(dp(1.5f), 0f, 0f, Color.argb(shadowAlpha, 0, 0, 0))
        } else {
            surfacePaint.clearShadowLayer()
        }

        // Stage 1: Button surface backgrounds & radial highlights (ProfileActionsView.java:298-329)
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

            radialGradient?.let { grad ->
                shaderMatrix.setTranslate(left, top)
                grad.setLocalMatrix(shaderMatrix)
                shaderPaint.alpha = (Color.alpha(surfaceColor) * state.backgroundAlpha).roundToInt().coerceIn(0, 255)
                canvas.drawRoundRect(rect, radius, radius, shaderPaint)
            }
            canvas.restoreToCount(checkpoint)
        }

        // Stage 2: RenderNode avatar blur shader (ProfileActionsView.java:331, 341-373)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canvas.isHardwareAccelerated) {
            drawRenderNodeBlur(canvas, n, radius, itemHeight)
        }

        // Stage 3: Text labels and action icons or loading state on top (ProfileActionsView.java:333-338)
        if (state.contentAlpha > 0f) {
            for (index in 0 until n) {
                val host = getChildAt(index) as? LinearLayout ?: continue
                if (host.visibility != View.VISIBLE || host.width <= 0) continue
                val left = host.left.toFloat()
                val right = host.right.toFloat()
                val top = dp(8f)
                val scale = pressScale[index]
                val centerX = (left + right) / 2f
                val centerY = top + itemHeight / 2f
                val checkpoint = canvas.save()
                canvas.scale(scale, scale, centerX, centerY)

                val img = host.getChildAt(0) as? ImageView
                val label = host.getChildAt(1) as? TextView
                if (img != null && label != null) {
                    paintContent(
                        canvas, left, right, top, itemHeight,
                        mix, textColor, label, first[index], second[index], index
                    )
                }

                val ld = loadingDrawables[index]
                if (ld != null && !ld.isDisappeared()) {
                    rect.set(left, top, right, top + itemHeight)
                    ld.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
                    ld.setRadii(radius)
                    ld.alpha = (255f * state.contentAlpha).roundToInt().coerceIn(0, 255)
                    ld.draw(canvas)
                }
                canvas.restoreToCount(checkpoint)
            }
        }
        if (isAttachedToWindow && (loadingStates.any { it } || loadingDrawables.any { it != null && !it.isDisappeared() })) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawRenderNodeBlur(canvas: Canvas, n: Int, radius: Float, itemHeight: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val node = renderNode ?: return
        if (!node.hasDisplayList() || !canvas.isHardwareAccelerated) return

        val checkpoint = canvas.save()
        val avatar = avatarView
        if (avatar != null && avatar.isAttachedToWindow && isAttachedToWindow) {
            avatar.getLocationInWindow(locAvatar)
            getLocationInWindow(locActions)
            val vl = (locAvatar[0] - locActions[0]).toFloat()
            val vt = (locAvatar[1] - locActions[1]).toFloat()
            val vw = avatar.width.toFloat() * avatar.scaleX
            val vh = avatar.height.toFloat() * avatar.scaleY

            clipAvatarPath.rewind()
            clipAvatarPath.addRoundRect(
                vl,
                vt,
                vl + vw,
                vt + vh,
                vw / 2f,
                vh / 2f,
                Path.Direction.CCW
            )
            canvas.clipPath(clipAvatarPath)
        }

        clipPath.rewind()
        for (i in 0 until n) {
            val host = getChildAt(i) as? LinearLayout ?: continue
            if (host.visibility != View.VISIBLE || host.width <= 0) continue
            val left = host.left.toFloat()
            val right = host.right.toFloat()
            val top = dp(8f)
            val r = RectF(left, top, right, top + itemHeight)
            val s = pressScale[i]
            r.inset(r.width() / 2f * (1f - s), r.height() / 2f * (1f - s))
            clipPath.addRoundRect(r, radius, radius, Path.Direction.CCW)
        }
        canvas.clipPath(clipPath)
        canvas.translate(0f, renderNodeTranslateY)
        canvas.scale(renderNodeScale, renderNodeScale)
        canvas.drawRenderNode(node)
        canvas.restoreToCount(checkpoint)
    }

    private fun paintContent(
        canvas: Canvas, left: Float, right: Float, top: Float,
        itemHeight: Float, mix: Float, textColor: Int,
        label: TextView, filledIcon: android.graphics.drawable.Drawable?,
        outlineIcon: android.graphics.drawable.Drawable?,
        index: Int
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
        // 54dp target, offset by pill top margin (top).
        val iconTop = top + max(0f, (dp(54f) - textLayout.height * textScale) / 3f + dp(1.33f))
        val iconSize = dp(24f)
        val iconLeft = centerX - iconSize / 2f
        val textY = (iconTop + iconSize) + (iconTop - top) -
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

        val ld = loadingDrawables[index]
        val isLoadingActive = loadingStates[index] || (ld != null && !ld.isDisappeared())
        fun paintIcon(icon: android.graphics.drawable.Drawable?, opacity: Float) {
            if (icon == null || opacity <= 0f) return
            icon.bounds = android.graphics.Rect(iconLeft.toInt(), iconTop.toInt(),
                (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt())
            icon.colorFilter = PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)
            val iconAlpha = if (isLoadingActive) 0.35f else 1f
            icon.alpha = (255f * contentAlpha * opacity * iconAlpha).roundToInt().coerceIn(0, 255)
            icon.draw(canvas)
        }
        paintIcon(outlineIcon, 1f - mix)
        paintIcon(filledIcon, mix)
        canvas.restoreToCount(clip)
    }
}
