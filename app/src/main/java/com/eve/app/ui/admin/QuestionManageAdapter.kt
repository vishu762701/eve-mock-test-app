package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemManageQuestionBinding

class QuestionManageAdapter(
    private val onEdit: (Question) -> Unit,
    private val onDelete: (Question) -> Unit,
    private val canManage: () -> Boolean = { true }
) : RecyclerView.Adapter<QuestionManageAdapter.VH>() {

    private var items: List<Question> = emptyList()

    fun submit(list: List<Question>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemManageQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(q: Question, position: Int) {
            b.tvQuestionText.text = "Q${position + 1}. ${q.questionText}"
            val topicSuffix = if (q.topic.isNotBlank()) "  •  Topic: ${q.topic}" else ""
            val pyqSuffix = if (q.isPyq && q.pyqYear > 0) {
                val paper = if (q.pyqPaper.isBlank()) "" else " ${q.pyqPaper}"
                "  •  PYQ ${q.pyqYear}$paper"
            } else ""
            val allowed = canManage()
            val answer = if (allowed && q.correctAnswer.isNotBlank())
                "Correct: ${q.correctAnswer}. ${q.optionText(q.correctAnswer)}" else ""
            b.tvCorrect.text = "$answer$topicSuffix$pyqSuffix".trim().removePrefix("•").trim()
            b.tvCorrect.visibility = if (b.tvCorrect.text.isEmpty()) View.GONE else View.VISIBLE
            b.btnEdit.visibility = if (allowed) View.VISIBLE else View.GONE
            b.btnDelete.visibility = if (allowed) View.VISIBLE else View.GONE
            b.btnEdit.isEnabled = allowed
            b.btnDelete.isEnabled = allowed
            b.btnEdit.setOnClickListener { if (canManage()) onEdit(q) }
            b.btnDelete.setOnClickListener { if (canManage()) onDelete(q) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemManageQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    override fun getItemCount() = items.size
}
