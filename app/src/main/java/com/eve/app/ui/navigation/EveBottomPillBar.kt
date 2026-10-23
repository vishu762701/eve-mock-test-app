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

    private val backgroundDrawable: TelegramGlassBackgroundDrawable

    companion object {
        const val TAB_HOME = 0
        const val TAB_HISTORY = 1
        const val TAB_SETTINGS = 2
        const val TAB_PROFILE = 3
    }

    init {
        clipChildren = false
        clipToPadding = false

        backgroundDrawable = TelegramGlassBackgroundDrawable(context)
        background = backgroundDrawable

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

    fun selectTab(position: Int, animated: Boolean = true) {
        tabsLayout.selectTab(position, animated)
    }

    fun getSelectedTab(): Int = tabsLayout.getSelectedTabPosition()

    fun setTabSelectionListener(listener: MainTabsLayout.TabSelectionListener) {
        tabsLayout.setTabSelectionListener(listener)
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
        backgroundDrawable.setPalette(palette)
        tabsLayout.updatePalette(palette)
        invalidate()
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
        private var isDark = false

        init {
            val density = context.resources.displayMetrics.density
            radius = 28f * density // DialogsActivity.MAIN_TABS_HEIGHT / 2f
            strokeTopWidth = 0.4f * density
            strokeBottomWidth = 0.4f * density

            paintStrokeTop.strokeWidth = strokeTopWidth
            paintStrokeBottom.strokeWidth = strokeBottomWidth

            // Shadow properties from BlurredBackgroundProviderImpl: radius 2.667dp, dy 0.85dp
            val shadowRadius = 2.667f * density
            val shadowDy = 0.85f * density
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
            } else {
                paintStrokeTop.color = 0x11000000
                paintStrokeBottom.color = 0x20000000
                paintShadow.color = 0x20000000
            }
            invalidateSelf()
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            rect.set(bounds)
        }

        override fun draw(canvas: Canvas) {
            // Fill
            canvas.drawRoundRect(rect, radius, radius, paintFill)
            // Top stroke
            canvas.drawRoundRect(rect, radius, radius, paintStrokeTop)
            // Bottom stroke
            canvas.drawRoundRect(rect, radius, radius, paintStrokeBottom)
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
}
