package com.eve.app.ui.result

import android.view.View
import com.eve.app.databinding.ActivityResultBinding
import com.google.android.material.tabs.TabLayout

/** Connect the approved segmented order to the existing Result content sections. */
object ResultTabs {
    fun bind(binding: ActivityResultBinding, onReviewSelected: () -> Unit) {
        val track = binding.tabLayoutResult
        var readableHeight = Math.round(44f * track.resources.displayMetrics.density)
        for (index in 0 until track.tabCount) {
            val tab = track.getTabAt(index) ?: continue
            val label = android.widget.TextView(track.context).apply {
                id = android.R.id.text1
                text = tab.text
                contentDescription = tab.text
                textSize = 13f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(track.tabTextColors)
                // Keep 13sp when wrapping; TabLayout's built-in label can auto-shrink.
            }
            tab.customView = label
            readableHeight = maxOf(readableHeight,
                kotlin.math.ceil(label.paint.fontSpacing * 2 + 8f * track.resources.displayMetrics.density).toInt())
        }
        track.layoutParams = track.layoutParams.apply { height = readableHeight }

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
