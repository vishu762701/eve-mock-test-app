/*
 * Derived from Telegram Android ShadowSectionCell.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram ShadowSectionCell ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Cells/ShadowSectionCell.java:20-50
 * - Default height: 12dp
 * - Background color: windowBackgroundGray
 */
class ShadowSectionCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private var heightDp: Int = 12

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        setBackgroundColor(palette.windowBackground)
    }

    fun setHeightDp(dpValue: Int) {
        heightDp = dpValue
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, dp(heightDp.toFloat()))
    }
}
