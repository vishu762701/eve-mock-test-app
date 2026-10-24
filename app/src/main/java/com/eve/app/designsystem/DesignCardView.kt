package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView

/** Solid token card; runtime ancestors include ViewPager2's internal RecyclerView. */
class DesignCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ShadowHost.releaseClipping(this)
        if (isClickable) TouchTargets.ensure(this)
    }
}
