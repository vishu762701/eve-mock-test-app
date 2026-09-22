package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemCategoryHeaderBinding
import com.eve.app.databinding.ItemExamBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EXAM = 1

class ExamAdapter(
    private val onClick: (Exam) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<HomeListItem> = emptyList()

    fun submit(list: List<HomeListItem>) {
        items = list
        notifyDataSetChanged()
    }

    inner class HeaderVH(private val b: ItemCategoryHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(header: HomeListItem.Header) {
            b.tvCategoryHeader.text = header.title
        }
    }

    inner class ExamVH(private val b: ItemExamBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(exam: Exam) {
            b.tvExamName.text = exam.examName
            b.tvExamTime.text = "Time: ${exam.timeLimitMinutes} minutes  •  ${exam.categoryOrOther}"
            b.root.setOnClickListener { onClick(exam) }
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HomeListItem.Header -> VIEW_TYPE_HEADER
        is HomeListItem.ExamRow -> VIEW_TYPE_EXAM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderVH(ItemCategoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            ExamVH(ItemExamBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HomeListItem.Header -> (holder as HeaderVH).bind(item)
            is HomeListItem.ExamRow -> (holder as ExamVH).bind(item.exam)
        }
    }

    override fun getItemCount() = items.size
}
