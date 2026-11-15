package com.eve.app.ui.result

import android.view.View
import com.eve.app.databinding.ActivityResultBinding

/** Connect the approved segmented order to the existing Result content sections. */
object ResultTabs {
    fun bind(binding: ActivityResultBinding, onReviewSelected: () -> Unit) {
        val track = binding.tabLayoutResult
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
        track.onTabSelectedListener = { position ->
            show(position)
        }
        show(track.selectedTabPosition)
    }
}
