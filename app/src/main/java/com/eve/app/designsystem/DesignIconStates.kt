package com.eve.app.designsystem

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.eve.app.R

/** Keep bookmark state visible when both states use the same outline icon family. */
object DesignIconStates {
    fun bookmark(view: ImageView, checked: Boolean) {
        view.setImageResource(R.drawable.ic_star_outline)
        view.isSelected = checked
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(
            if (checked) DesignTokens.accent(view.context) else DesignTokens.secondary(view.context)
        ))
        view.contentDescription = if (checked) "Remove bookmark" else "Bookmark question"
    }
}
