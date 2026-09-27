package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemExistingExamAdminBinding

data class ExamWithStats(
    val exam: Exam,
    val attemptCount: Long = 0L,
    val questionCount: Int = exam.questionCount
)

class ExistingExamsAdapter(
    private val onExamClick: (Exam) -> Unit,
    private val onEditClick: (Exam) -> Unit,
    private val onPreviewClick: (Exam) -> Unit,
    private val onDeleteClick: (Exam) -> Unit
) : ListAdapter<ExamWithStats, ExistingExamsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemExistingExamAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExamWithStats) {
            val exam = item.exam
            binding.tvExamName.text = exam.examName
            binding.tvCategoryBadge.text = exam.categoryOrOther
            binding.tvTimeLimit.text = "⏱️ ${exam.timeLimitMinutes} mins"
            val qCount = if (item.questionCount > 0) item.questionCount else exam.questionCount
            binding.tvQuestionCount.text = "📝 $qCount Qs"
            binding.tvAttemptCount.text = "📊 ${item.attemptCount} student attempts"

            binding.root.setOnClickListener { onExamClick(exam) }
            binding.btnQuickEdit.setOnClickListener { onEditClick(exam) }
            binding.btnQuickPreview.setOnClickListener { onPreviewClick(exam) }

            binding.btnMoreOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "✏️ Edit Exam")
                popup.menu.add(0, 2, 1, "👁️ Preview (Student View)")
                popup.menu.add(0, 3, 2, "🗑️ Delete Exam")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> {
                            onEditClick(exam)
                            true
                        }
                        2 -> {
                            onPreviewClick(exam)
                            true
                        }
                        3 -> {
                            onDeleteClick(exam)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExistingExamAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExamWithStats>() {
        override fun areItemsTheSame(oldItem: ExamWithStats, newItem: ExamWithStats) =
            oldItem.exam.id == newItem.exam.id

        override fun areContentsTheSame(oldItem: ExamWithStats, newItem: ExamWithStats) =
            oldItem == newItem
    }
}
