/*
 * Source-derived inline spinner branch:
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * ActionBar/AlertDialog.java:329-336,371-375,901-915.
 * Copyright Nikolai Kudashov (2013-2018), GPL-2.0-or-later.
 *
 * EVE-native ADAPTED renderer. Upstream dimensions and entrance physics
 * are retained; blur and dialog_inlineProgressBackground color role are
 * adapted to the EVE theme palette. No Telegram account/service classes.
 */
package com.eve.app.designsystem

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/** Modal inline loading spinner; NEVER blocks or mutates any EVE network operation. */
class TelegramNativeSpinnerDialog(context: Context) :
    Dialog(context, R.style.Theme_Eve_TelegramNativeDialog) {

    private var panel: FrameLayout? = null
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val panelColor = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface
        val host = FrameLayout(context).apply {
            tag = "telegram_native_spinner_panel"
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(panelColor)
            }
            addView(TelegramRadialProgressView(context).apply {
                tag = "telegram_native_spinner_progress"
                setSize(dp(32))
                setProgressColor(if (palette.isDark) Color.WHITE else palette.accent)
            }, FrameLayout.LayoutParams(dp(86), dp(86), Gravity.CENTER))
        }
        panel = host
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(0.5f)
        setContentView(host, android.view.ViewGroup.LayoutParams(dp(86), dp(86)))
        window?.setLayout(dp(86), dp(86))
    }

    private fun hostAlive(): Boolean {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return !current.isDestroyed && !current.isFinishing
            current = current.baseContext
        }
        return true
    }

    override fun show() {
        if (!hostAlive()) return
        super.show()
        panel?.let { view ->
            view.animate().cancel()
            if (UiArcMotion.isMotionEnabled(context)) {
                view.scaleX = 0f
                view.scaleY = 0f
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setInterpolator(OvershootInterpolator(1.3f))
                    .setDuration(190)
                    .start()
            } else {
                view.scaleX = 1f
                view.scaleY = 1f
            }
        }
    }

    override fun onStop() {
        panel?.animate()?.cancel()
        super.onStop()
    }
}
