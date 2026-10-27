package com.eve.app.ui

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.util.AppBulletin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the production overlay in a real attached activity, not an XML-only fixture. */
@RunWith(AndroidJUnit4::class)
class BulletinHostNativeTest {
    @Test fun actionableBurstKeepsOneVisibleAndInvokesOnlyClickedAction() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var actions = 0
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.Theme_Eve)
                activity.setContentView(FrameLayout(activity))
                AppBulletin.make(activity, "पहला परीक्षण संदेश", 10000L, actionText = "Undo", onAction = { actions++ }).show()
                AppBulletin.make(activity, "Second", 10000L, actionText = "Undo", onAction = { actions++ }).show()
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(350)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val card = content.findViewById<View>(R.id.cardBulletin)
                assertNotNull(card)
                assertTrue(card.isShown && card.width > 0 && card.height > 0)
                assertEquals("पहला परीक्षण संदेश", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
                assertTrue(content.findViewById<View>(R.id.btnBulletinAction).performClick())
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(300)
            scenario.onActivity { activity ->
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertEquals(1, actions)
                assertEquals("Second", content.findViewById<TextView>(R.id.tvBulletinMessage).text.toString())
            }
        }
    }
}
