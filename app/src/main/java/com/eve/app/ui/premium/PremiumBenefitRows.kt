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
        container.removeAllViews()
        for (benefit in benefits) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = resources.getDimensionPixelSize(R.dimen.design_row_min)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.design_row_gap) }
            }
            row.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.design_icon_size), resources.getDimensionPixelSize(R.dimen.design_icon_size))
                setImageResource(R.drawable.ic_check_circle)
                setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
            })
            row.addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.design_card_padding)
                }
                text = benefit
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.BODY)
            })
            container.addView(row)
        }
    }
}
