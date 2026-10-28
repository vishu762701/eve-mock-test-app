package com.eve.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.ui.home.HomeAppearance
import com.eve.app.ui.home.HomeCategoryChipGroup
import com.google.android.material.chip.Chip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Native regression: taps must never leave Home with zero or two checked categories. */
@RunWith(AndroidJUnit4::class)
class HomeCategorySelectionNativeTest {
    @Test
    fun repeatedAllAndRapidSwitchesAlwaysKeepOneSelectedCategory() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContentView(R.layout.activity_main)
                val group = activity.findViewById<HomeCategoryChipGroup>(R.id.chipGroupCategory)
                group.removeAllViews()
                val all = HomeAppearance.categoryChip(activity, "All", "All") {}
                val other = HomeAppearance.categoryChip(activity, "Other", "All") {}
                group.addView(all)
                group.addView(other)

                fun assertSingle(expected: Chip) {
                    assertEquals("Exactly one category must remain checked",
                        1, listOf(all, other).count { it.isChecked })
                    assertTrue("Wrong selected category", expected.isChecked)
                }

                assertTrue("Selection-required group must be active", group.isSelectionRequired)
                assertSingle(all)
                repeat(10) {
                    all.performClick()
                    assertSingle(all)
                }
                repeat(10) {
                    other.performClick()
                    assertSingle(other)
                    all.performClick()
                    assertSingle(all)
                }
                group.removeView(other)
                assertSingle(all)
            }
        }
    }
}
