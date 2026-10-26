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
    internal val backgroundDrawable: TelegramGlassBackgroundDrawable

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
        glassBackgroundView = com.eve.app.designsystem.DesignGlassLayout(context).apply {
            useCapsuleShape()
            mainTabsCoating = true
            drawHairline = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            foreground = backgroundDrawable
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
            val fallback = tabProfile.avatarFallbackDrawable()
            tabProfile.avatarView.load(photoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(fallback)
                error(fallback)
            }
        } else {
            tabProfile.avatarView.setImageDrawable(tabProfile.avatarFallbackDrawable())
        }
    }

    fun updateThemePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        currentPalette = palette
        backgroundDrawable.setPalette(palette)
        tabsLayout.updatePalette(palette)
        (glassBackgroundView as com.eve.app.designsystem.DesignGlassLayout).updatePalette(palette)
        invalidate()
    }


    // Source: MainTabsLayout.java:64-66 caps the tab strip at 344dp.
    // Cap the OUTER glass too; a MATCH_PARENT glass background otherwise spans
    // the entire window even when MainTabsLayout internally measures narrower.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val maxWidth = dp(344)
        val boundedWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            maxWidth
        } else {
            minOf(available, maxWidth)
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(boundedWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    /**
     * Source edge-color and shadow foreground; DesignGlassLayout owns backdrop/coating.
     * Ported from Telegram BlurredBackgroundProviderImpl.mainTabs() & StrokeDrawable.
     */
    internal class TelegramGlassBackgroundDrawable(val context: Context) : Drawable() {
        private val paintStrokeTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val paintStrokeBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val paintShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        private val rect = RectF()
        private var radius = 0f
        private var strokeTopWidth = 0f
        private var strokeBottomWidth = 0f
        private var shadowRadius = 0f
        private var shadowDy = 0f
        private var shadowColor = 0x20000000
        private var isDark = false

        init {
            val density = context.resources.displayMetrics.density
            radius = 28f * density // DialogsActivity.MAIN_TABS_HEIGHT / 2f
            strokeTopWidth = 0.4f * density
            strokeBottomWidth = 0.4f * density
            shadowRadius = 2.667f * density
            shadowDy = 0.85f * density

            paintStrokeTop.strokeWidth = strokeTopWidth
            paintStrokeBottom.strokeWidth = strokeBottomWidth
            paintShadow.strokeWidth = strokeBottomWidth
            paintShadow.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }

        fun setPalette(palette: TelegramThemeEngine.ResolvedPalette) {
            isDark = palette.isDark
            // Source non-liquid branch: the BlurView behind this foreground draws the
            // captured backdrop plus solveSrcColor(surface, target, .76) exactly once.
            // Stroke and shadow colors from source:
            // setStrokeColorTop(0x11000000, 0x06FFFFFF)
            // setStrokeColorBottom(0x20000000, 0x11FFFFFF)
            // setShadowColor(0x20000000, 0x04FFFFFF)
            if (isDark) {
                paintStrokeTop.color = 0x06FFFFFF
                paintStrokeBottom.color = 0x11FFFFFF
                shadowColor = 0x04FFFFFF
            } else {
                paintStrokeTop.color = 0x11000000
                paintStrokeBottom.color = 0x20000000
                shadowColor = 0x20000000
            }
            paintShadow.color = shadowColor
            paintShadow.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            invalidateSelf()
        }

        internal val strokeTopColor get() = paintStrokeTop.color
        internal val strokeBottomColor get() = paintStrokeBottom.color
        internal val currentShadowColor get() = shadowColor
        internal val currentShadowRadius get() = shadowRadius
        internal val currentShadowDy get() = shadowDy
        internal val currentStrokeWidth get() = strokeTopWidth

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            rect.set(bounds)
        }

        override fun draw(canvas: Canvas) {
            // Shadow layer from BlurredBackgroundProviderImpl (radius 2.667dp, dy 0.85dp, stroke 0.4dp)
            canvas.drawRoundRect(rect, radius, radius, paintShadow)
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
            paintStrokeTop.alpha = alpha
            paintStrokeBottom.alpha = alpha
            paintShadow.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paintStrokeTop.colorFilter = colorFilter
            paintStrokeBottom.colorFilter = colorFilter
            paintShadow.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

}
