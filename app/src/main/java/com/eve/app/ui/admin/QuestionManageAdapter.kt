package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemManageQuestionBinding

class QuestionManageAdapter(
    private val onEdit: (Question) -> Unit,
    private val onDelete: (Question) -> Unit
) : RecyclerView.Adapter<QuestionManageAdapter.VH>() {

    private var items: List<Question> = emptyList()

    fun submit(list: List<Question>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemManageQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(q: Question, position: Int) {
            b.tvQuestionText.text = "Q${position + 1}. ${q.questionText}"
            b.tvCorrect.text = "Correct: ${q.correctAnswer}. ${q.optionText(q.correctAnswer)}"
            b.btnEdit.setOnClickListener { onEdit(q) }
            b.btnDelete.setOnClickListener { onDelete(q) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemManageQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    override fun getItemCount() = items.size
}
