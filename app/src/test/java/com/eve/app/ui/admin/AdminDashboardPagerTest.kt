package com.eve.app.ui.admin

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.eve.app.R
import com.eve.app.databinding.ActivityAdminBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AdminDashboardPagerTest {
    @Test fun allFiveSectionsKeepTheirActualExistingActionsAndSelection() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val context = controller.get().apply { setTheme(R.style.Theme_Eve) }
        val binding = ActivityAdminBinding.inflate(LayoutInflater.from(context))
        context.setContentView(binding.root)
        var clicks = 0
        binding.rowExams.setOnClickListener { clicks++ }
        binding.rowUsers.setOnClickListener { clicks++ }
        binding.rowPolls.setOnClickListener { clicks++ }
        binding.rowAnalytics.setOnClickListener { clicks++ }
        binding.rowAppConfig.setOnClickListener { clicks++ }
        val dashboard = AdminDashboardPager(binding.scrollAdminHub, 2)
        binding.root.measure(View.MeasureSpec.makeMeasureSpec(440, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
        binding.root.layout(0, 0, 440, 900)
        val pager = binding.root.findViewById<ViewPager>(R.id.adminSectionsPager)
        assertEquals(5, pager.adapter!!.count)
        assertEquals(2, dashboard.currentSection)
        for ((index, action) in listOf(binding.rowExams, binding.rowUsers, binding.rowPolls,
                binding.rowAnalytics, binding.rowAppConfig).withIndex()) {
            pager.setCurrentItem(index, false)
            binding.root.measure(View.MeasureSpec.makeMeasureSpec(440, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
            binding.root.layout(0, 0, 440, 900)
            assertSame(action, pager.findViewById<View>(action.id))
            assertTrue(action.performClick())
        }
        assertEquals(5, clicks)
        assertSame(binding.rowAppConfig, pager.findViewById<View>(R.id.rowAppConfig))
        dashboard.refreshAccess()
        assertEquals(4, dashboard.currentSection)
        controller.pause().stop().destroy()
    }
}
