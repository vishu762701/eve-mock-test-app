package com.eve.app.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import com.eve.app.R

/**
 * Source-backed adaptation of Telegram ActionBarPopupWindow.GapView
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15,
 * ActionBarPopupWindow.java:1110-1139 and Theme.java:8910-8916.
 *
 * Original greydivider.9.png density binaries are byte-exact and GPL-covered.
 * EVE XML supplies the original Day/Night separator theme background.
 */
class TelegramPopupGapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val shadowDrawable = AppCompatResources.getDrawable(
        context, R.drawable.telegram_menu_greydivider
    )?.mutate()?.apply {
        // ThemeColors.java:134: key_windowBackgroundGrayShadow = 0xff000000;
        // uploaded night.attheme has no override for that key.
        colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY)
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        shadowDrawable?.setBounds(0, 0, width, height)
        shadowDrawable?.draw(canvas)
    }
}
