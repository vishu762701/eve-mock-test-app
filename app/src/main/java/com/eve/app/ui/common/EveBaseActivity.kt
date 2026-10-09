package com.eve.app.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.util.FontManager

/**
 * Base activity for all activities in EVE Mock Test App.
 * Handles:
 * 1. Runtime font theme overlay application before view inflation (API 24+ compatible).
 * 2. Automatic back-stack font synchronization on onResume via in-memory fontVersion.
 */
abstract class EveBaseActivity : AppCompatActivity() {

    private var activityFontVersion: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        FontManager.apply(this)
        activityFontVersion = FontManager.fontVersion
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }

    override fun onResume() {
        super.onResume()
        if (activityFontVersion != FontManager.fontVersion) {
            activityFontVersion = FontManager.fontVersion
            recreate()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) com.eve.app.util.SystemBarHelper.syncSystemBars(this)
    }
}
