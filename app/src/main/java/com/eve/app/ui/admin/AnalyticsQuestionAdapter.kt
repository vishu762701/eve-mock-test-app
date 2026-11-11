package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.databinding.ItemAnalyticsQuestionBinding
import java.util.Locale

class AnalyticsQuestionAdapter(
    private val onEdit: ((QuestionAnalytics) -> Unit)? = null,
    private val canEdit: () -> Boolean = { true }
) : RecyclerView.Adapter<AnalyticsQuestionAdapter.VH>() {
    private var items: List<QuestionAnalytics> = emptyList()
    fun submit(value: List<QuestionAnalytics>) { items = value; notifyDataSetChanged() }
    inner class VH(private val b: ItemAnalyticsQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(x: QuestionAnalytics) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            b.tvQuestion.text = "Q${x.questionNumber}. ${x.questionText}"
            b.tvQuestion.setTextColor(palette.textPrimary)
            val topic = if (x.topic.isBlank()) "" else " • ${x.topic}"
            b.tvExam.text = "${x.examName}$topic"
            b.tvExam.setTextColor(palette.textSecondary)
            b.tvStats.text = String.format(
                Locale.US, "%.1f%% wrong • %d wrong / %d attempted • %d unattempted",
                x.wrongRate, x.wrong, x.attempts, x.unattempted
            )
            b.tvStats.setTextColor(palette.textSecondary)
            b.root.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            b.btnEditQuestion.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            val allowed = onEdit != null && canEdit()
            b.btnEditQuestion.visibility = if (allowed) View.VISIBLE else View.GONE
            b.btnEditQuestion.isEnabled = allowed
            b.btnEditQuestion.setOnClickListener { if (canEdit()) onEdit?.invoke(x) }
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAnalyticsQuestionBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])
    override fun getItemCount() = items.size
}
