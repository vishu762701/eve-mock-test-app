package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.databinding.ItemAnalyticsQuestionBinding
import java.util.Locale

class AnalyticsQuestionAdapter : RecyclerView.Adapter<AnalyticsQuestionAdapter.VH>() {
    private var items: List<QuestionAnalytics> = emptyList()
    fun submit(value: List<QuestionAnalytics>) { items = value; notifyDataSetChanged() }
    inner class VH(private val b: ItemAnalyticsQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(x: QuestionAnalytics) {
            b.tvQuestion.text = "Q${x.questionNumber}. ${x.questionText}"
            val topic = if (x.topic.isBlank()) "" else " • ${x.topic}"
            b.tvExam.text = "${x.examName}$topic"
            b.tvStats.text = String.format(
                Locale.US, "%.1f%% wrong • %d wrong / %d attempted • %d unattempted",
                x.wrongRate, x.wrong, x.attempts, x.unattempted
            )
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAnalyticsQuestionBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size
}
