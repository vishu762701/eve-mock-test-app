package com.eve.app.util

import android.app.Activity
import android.app.UiModeManager
import android.os.Build
import com.eve.app.R

/** Android accessibility interpretation; no UIKit or proprietary rendering dependency. */
object DesignContrast {
    fun isHigh(activity: Activity): Boolean = Build.VERSION.SDK_INT >= 34 &&
        activity.getSystemService(UiModeManager::class.java).contrast >= 0.5f

    fun apply(activity: Activity): Boolean {
        val high = isHigh(activity)
        if (high) activity.theme.applyStyle(R.style.ThemeOverlay_Eve_HighContrast, true)
        return high
    }
}
