package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemExamBinding

class ExamAdapter(
    private val onClick: (Exam) -> Unit
) : RecyclerView.Adapter<ExamAdapter.VH>() {

    private var items: List<Exam> = emptyList()

    fun submit(list: List<Exam>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemExamBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(exam: Exam) {
            b.tvExamName.text = exam.examName
            b.tvExamTime.text = "Time: ${exam.timeLimitMinutes} minutes"
            b.root.setOnClickListener { onClick(exam) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}
