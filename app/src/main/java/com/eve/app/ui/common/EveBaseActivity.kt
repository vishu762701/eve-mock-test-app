package com.eve.app.ui.common

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/** Shared edge-to-edge host, typography styling, and accessibility contrast synchronization. */
abstract class EveBaseActivity : AppCompatActivity() {

    private var highContrast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        com.eve.app.util.EvePerformanceTracer.recordLaunchStart(this, isRestoring = savedInstanceState != null)
        highContrast = com.eve.app.util.DesignContrast.apply(this)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
        hookFirstFrameTracer()
    }

    override fun onRestart() {
        super.onRestart()
        com.eve.app.util.EvePerformanceTracer.recordReturnStart(this)
        hookFirstFrameTracer()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
        applyTypographyIfSafe()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
        applyTypographyIfSafe()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
        applyTypographyIfSafe()
    }

    override fun onResume() {
        super.onResume()
        if (highContrast != com.eve.app.util.DesignContrast.isHigh(this) &&
            !com.eve.app.util.ThemeManager.isTransitioning) {
            recreate()
            return
        }
        applyTypographyIfSafe()
        com.eve.app.util.EvePerformanceTracer.recordInteractive(this)
    }

    private fun hookFirstFrameTracer() {
        val decor = window?.decorView ?: return
        val vto = decor.viewTreeObserver
        if (vto != null && vto.isAlive) {
            vto.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (decor.viewTreeObserver.isAlive) {
                        decor.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    com.eve.app.util.EvePerformanceTracer.recordFirstFrame(this@EveBaseActivity)
                    return true
                }
            })
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    private val themePaletteListener: (com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) -> Unit = { palette ->
        onApplyThemePalette(palette)
    }

    override fun onStart() {
        super.onStart()
        com.eve.app.theme.TelegramThemeEngine.addThemeChangeListener(themePaletteListener)
        onApplyThemePalette(com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(this))
    }

    override fun onStop() {
        super.onStop()
        com.eve.app.theme.TelegramThemeEngine.removeThemeChangeListener(themePaletteListener)
    }

    private var lastAppliedPalette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette? = null

    protected open fun onApplyThemePalette(palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) {
        if (lastAppliedPalette == palette) {
            com.eve.app.util.SystemBarHelper.syncSystemBars(this)
            return
        }
        lastAppliedPalette = palette
        com.eve.app.theme.TelegramThemeApplier.applyToActivity(this, palette)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    private fun applyTypographyIfSafe() {
        try {
            com.eve.app.typography.TypographyApplier.applyToActivity(this)
        } catch (_: Exception) {}
    }
}
