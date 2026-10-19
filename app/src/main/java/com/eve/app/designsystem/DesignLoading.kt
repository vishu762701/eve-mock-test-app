package com.eve.app.designsystem

import android.view.View

/** Publish the existing loaded state without retaining an old placeholder fade or end action. */
object DesignLoading {
    fun showContent(loading: View, content: View) {
        loading.animate().withEndAction(null).cancel()
        content.animate().withEndAction(null).cancel()
        loading.visibility = View.GONE
        loading.alpha = 1f
        content.visibility = View.VISIBLE
        content.alpha = 1f
    }
}
