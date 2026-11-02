package com.eve.app.ui.profile

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

/** Actual scanner Activity callback cancellation, without starting CameraX or a network client. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class QrScannerPausedResultTest {
    private fun findFrame(view: View): QrScannerFrameOverlay? {
        if (view is QrScannerFrameOverlay) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findFrame(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    @Test fun pausingActualActivityCancelsPendingFoundNavigationAndAllowsFreshResumedResult() {
        val controller = Robolectric.buildActivity(EveQrLiveScannerActivity::class.java).setup()
        val activity = controller.get()
        try {
            // Setup can request CAMERA while RESUMED. Assert and consume that
            // existing OS request before observing intents produced after pause.
            generateSequence { shadowOf(activity).nextStartedActivity }.forEach { intent ->
                assertEquals("android.content.pm.action.REQUEST_PERMISSIONS", intent.action)
            }
            val overlay = requireNotNull(findFrame(activity.findViewById(android.R.id.content)))
            overlay.layout(0, 0, 640, 1000)
            val handle = EveQrLiveScannerActivity::class.java.getDeclaredMethod(
                "handleLink", String::class.java, FloatArray::class.java).apply { isAccessible = true }
            val pending = EveQrLiveScannerActivity::class.java.getDeclaredField("foundNavigation")
                .apply { isAccessible = true }
            handle.invoke(activity, "eve-mock-test://username/test_name",
                floatArrayOf(100f,100f,200f,100f,100f,200f,200f,200f))
            assertNotNull("Actual four-point path scheduled the source found delay", pending.get(activity))
            controller.pause()
            assertNull("Pause removed the actual pending runnable", pending.get(activity))
            shadowOf(Looper.getMainLooper()).idleFor(600, TimeUnit.MILLISECONDS)
            assertNull("No background navigation", shadowOf(activity).nextStartedActivity)
            assertFalse("Pause does not finish the EVE Gallery/Settings return host", activity.isFinishing)
            controller.resume()
            handle.invoke(activity, "eve-mock-test://username/test_name", null)
            shadowOf(Looper.getMainLooper()).idleFor(600, TimeUnit.MILLISECONDS)
            val launched = shadowOf(activity).nextStartedActivity
            assertNotNull("A fresh resumed result still navigates", launched)
            assertEquals(UsernameCardActivity::class.java.name, launched.component?.className)
            assertNull("Exactly one destination", shadowOf(activity).nextStartedActivity)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun rotationDoesNotLaunchAnotherPermissionWhileOriginalRequestIsPending() {
        val first = Robolectric.buildActivity(EveQrLiveScannerActivity::class.java).setup()
        val state = Bundle()
        shadowOf(Looper.getMainLooper()).idleFor(600, TimeUnit.MILLISECONDS)
        val request = shadowOf(first.get()).nextStartedActivity
        assertNotNull("Initial on-demand camera request", request)
        assertEquals("android.content.pm.action.REQUEST_PERMISSIONS", request.action)
        first.pause().saveInstanceState(state).stop().destroy()
        val restored = Robolectric.buildActivity(EveQrLiveScannerActivity::class.java)
            .create(state).start().resume()
        try {
            shadowOf(Looper.getMainLooper()).idleFor(600, TimeUnit.MILLISECONDS)
            assertTrue(state.getBoolean("camera_permission_pending"))
            assertNull("No duplicate system permission launch after restoration",
                shadowOf(restored.get()).nextStartedActivity)
        } finally { restored.pause().stop().destroy() }
    }

}
