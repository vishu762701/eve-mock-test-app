package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.databinding.ItemAnalyticsExamBinding
import java.util.Locale

class AnalyticsExamAdapter : RecyclerView.Adapter<AnalyticsExamAdapter.VH>() {
    private var items: List<ExamAnalytics> = emptyList()

    fun submit(value: List<ExamAnalytics>) {
        items = value
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAnalyticsExamBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(x: ExamAnalytics) {
            val ctx = b.root.context
            b.tvExam.text = x.examName
            b.tvCategoryBadge.text = x.category.ifBlank { "General" }

            val avgScoreText = if (x.averageScore > 0) {
                "Avg Score: ${String.format(Locale.US, "%.1f", x.averageScore)}"
            } else {
                "Avg Score: —"
            }
            b.tvMeta.text = "Attempts: ${x.attemptCount}  •  $avgScoreText"
            b.tvStudents.text = "${x.uniqueUsers} students"

            when {
                x.attemptCount == 0L -> {
                    b.tvEngagementBadge.visibility = View.VISIBLE
                    b.tvEngagementBadge.text = "⚠️ 0 Attempts - Needs Promotion"
                    b.tvEngagementBadge.setTextColor(ContextCompat.getColor(ctx, R.color.eve_red))
                }
                x.attemptCount < 5L -> {
                    b.tvEngagementBadge.visibility = View.VISIBLE
                    b.tvEngagementBadge.text = "⚠️ Low Engagement (<5 attempts)"
                    b.tvEngagementBadge.setTextColor(ContextCompat.getColor(ctx, R.color.eve_amber))
                }
                else -> {
                    b.tvEngagementBadge.visibility = View.VISIBLE
                    b.tvEngagementBadge.text = "✅ Active Engagement"
                    b.tvEngagementBadge.setTextColor(ContextCompat.getColor(ctx, R.color.eve_green))
                }
            }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAnalyticsExamBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

    override fun getItemCount() = items.size
}
