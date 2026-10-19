package com.eve.app.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat

/** Shared slide/fade options; activity destinations, request codes and back-stack semantics stay native. */
object EveNavigationHelper {

    fun navigate(
        activity: Activity,
        intent: Intent,
        sourceView: View? = null,
        requestCode: Int? = null
    ) {
        // The source supplies only its resource context, never a competing scale animation.
        // WindowManager applies the system transition animation scale to these resources.
        val animationContext = sourceView?.context ?: activity
        val optionsBundle: Bundle? = ActivityOptionsCompat.makeCustomAnimation(
            animationContext, com.eve.app.R.anim.design_screen_enter,
            com.eve.app.R.anim.design_screen_recede
        ).toBundle()

        if (requestCode != null) {
            ActivityCompat.startActivityForResult(activity, intent, requestCode, optionsBundle)
        } else {
            ActivityCompat.startActivity(activity, intent, optionsBundle)
        }
    }

    /**
     * Native back navigation uses the matching shared close animations.
     */
    fun finishWithTransition(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
    }
}
