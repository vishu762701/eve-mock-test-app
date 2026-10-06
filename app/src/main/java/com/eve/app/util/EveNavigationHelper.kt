package com.eve.app.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat

/**
 * iOS 26-inspired contextual and fluid navigation system (Part F).
 * Provides source-aware contextual scaling/expansion from tap origin when a source element
 * exists, and falls back to fluid scale/fade window transitions when no specific source is provided.
 */
object EveNavigationHelper {

    fun navigate(
        activity: Activity,
        intent: Intent,
        sourceView: View? = null,
        requestCode: Int? = null
    ) {
        val optionsBundle: Bundle? = if (EveMotionHelper.areAnimationsEnabled(activity) &&
            sourceView != null && sourceView.isAttachedToWindow && sourceView.width > 0 && sourceView.height > 0
        ) {
            try {
                ActivityOptionsCompat.makeScaleUpAnimation(
                    sourceView,
                    0,
                    0,
                    sourceView.width,
                    sourceView.height
                ).toBundle()
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }

        if (requestCode != null) {
            ActivityCompat.startActivityForResult(activity, intent, requestCode, optionsBundle)
        } else {
            ActivityCompat.startActivity(activity, intent, optionsBundle)
        }
    }

    /**
     * Contextual reverse back navigation returning smoothly toward the source origin.
     */
    fun finishWithTransition(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
    }
}
