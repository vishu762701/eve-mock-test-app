package com.eve.app.ui.home

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import com.eve.app.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
class HomeSevenActionPopupTest {
    @Test fun sixDestinationRowsDismissBeforeOneCallbackAndThemeRemainsAccessible() {
        val controller=Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Eve)
        controller.setup().visible()
        val activity=controller.get()
        Settings.Global.putFloat(activity.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,0f)
        val host=FrameLayout(activity)
        val anchor=View(activity)
        host.addView(anchor,FrameLayout.LayoutParams(48,48))
        activity.setContentView(host)
        host.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
        host.layout(0,0,400,800)
        var popup:TelegramMenuPopup?=null
        try {
            val ids=listOf(R.id.menuRowHistory,R.id.menuRowBookmarks,R.id.menuRowMistakes,R.id.menuRowTopic,R.id.menuRowPyq,R.id.menuRowLogout)
            for ((index,id) in ids.withIndex()) {
                val events=mutableListOf<Int>()
                fun action(n:Int) { assertFalse(popup!!.isShowing); events+=n }
                popup=TelegramMenuPopup(activity,{_,_,_,_->}, {action(0)}, {action(1)}, {action(2)}, {action(3)}, {action(4)}, {action(5)})
                popup.show(anchor)
                val root=popup.contentView
                assertTrue(root.findViewById<View>(R.id.cardTheme).isClickable)
                ids.forEach { assertTrue(root.findViewById<View>(it).isClickable) }
                root.findViewById<View>(id).performClick()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
                assertEquals(listOf(index),events)
                root.findViewById<View>(id).performClick()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
                assertEquals("A detached stale row must not navigate twice",listOf(index),events)
                popup.dismissForHostTeardown()
            }
        } finally { popup?.dismissForHostTeardown(); controller.pause().stop().destroy() }
    }
}
