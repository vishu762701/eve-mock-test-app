package com.eve.app.designsystem

import android.app.Dialog
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

/** Solid modal surfaces; callers keep their existing show/dismiss/behavior listeners. */
object DesignSurfaces {
    fun badge(view: android.widget.TextView, @androidx.annotation.DrawableRes background: Int) {
        view.setBackgroundResource(background)
        DesignType.apply(view, DesignType.Role.CHIP)
        view.minHeight = view.resources.getDimensionPixelSize(R.dimen.design_pill_height)
        val padding = view.resources.getDimensionPixelSize(R.dimen.design_pill_padding)
        view.setPaddingRelative(padding, 0, padding, 0)
        view.gravity = android.view.Gravity.CENTER
    }

    fun dialog(dialog: Dialog) {
        val window = dialog.window ?: return
        window.setBackgroundDrawableResource(R.drawable.design_surface_panel)
        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        if (Build.VERSION.SDK_INT >= 31) {
            window.setBackgroundBlurRadius(0)
            window.attributes = window.attributes.apply { blurBehindRadius = 0 }
        }
    }
    fun sheet(view: View) {
        val shape = ShapeAppearanceModel.builder(view.context, R.style.ShapeAppearance_Eve_Token_Sheet, 0).build()
        view.background = MaterialShapeDrawable(shape).apply {
            fillColor = ContextCompat.getColorStateList(view.context, R.color.surface)
        }
    }
}
