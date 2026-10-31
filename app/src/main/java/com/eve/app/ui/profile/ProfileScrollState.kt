package com.eve.app.ui.profile

/** EVE NestedScrollView integration, source profile avatar collapse range: 100dp. */
internal object ProfileScrollState {
    fun progress(scrollY: Int, density: Float): Float {
        require(density > 0f)
        return (scrollY.toFloat() / (100f * density)).coerceIn(0f, 1f)
    }
}
