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
                assertTrue("Home filter uses no width-changing selected check icon",
                    !all.isCheckedIconVisible && !other.isCheckedIconVisible)
                val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                    (320 * activity.resources.displayMetrics.density).toInt(),
                    android.view.View.MeasureSpec.AT_MOST
                )
                val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                    (80 * activity.resources.displayMetrics.density).toInt(),
                    android.view.View.MeasureSpec.AT_MOST
                )
                all.measure(widthSpec, heightSpec)
                val selectedWidth = all.measuredWidth
                all.isChecked = false
                all.measure(widthSpec, heightSpec)
                val unselectedWidth = all.measuredWidth
                all.isChecked = true
                assertEquals("Home category may not jump in width on select", selectedWidth, unselectedWidth)
                assertSingle(all)
                val isDark = com.eve.app.theme.TelegramThemeEngine.isDarkActive(activity)
                val activeBg = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                val activeFg = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                fun assertRenderedRoles(chip: Chip) {
                    val checked = intArrayOf(android.R.attr.state_checked)
                    assertEquals("Material chip selected fill cannot fall back to blue", activeBg,
                        chip.chipBackgroundColor?.getColorForState(checked, -1))
                    assertEquals("Selected label must have the opposite monochrome foreground", activeFg,
                        chip.textColors.getColorForState(checked, -1))
                }
                assertRenderedRoles(all)
                repeat(10) {
                    all.performClick()
                    assertSingle(all)
                }
                repeat(10) {
                    other.performClick()
                    assertSingle(other)
                    assertRenderedRoles(other)
                    all.performClick()
                    assertSingle(all)
                }
                group.removeView(other)
                assertSingle(all)
            }
        }
    }
}
