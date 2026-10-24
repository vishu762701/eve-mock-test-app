package com.eve.app.ui.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Floating bottom navigation bar matching Telegram's MainTabs bar.
 * Height 56dp, margin 8dp, outer allocation 72dp before system insets.
 * Background uses source BlurredBackgroundProviderImpl.mainTabs() properties.
 */
class EveBottomPillBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val tabsLayout: MainTabsLayout
    val tabHome: GlassTabView
    val tabHistory: GlassTabView
    val tabSettings: GlassTabView
    val tabProfile: GlassTabView

    val glassBackgroundView: android.view.View
    private val backgroundDrawable: TelegramGlassBackgroundDrawable

    companion object {
        const val TAB_HOME = 0
        const val TAB_HISTORY = 1
        const val TAB_SETTINGS = 2
        const val TAB_PROFILE = 3
    }

    internal var currentPalette: TelegramThemeEngine.ResolvedPalette? = null

    init {
        clipChildren = false
        clipToPadding = false

        backgroundDrawable = TelegramGlassBackgroundDrawable(context)
        glassBackgroundView = android.view.View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = backgroundDrawable
        }
        addView(glassBackgroundView)

        tabsLayout = MainTabsLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(56))
        }

        tabHome = GlassTabView(context).apply {
            configureAsIcon(R.drawable.msg_home, "Home")
        }
        tabHistory = GlassTabView(context).apply {
            configureAsIcon(R.drawable.ic_history, "History")
        }
        tabSettings = GlassTabView(context).apply {
            configureAsLottie(R.raw.tab_settings, R.drawable.ic_settings, "Settings")
        }
        tabProfile = GlassTabView(context).apply {
            configureAsAvatar("Profile")
        }

        tabsLayout.addView(tabHome)
        tabsLayout.addView(tabHistory)
        tabsLayout.addView(tabSettings)
        tabsLayout.addView(tabProfile)

        addView(tabsLayout)

        updateThemePalette(TelegramThemeEngine.resolveCurrentPalette(context))

        // Handle navigation bar system insets
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val lp = v.layoutParams as? MarginLayoutParams
            if (lp != null) {
                val bottomMargin = dp(8) + navBars.bottom
                if (lp.bottomMargin != bottomMargin) {
                    lp.bottomMargin = bottomMargin
                    v.layoutParams = lp
                }
            }
            insets
        }
    }

    fun selectTab(position: Int, animated: Boolean = true, notify: Boolean = true) {
        tabsLayout.selectTab(position, animated, notify)
    }

    fun getSelectedTab(): Int = tabsLayout.getSelectedTabPosition()

    private var currentTabListener: MainTabsLayout.TabSelectionListener? = null

    fun setTabSelectionListener(listener: MainTabsLayout.TabSelectionListener) {
        currentTabListener = listener
        tabsLayout.setTabSelectionListener(listener)
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        val prev = currentTabListener
        val combined = object : MainTabsLayout.TabSelectionListener {
            override fun onTabSelected(position: Int) {
                listener(position)
                prev?.onTabSelected(position)
            }
            override fun onTabReselected(position: Int) {
                prev?.onTabReselected(position)
            }
        }
        currentTabListener = combined
        tabsLayout.setTabSelectionListener(combined)
    }

    fun setOnTabReselectedListener(listener: (Int) -> Unit) {
        val prev = currentTabListener
        val combined = object : MainTabsLayout.TabSelectionListener {
            override fun onTabSelected(position: Int) {
                prev?.onTabSelected(position)
            }
            override fun onTabReselected(position: Int) {
                listener(position)
                prev?.onTabReselected(position)
            }
        }
        currentTabListener = combined
        tabsLayout.setTabSelectionListener(combined)
    }

    fun setUserAvatar(photoUrl: String?) {
        if (!photoUrl.isNullOrEmpty()) {
            tabProfile.avatarView.load(photoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_user_profile)
                error(R.drawable.ic_user_profile)
            }
        } else {
            tabProfile.avatarView.setImageResource(R.drawable.ic_user_profile)
        }
    }

    fun updateThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        currentPalette = palette
        backgroundDrawable.setPalette(palette)
        tabsLayout.updatePalette(palette)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidGlassHelper.apply(glassBackgroundView, context, palette)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && currentPalette != null) {
            LiquidGlassHelper.apply(glassBackgroundView, context, currentPalette!!)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    /**
     * Source-matching Telegram glass background drawable.
     * Ported from Telegram BlurredBackgroundProviderImpl.mainTabs() & StrokeDrawable.
     */
    private class TelegramGlassBackgroundDrawable(val context: Context) : Drawable() {
        private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paintStrokeTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val paintStrokeBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val paintShadow = Paint(Paint.ANTI_ALIAS_FLAG)

        private val rect = RectF()
        private var radius = 0f
        private var strokeTopWidth = 0f
        private var strokeBottomWidth = 0f
        private var shadowRadius = 0f
        private var shadowDy = 0f
        private var isDark = false

        init {
            val density = context.resources.displayMetrics.density
            radius = 28f * density // DialogsActivity.MAIN_TABS_HEIGHT / 2f
            strokeTopWidth = 0.4f * density
            strokeBottomWidth = 0.4f * density

            paintStrokeTop.strokeWidth = strokeTopWidth
            paintStrokeBottom.strokeWidth = strokeBottomWidth

            // Shadow properties from BlurredBackgroundProviderImpl: radius 2.667dp, dy 0.85dp
            shadowRadius = 2.667f * density
            shadowDy = 0.85f * density
            paintShadow.setShadowLayer(shadowRadius, 0f, shadowDy, 0x20000000)
        }

        fun setPalette(palette: TelegramThemeEngine.ResolvedPalette) {
            isDark = palette.isDark
            val bgTarget = palette.glassTargetMainTabs
            val bgSurface = palette.surface
            // Source coating alpha is 0.85 with liquid glass, 0.76 fallback
            val alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 0.85f else 0.76f
            val fillColor = TelegramThemeEngine.solveSrcColor(bgSurface, bgTarget, alpha)

            paintFill.color = fillColor

            // Stroke colors from source:
            // setStrokeColorTop(0x11000000, 0x06FFFFFF)
            // setStrokeColorBottom(0x20000000, 0x11FFFFFF)
            // setShadowColor(0x20000000, 0x04FFFFFF)
            if (isDark) {
                paintStrokeTop.color = 0x06FFFFFF
                paintStrokeBottom.color = 0x11FFFFFF
                paintShadow.color = 0x04FFFFFF
                paintShadow.setShadowLayer(shadowRadius, 0f, shadowDy, 0x04FFFFFF)
            } else {
                paintStrokeTop.color = 0x11000000
                paintStrokeBottom.color = 0x20000000
                paintShadow.color = 0x20000000
                paintShadow.setShadowLayer(shadowRadius, 0f, shadowDy, 0x20000000)
            }
            invalidateSelf()
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            rect.set(bounds)
        }

        override fun draw(canvas: Canvas) {
            // Shadow layer
            canvas.drawRoundRect(rect, radius, radius, paintShadow)
            // Fill
            canvas.drawRoundRect(rect, radius, radius, paintFill)
            // Top stroke (clipped to top)
            drawStroke(canvas, rect, radius, strokeTopWidth, true, paintStrokeTop)
            // Bottom stroke (clipped to bottom)
            drawStroke(canvas, rect, radius, strokeBottomWidth, false, paintStrokeBottom)
        }

        private fun drawStroke(
            canvas: Canvas,
            rect: RectF,
            radii: Float,
            strokeWidth: Float,
            isTop: Boolean,
            paint: Paint
        ) {
            val strokeHalf = strokeWidth / 2f
            canvas.save()
            if (isTop) {
                if (canvas.clipRect(rect.left - strokeHalf, rect.top, rect.right + strokeHalf, MathUtils.clamp(rect.top + radii * 2, rect.top, rect.bottom))) {
                    canvas.drawRoundRect(
                        rect.left - strokeHalf,
                        rect.top + strokeHalf,
                        rect.right + strokeHalf,
                        rect.bottom + strokeHalf,
                        radii, radii,
                        paint
                    )
                }
            } else {
                if (canvas.clipRect(rect.left - strokeHalf, MathUtils.clamp(rect.bottom - radii * 2, rect.top, rect.bottom), rect.right + strokeHalf, rect.bottom)) {
                    canvas.drawRoundRect(
                        rect.left - strokeHalf,
                        rect.top - strokeHalf,
                        rect.right + strokeHalf,
                        rect.bottom - strokeHalf,
                        radii, radii,
                        paint
                    )
                }
            }
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) {
            paintFill.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paintFill.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object LiquidGlassHelper {
        private var runtimeShader: android.graphics.RuntimeShader? = null

        fun apply(view: android.view.View, context: Context, palette: TelegramThemeEngine.ResolvedPalette) {
            try {
                if (runtimeShader == null) {
                    val code = context.resources.openRawResource(R.raw.liquid_glass_shader).bufferedReader().use { it.readText() }
                    runtimeShader = android.graphics.RuntimeShader(code)
                }
                val shader = runtimeShader ?: run {
                    view.setRenderEffect(null)
                    return
                }
                val w = view.width.toFloat()
                val h = view.height.toFloat()
                if (w <= 0f || h <= 0f) {
                    view.setRenderEffect(null)
                    return
                }

                val density = context.resources.displayMetrics.density
                val radius = 28f * density

                // In Telegram's liquid_glass_shader.agsl:
                // srcOver(foreground_color_premultiplied, img.eval(uv))
                // outRGB = src.rgb + dst.rgb * (1.0 - src.a)
                // A direct opaque palette target (alpha=1.0) zeroes out sampled backdrop.
                // Translucent coating alpha (0.55f) ensures the backdrop is refracted and visible.
                val bgTarget = palette.glassTargetMainTabs
                val bgSurface = palette.surface
                val coating = TelegramThemeEngine.solveSrcColor(bgSurface, bgTarget, 0.55f)
                val a = (Color.alpha(coating) / 255f).coerceIn(0.20f, 0.70f)
                val r = Color.red(coating) / 255f * a
                val g = Color.green(coating) / 255f * a
                val b = Color.blue(coating) / 255f * a

                shader.setFloatUniform("resolution", w, h)
                shader.setFloatUniform("center", w / 2f, h / 2f)
                shader.setFloatUniform("size", w / 2f, h / 2f)
                shader.setFloatUniform("radius", radius, radius, radius, radius)
                shader.setFloatUniform("thickness", 20f * density)
                shader.setFloatUniform("refract_intensity", 0.15f)
                shader.setFloatUniform("refract_index", 1.33f)
                shader.setFloatUniform("foreground_color_premultiplied", r, g, b, a)

                val effect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "img")
                view.setRenderEffect(effect)
            } catch (_: Throwable) {
                // Supported fallback: clear render effect, backgroundDrawable draws solid coating & strokes
                view.setRenderEffect(null)
            }
        }
    }
}
