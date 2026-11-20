package com.eve.app.ui.result

import android.view.View
import com.eve.app.databinding.ActivityResultBinding

/** Connect the approved segmented order to the Result view-tree content sections. */
object ResultTabs {
    fun bind(binding: ResultTelegramViewTree, onReviewSelected: () -> Unit) {
        val track = binding.tabLayoutResult
        val scrollable = track.resources.configuration.fontScale > 1.2f && track.resources.configuration.screenWidthDp < 600
        track.tabMode = if (scrollable) com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE else com.google.android.material.tabs.TabLayout.MODE_FIXED
        track.tabGravity = if (scrollable) com.google.android.material.tabs.TabLayout.GRAVITY_START else com.google.android.material.tabs.TabLayout.GRAVITY_FILL
        track.layoutParams = track.layoutParams.apply {
            height = track.resources.getDimensionPixelSize(com.eve.app.R.dimen.design_tab_height)
        }
        track.addTab("Overview")
        track.addTab("Review")
        track.addTab("Leaderboard")

        fun show(position: Int) {
            val section = ResultSection.fromTab(position)
            binding.sectionReview.visibility = if (section == ResultSection.REVIEW) View.VISIBLE else View.GONE
            binding.scrollResultContent.visibility = if (section == ResultSection.OVERVIEW) View.VISIBLE else View.GONE
            binding.scrollLeaderboard.visibility = if (section == ResultSection.LEADERBOARD) View.VISIBLE else View.GONE
            if (section == ResultSection.REVIEW) onReviewSelected()
        }
        track.onTabSelectedListener = { position -> show(position) }
        show(track.selectedTabPosition)
    }
    /** Backward-compatible presenter for XML-backed UI tests and legacy callers. */
    fun bind(binding: ActivityResultBinding, onReviewSelected: () -> Unit) {
        val track = binding.tabLayoutResult
        val scrollable = track.resources.configuration.fontScale > 1.2f && track.resources.configuration.screenWidthDp < 600
        track.tabMode = if (scrollable) com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE else com.google.android.material.tabs.TabLayout.MODE_FIXED
        track.tabGravity = if (scrollable) com.google.android.material.tabs.TabLayout.GRAVITY_START else com.google.android.material.tabs.TabLayout.GRAVITY_FILL
        track.layoutParams = track.layoutParams.apply {
            height = track.resources.getDimensionPixelSize(com.eve.app.R.dimen.design_tab_height)
        }
        track.addTab("Overview")
        track.addTab("Review")
        track.addTab("Leaderboard")

        com.eve.app.designsystem.ShadowHost.reserveTabStrip(track, binding.scrollResultContent)
        com.eve.app.designsystem.ShadowHost.reserveTabStrip(track, binding.scrollLeaderboard)
        com.eve.app.designsystem.ShadowHost.reserveTabStrip(track, binding.sectionReview)

        fun show(position: Int) {
            val section = ResultSection.fromTab(position)
            binding.sectionReview.visibility = if (section == ResultSection.REVIEW) View.VISIBLE else View.GONE
            binding.scrollResultContent.visibility = if (section == ResultSection.OVERVIEW) View.VISIBLE else View.GONE
            binding.scrollLeaderboard.visibility = if (section == ResultSection.LEADERBOARD) View.VISIBLE else View.GONE
            if (section == ResultSection.REVIEW) onReviewSelected()
        }
        track.onTabSelectedListener = { position -> show(position) }
        show(track.selectedTabPosition)
    }

}
