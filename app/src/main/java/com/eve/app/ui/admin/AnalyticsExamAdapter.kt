package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.ExamAnalytics
import com.eve.app.databinding.ItemAnalyticsExamBinding

class AnalyticsExamAdapter : RecyclerView.Adapter<AnalyticsExamAdapter.VH>() {
    private var items: List<ExamAnalytics> = emptyList()
    fun submit(value: List<ExamAnalytics>) { items = value; notifyDataSetChanged() }
    inner class VH(private val b: ItemAnalyticsExamBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(x: ExamAnalytics) {
            b.tvExam.text = x.examName
            b.tvMeta.text = if (x.category.isBlank()) "${x.attemptCount} attempts" else "${x.category} • ${x.attemptCount} attempts"
            b.tvStudents.text = "${x.uniqueUsers} unique students"
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAnalyticsExamBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size
}
