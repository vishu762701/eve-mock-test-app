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
    private fun bulletinCount(view: View): Int {
        val own = if (view.id == R.id.cardBulletin) 1 else 0
        return own + if (view is android.view.ViewGroup) {
            (0 until view.childCount).sumOf { bulletinCount(view.getChildAt(it)) }
        } else 0
    }

    @Test fun informationalBurstReplacesRatherThanStacks() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Eve)
        AppBulletin.make(activity, "first", 10000L).show()
        AppBulletin.make(activity, "second", 10000L).show()
        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        assertEquals(1, bulletinCount(content))
        assertEquals("second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
        controller.pause().stop().destroy()
    }

    @Test fun dismissDoesNotInvokeAnAction() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Eve)
        var calls = 0
        val bulletin = AppBulletin.make(activity, "message", 10000L, actionText = "Undo", onAction = { calls++ })
        bulletin.show()
        bulletin.dismiss()
        assertEquals(0, calls)
        controller.pause().stop().destroy()
    }

    @Test fun externallyRemovedOverlayCanBeRecreated() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Eve)
        AppBulletin.make(activity, "first", 10000L).show()
        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val overlay = content.findViewById<View>(R.id.bulletinContainer)
        content.removeView(overlay)
        AppBulletin.make(activity, "second", 10000L).show()
        assertEquals(1, bulletinCount(content))
        assertEquals("second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
        controller.pause().stop().destroy()
    }

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
        assertEquals(1, bulletinCount(content))
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
