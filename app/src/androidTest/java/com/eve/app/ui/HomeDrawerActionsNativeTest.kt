package com.eve.app.ui

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.GravityCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.home.HomeDrawerActions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDrawerActionsNativeTest {
    @Test fun allProductionRowsReceiveTapsAfterAvailabilityOverlayWasVisible() {
        val rows = mapOf(
            R.id.layoutDrawerProfile to "ProfileActivity",
            R.id.layoutDrawerPremium to "PremiumActivity",
            R.id.layoutDrawerPerformance to "PerformanceActivity",
            R.id.layoutDrawerLeaderboard to "LeaderboardActivity",
            R.id.layoutDrawerSyllabus to "SyllabusActivity",
            R.id.layoutDrawerTopic to "PracticeActivity",
            R.id.layoutDrawerPyq to "PyqActivity",
            R.id.layoutDrawerMistakes to "MistakesActivity",
            R.id.layoutDrawerSettings to "SettingsActivity"
        )
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var binding: ActivityMainBinding
            var destination: android.content.Intent? = null
            scenario.onActivity { activity ->
                binding = ActivityMainBinding.inflate(activity.layoutInflater)
                activity.setContentView(binding.root)
                HomeDrawerActions.bind(binding) { destination = it }
                binding.maintenanceOverlay.visibility = View.VISIBLE
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                binding.maintenanceOverlay.visibility = View.GONE
                binding.drawerLayout.openDrawer(GravityCompat.START, false)
            }
            for ((id, target) in rows) {
                onView(withId(id)).perform(scrollTo(), click())
                scenario.onActivity {
                    assertEquals(target, destination?.component?.shortClassName?.substringAfterLast('.'))
                    destination = null
                    assertTrue(binding.root.findViewById<View>(id).performAccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK, null))
                    assertEquals(target, destination?.component?.shortClassName?.substringAfterLast('.'))
                }
            }
            scenario.onActivity {
                binding.ivDrawerAvatar.performClick()
                assertTrue(destination?.component?.className?.endsWith("ProfileActivity") == true)
                binding.drawerLayout.closeDrawer(GravityCompat.START, false)
                assertFalse(binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                binding.drawerLayout.openDrawer(GravityCompat.START, false)
                assertTrue(binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            }
        }
    }
}
