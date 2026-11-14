package com.eve.app.ui.premium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine

/**
 * Telegram PremiumFeatureCell.java:48-78 layout:
 * - 28x28dp icon at start margin 18dp
 * - Text column at start margin 62dp, top 8dp, end 24dp, bottom 9dp
 * - Text: 15sp (or Title 15sp + Subtitle 14sp per PremiumFeatureCell)
 * - Hairline divider at start inset 62dp
 * - Flat touch selector
 */
object PremiumBenefitRows {
    fun bind(container: LinearLayout, benefits: List<String>) {
        val context = container.context
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val density = context.resources.displayMetrics.density
        fun dp(v: Float): Int = (v * density + 0.5f).toInt()

        container.removeAllViews()
        benefits.forEachIndexed { index, benefit ->
            val isLast = index == benefits.lastIndex
            val cell = object : FrameLayout(context) {
                private val dividerPaint = Paint().apply {
                    strokeWidth = 1f
                    color = palette.hairline
                }

                init {
                    setWillNotDraw(false)
                    background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
                    minimumHeight = dp(56f)

                    val icon = ImageView(context).apply {
                        setImageResource(R.drawable.ic_check_circle)
                        setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                    addView(icon, LayoutParams(dp(28f), dp(28f), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                        marginStart = dp(18f)
                    })

                    val textCol = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        val tv = TextView(context).apply {
                            text = benefit
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                            setLineSpacing(dp(2f).toFloat(), 1f)
                            com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.BODY)
                        }
                        addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    }
                    addView(textCol, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                        marginStart = dp(62f)
                        marginEnd = dp(24f)
                        topMargin = dp(10f)
                        bottomMargin = dp(10f)
                    })
                }

                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    if (!isLast) {
                        canvas.drawLine(
                            dp(62f).toFloat(),
                            (height - 1).toFloat(),
                            width.toFloat(),
                            (height - 1).toFloat(),
                            dividerPaint
                        )
                    }
                }
            }
            container.addView(cell, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }
}
