package com.eve.app.ui.result

import android.view.View
import com.eve.app.databinding.ActivityResultBinding
import com.google.android.material.tabs.TabLayout

/** Connect the approved segmented order to the existing Result content sections. */
object ResultTabs {
    fun bind(binding: ActivityResultBinding, onReviewSelected: () -> Unit) {
        fun show(position: Int) {
            val section = ResultSection.fromTab(position)
            binding.sectionReview.visibility = if (section == ResultSection.REVIEW) View.VISIBLE else View.GONE
            binding.scrollResultContent.visibility = if (section == ResultSection.OVERVIEW) View.VISIBLE else View.GONE
            binding.scrollLeaderboard.visibility = if (section == ResultSection.LEADERBOARD) View.VISIBLE else View.GONE
            if (section == ResultSection.REVIEW) onReviewSelected()
        }
        binding.tabLayoutResult.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { show(tab?.position ?: 0) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        show(binding.tabLayoutResult.selectedTabPosition)
    }
}
