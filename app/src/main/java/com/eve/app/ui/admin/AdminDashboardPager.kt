package com.eve.app.ui.admin

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.view.children
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.eve.app.R
import com.eve.app.telegram.components.PagerSlidingTabStrip
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.admin.tg.HeaderCell
import com.eve.app.ui.admin.tg.ShadowSectionCell

/** EVE's five existing admin sections, hosted by the upstream Telegram pager tab renderer. */
internal class AdminDashboardPager(
    private val host: FrameLayout,
    selectedSection: Int
) {
    private val context: Context = host.context
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density + .5f).toInt()
    private val names = context.resources.getStringArray(R.array.admin_dashboard_sections)
    private val sections = ArrayList<LinearLayout>()
    private val headerIds = listOf(R.id.headerContent, R.id.headerPeople,
        R.id.headerCommunication, R.id.headerInsights, R.id.headerSystem)
    private var visibleSections = headerIds.indices.toList()
    private val pager = ViewPager(context).apply { id = R.id.adminSectionsPager }
    private val tabs = PagerSlidingTabStrip(context) {
        TelegramThemeEngine.resolveCurrentPalette(context).textPrimary
    }
    val currentSection: Int get() = visibleSections.getOrElse(pager.currentItem) { 0 }

    private val adapter = object : PagerAdapter() {
        override fun getCount() = visibleSections.size
        override fun getPageTitle(position: Int) = names[visibleSections[position]]
        override fun isViewFromObject(view: View, item: Any) = view === item
        override fun getItemPosition(item: Any) = POSITION_NONE
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val content = sections[visibleSections[position]]
            (content.parent as? ViewGroup)?.removeView(content)
            return ScrollView(context).apply {
                isFillViewport = true
                addView(content, FrameLayout.LayoutParams(-1, -2))
                container.addView(this, ViewGroup.LayoutParams(-1, -1))
            }
        }
        override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
            (item as ScrollView).removeAllViews()
            container.removeView(item)
        }
    }


    init {
        val oldContent = host.getChildAt(0) as LinearLayout
        val children = oldContent.children.toList()
        oldContent.removeAllViews()
        host.removeAllViews()
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        host.addView(layout, FrameLayout.LayoutParams(-1, -1))
        var section: LinearLayout? = null
        for (child in children) {
            if (child.id in headerIds) {
                section = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 0, 0, dp(32))
                }
                sections.add(section)
            }
            if (section == null) layout.addView(child) else section.addView(child)
        }
        check(sections.size == headerIds.size) { "Admin section inventory is incomplete" }
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        tabs.setIndicatorColor(androidx.core.graphics.ColorUtils.setAlphaComponent(palette.accent, 31))
        tabs.setUnderlineHeight(0)
        tabs.setPadding(0, dp(6), 0, dp(6))
        layout.addView(tabs, LinearLayout.LayoutParams(-1, dp(48)))
        layout.addView(pager, LinearLayout.LayoutParams(-1, 0, 1f))
        pager.adapter = adapter
        tabs.setViewPager(pager)
        pager.setCurrentItem(selectedSection.coerceIn(0, sections.lastIndex), false)
    }


    fun refreshAccess() {
        val previous = currentSection
        visibleSections = sections.indices.filter { index ->
            sections[index].children.any {
                it.visibility == View.VISIBLE && it !is HeaderCell && it !is ShadowSectionCell
            }
        }
        adapter.notifyDataSetChanged()
        tabs.notifyDataSetChanged()
        if (visibleSections.isNotEmpty()) pager.setCurrentItem(visibleSections.indexOf(previous).coerceAtLeast(0), false)
        tabs.visibility = if (visibleSections.size > 1) View.VISIBLE else View.GONE
    }

    fun refreshTheme() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        tabs.setIndicatorColor(androidx.core.graphics.ColorUtils.setAlphaComponent(palette.accent, 31))
        tabs.notifyDataSetChanged()
    }
}
