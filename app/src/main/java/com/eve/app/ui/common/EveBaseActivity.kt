package com.eve.app.ui.common

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/** Shared edge-to-edge host and accessibility contrast synchronization. */
abstract class EveBaseActivity : AppCompatActivity() {

    private var highContrast = false
    override fun onCreate(savedInstanceState: Bundle?) {
        highContrast = com.eve.app.util.DesignContrast.apply(this)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    override fun onResume() {
        super.onResume()
        if (highContrast != com.eve.app.util.DesignContrast.isHigh(this) &&
            !com.eve.app.util.ThemeManager.isTransitioning) {
            recreate()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }
}
