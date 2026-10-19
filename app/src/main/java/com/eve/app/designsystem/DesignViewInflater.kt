package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import com.google.android.material.theme.MaterialComponentsViewInflater

/** Preserve Material's inflater, adding shared card-host behavior to existing XML. */
@Keep
class DesignViewInflater : MaterialComponentsViewInflater() {
    override fun createImageView(context: Context, attrs: AttributeSet): DesignImageView = DesignImageView(context, attrs)
    override fun createImageButton(context: Context, attrs: AttributeSet): DesignImageButton = DesignImageButton(context, attrs)

    override fun createView(context: Context, name: String, attrs: AttributeSet): View? = when (name) {
        "com.google.android.material.card.MaterialCardView", "androidx.cardview.widget.CardView" -> DesignCardView(context, attrs)
        "LinearLayout", "android.widget.LinearLayout" -> DesignLinearLayout(context, attrs)
        "FrameLayout", "android.widget.FrameLayout" -> DesignFrameLayout(context, attrs)
        else -> super.createView(context, name, attrs)
    }
}
