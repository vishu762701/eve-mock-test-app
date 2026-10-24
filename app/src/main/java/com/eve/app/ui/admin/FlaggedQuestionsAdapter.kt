package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.databinding.ItemFlaggedQuestionBinding

class FlaggedQuestionsAdapter(
    private val onEdit: (AggregatedFlaggedQuestion) -> Unit,
    private val onDismiss: (AggregatedFlaggedQuestion) -> Unit
) : RecyclerView.Adapter<FlaggedQuestionsAdapter.VH>() {

    private var items: List<AggregatedFlaggedQuestion> = emptyList()

    fun submit(newItems: List<AggregatedFlaggedQuestion>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemFlaggedQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AggregatedFlaggedQuestion) {
            b.tvExamName.text = item.examName.ifBlank { "Exam ID: ${item.examId}" }
            b.tvFlagCount.text = "🚩 ${item.flagCount} flag${if (item.flagCount > 1) "s" else ""}"
            b.tvQuestionText.text = item.questionText

            if (item.reasons.isNotEmpty()) {
                b.tvReasons.visibility = View.VISIBLE
                b.tvReasons.text = "Reasons: ${item.reasons.joinToString(", ")}"
            } else {
                b.tvReasons.visibility = View.GONE
            }

            if (item.comments.isNotEmpty()) {
                b.tvComments.visibility = View.VISIBLE
                val commentsFormatted = item.comments.joinToString("\n") { "• $it" }
                b.tvComments.text = "Student Feedback:\n$commentsFormatted"
            } else {
                b.tvComments.visibility = View.GONE
            }

            b.btnEditQuestion.setOnClickListener { onEdit(item) }
            b.btnDismissFlag.setOnClickListener { onDismiss(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFlaggedQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
