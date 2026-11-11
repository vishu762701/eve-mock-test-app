package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.databinding.ItemFlaggedQuestionBinding

class FlaggedQuestionsAdapter(
    private val onEdit: (AggregatedFlaggedQuestion) -> Unit,
    private val onDismiss: (AggregatedFlaggedQuestion) -> Unit,
    private val canEdit: () -> Boolean = { true },
    private val canDismiss: () -> Boolean = { true }
) : RecyclerView.Adapter<FlaggedQuestionsAdapter.VH>() {

    private var items: List<AggregatedFlaggedQuestion> = emptyList()

    fun submit(newItems: List<AggregatedFlaggedQuestion>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemFlaggedQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AggregatedFlaggedQuestion) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            val density = b.root.resources.displayMetrics.density

            b.tvExamName.text = item.examName.ifBlank { "Exam ID: ${item.examId}" }
            b.tvExamName.setTextColor(palette.textPrimary)
            b.tvFlagCount.text = "🚩 ${item.flagCount} flag${if (item.flagCount > 1) "s" else ""}"
            b.tvFlagCount.setTextColor(palette.accent)
            b.tvQuestionText.text = item.questionText
            b.tvQuestionText.setTextColor(palette.textPrimary)

            if (item.reasons.isNotEmpty()) {
                b.tvReasons.visibility = View.VISIBLE
                b.tvReasons.text = "Reasons: ${item.reasons.joinToString(", ")}"
                b.tvReasons.setTextColor(palette.textSecondary)
            } else {
                b.tvReasons.visibility = View.GONE
            }

            if (item.comments.isNotEmpty()) {
                b.tvComments.visibility = View.VISIBLE
                val commentsFormatted = item.comments.joinToString("\n") { "• $it" }
                b.tvComments.text = "Student Feedback:\n$commentsFormatted"
                b.tvComments.setTextColor(palette.textSecondary)
            } else {
                b.tvComments.visibility = View.GONE
            }

            b.root.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            b.btnEditQuestion.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            b.btnDismissFlag.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)

            b.btnEditQuestion.visibility = if (canEdit()) View.VISIBLE else View.GONE
            b.btnEditQuestion.isEnabled = canEdit()
            b.btnDismissFlag.isEnabled = canDismiss()
            b.btnEditQuestion.setOnClickListener { if (canEdit()) onEdit(item) }
            b.btnDismissFlag.setOnClickListener { if (canDismiss()) onDismiss(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFlaggedQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.rippleColor = android.content.res.ColorStateList.valueOf(
            com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
