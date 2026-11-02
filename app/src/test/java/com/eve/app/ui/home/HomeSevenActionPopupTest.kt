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
    private fun exercise(fontScale: Float = 1f) {
        org.robolectric.RuntimeEnvironment.setFontScale(fontScale)
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
                val labels=listOf(R.string.home_menu_history,R.string.home_menu_bookmarks,R.string.mistake_notebook,R.string.home_menu_topic_test,R.string.home_menu_pyq,R.string.home_menu_logout)
                ids.zip(labels).forEach { (rowId,labelId) ->
                    val row=root.findViewById<View>(rowId)
                    assertTrue(row.isClickable)
                    assertEquals(activity.getString(labelId),row.contentDescription)
                }
                assertTrue(root.findViewById<View>(R.id.cardTheme).contentDescription in
                    listOf(activity.getString(R.string.home_menu_day_mode),activity.getString(R.string.home_menu_dark_mode)))
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
    @Test fun realRowsDismissBeforeOneCallbackAndRejectStaleClicks() = exercise()
    @Test @Config(qualifiers="night") fun nightMenuRetainsLabelsAndCallbacks() = exercise()
    @Test @Config(qualifiers="hi-rIN-ldrtl") fun hindiRtlLargeFontKeepsFullAccessibleLabels() = exercise(2f)

}
