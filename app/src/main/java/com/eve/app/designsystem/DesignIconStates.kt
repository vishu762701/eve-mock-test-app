package com.eve.app.designsystem

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.eve.app.R

/** Use a distinct filled star for saved questions and outline for unsaved ones. */
object DesignIconStates {
    fun bookmark(view: ImageView, checked: Boolean) {
        view.setImageResource(if (checked) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        view.isSelected = checked
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(
            if (checked) androidx.core.content.ContextCompat.getColor(view.context, R.color.accent_icon) else DesignTokens.secondary(view.context)
        ))
        view.contentDescription = if (checked) "Remove bookmark" else "Bookmark question"
    }
}
