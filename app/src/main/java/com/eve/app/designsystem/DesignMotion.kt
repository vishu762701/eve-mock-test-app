package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.animation.AnimationUtils
import com.eve.app.R

/** Shared curve and animation availability; Android applies the configured duration scale. */
object DesignMotion {
    fun interpolator(context: Context) = AnimationUtils.loadInterpolator(context, R.interpolator.design_screen_curve)

    fun enabled(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 26) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
