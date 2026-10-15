package com.eve.app.util

import android.content.res.Resources
import com.eve.app.R

/** Eve's Android geometry interpretation; these dp choices are not Apple specifications. */
object EveShapes {
    fun concentric(parentDp: Float, paddingDp: Float): Float =
        (parentDp - paddingDp).coerceAtLeast(4f)

    fun concentric(resources: Resources, parent: Int, padding: Int): Float {
        val density = resources.displayMetrics.density
        return concentric(resources.getDimension(parent) / density,
            resources.getDimension(padding) / density) * density
    }

    fun card(resources: Resources): Float = resources.getDimension(R.dimen.eve_radius_card)
    fun capsule(heightPx: Float): Float = heightPx / 2f
}
