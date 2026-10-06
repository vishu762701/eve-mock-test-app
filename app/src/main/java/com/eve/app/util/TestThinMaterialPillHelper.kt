package com.eve.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.eve.app.R
import eightbitlab.com.blurview.BlurView

/** Thin Material surfaces used only by Test answer options and the four Test actions. */
object TestThinMaterialPillHelper {

    fun attach(surface: BlurView, blurRoot: ViewGroup, context: Context, drawStroke: Boolean) {
        surface.setBackgroundColor(Color.TRANSPARENT)
        surface.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
            }
        }
        surface.clipToOutline = true
        EveBlurHelper.setupBlurView(
            surface,
            blurRoot,
            radiusDp = context.resources.getDimension(R.dimen.eve_thin_material_blur_radius) /
                context.resources.displayMetrics.density,
            overlayColor = ContextCompat.getColor(context, R.color.eve_material_thin)
        )
        setSelected(surface, false, context, drawStroke)
    }

    fun setSelected(surface: BlurView, selected: Boolean, context: Context, drawStroke: Boolean) {
        val cornerRadius = if (surface.height > 0) surface.height / 2f else 999f
        val overlay = if (selected) {
            ContextCompat.getColor(context, R.color.eve_test_option_selected_overlay)
        } else {
            Color.TRANSPARENT
        }
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(overlay)
            if (drawStroke) {
                setStroke(
                    1,
                    ContextCompat.getColor(context, R.color.eve_test_option_stroke)
                )
            }
        }
        surface.foreground = drawable
    }
}
