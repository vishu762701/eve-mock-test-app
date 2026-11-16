package com.eve.app.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.designsystem.TelegramRadialProgressView
import com.eve.app.ui.profile.TelegramQrSceneView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

/**
 * Standardized non-blocking UI runner and animation-freeze helpers.
 *
 * Prevents Espresso / AndroidX idling synchronization deadlocks when continuous
 * animations (RLottieNative, TelegramRadialProgressView, QrEmojiPickerGrid,
 * ThemeFrameCoordinator) are running on the UI thread.
 */
object TestLooperHelper {

    /**
     * Executes [action] on the activity's main UI thread via [Activity.runOnUiThread]
     * without blocking on Espresso main-looper idle checks, and awaits completion.
     */
    fun <T : Activity> runOnActivityNonBlocking(
        activity: T,
        timeoutMs: Long = 15_000L,
        action: (T) -> Unit
    ) {
        val latch = CountDownLatch(1)
        var thrown: Throwable? = null
        activity.runOnUiThread {
            try {
                action(activity)
            } catch (t: Throwable) {
                thrown = t
            } finally {
                latch.countDown()
            }
        }
        assertTrue("Main looper action timed out after ${timeoutMs}ms", latch.await(timeoutMs, TimeUnit.MILLISECONDS))
        thrown?.let { throw it }
    }

    /**
     * Recursively freezes all known continuous native/framework animations in a view hierarchy
     * so that snapshot captures and looper idle synchronization never hang.
     */
    fun freezeAllAnimations(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                freezeAllAnimations(view.getChildAt(i))
            }
        }
        if (view is TelegramRadialProgressView) {
            view.freezeForStaticSnapshot()
        }
        if (view is TelegramQrSceneView) {
            view.pausePlayback()
        }
        if (view is LottieAnimationView) {
            view.cancelAnimation()
            view.progress = 1f
        }
    }
}
