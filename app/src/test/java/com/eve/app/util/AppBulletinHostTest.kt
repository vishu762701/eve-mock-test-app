package com.eve.app.util

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.eve.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBulletinHostTest {
    @Test fun actionableMessagesArePresentedOneAtATime() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Eve)
        var calls = 0
        val first = AppBulletin.make(activity, "first", 10000L, actionText = "Undo", onAction = { calls++ })
        val second = AppBulletin.make(activity, "second", 10000L, actionText = "Undo", onAction = { calls++ })
        first.show()
        second.show()
        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        assertNotNull(content.findViewById<View>(R.id.cardBulletin))
        assertEquals("first", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
        content.findViewById<View>(R.id.btnBulletinAction).performClick()
        shadowOf(android.os.Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        assertEquals(1, calls)
        assertEquals("second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
        second.dismiss()
        shadowOf(android.os.Looper.getMainLooper()).idleFor(300, TimeUnit.MILLISECONDS)
        assertEquals(1, calls)
        controller.pause().stop().destroy()
    }
}
