package com.eve.app.ui.premium

import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R

/** Shared production row builder for server-provided benefits and native review. */
object PremiumBenefitRows {
    fun bind(container: LinearLayout, benefits: List<String>) {
        val context = container.context
        val density = context.resources.displayMetrics.density
        container.removeAllViews()
        for (benefit in benefits) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }
            row.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt())
                setImageResource(R.drawable.ic_check_circle)
                setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            })
            row.addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (10 * density).toInt()
                }
                text = benefit
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 13.5f
            })
            container.addView(row)
        }
    }
}
