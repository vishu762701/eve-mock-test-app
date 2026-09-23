package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.DailyQuestion
import com.eve.app.databinding.ItemManageQuestionBinding

class DailyQuestionAdapter(
    private val onEdit: (DailyQuestion) -> Unit,
    private val onDelete: (DailyQuestion) -> Unit
) : RecyclerView.Adapter<DailyQuestionAdapter.VH>() {

    private var items: List<DailyQuestion> = emptyList()

    fun submit(list: List<DailyQuestion>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemManageQuestionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(q: DailyQuestion, position: Int) {
            b.tvQuestionText.text = "Q${position + 1}. ${q.questionText}"
            val topic = if (q.topic.isNotBlank()) "  •  ${q.topic}" else ""
            val correctText = when (q.correctAnswer) {
                "A" -> q.optionA
                "B" -> q.optionB
                "C" -> q.optionC
                "D" -> q.optionD
                else -> ""
            }
            b.tvCorrect.text = "Correct: ${q.correctAnswer}. $correctText$topic"
            b.btnEdit.setOnClickListener { onEdit(q) }
            b.btnDelete.setOnClickListener { onDelete(q) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemManageQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

    override fun getItemCount() = items.size
}
