package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemCategoryHeaderBinding
import com.eve.app.databinding.ItemExamBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EXAM = 1

class ExamAdapter(
    private val onClick: (Exam) -> Unit,
    private val onLongClick: ((Exam, Boolean, View) -> Unit)? = null
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
        fun bind(exam: Exam, attempted: Boolean, isPinned: Boolean) {
            b.tvExamName.text = exam.examName
            b.tvExamTime.text = if (attempted) {
                b.root.context.getString(com.eve.app.R.string.exam_completed_view_history)
            } else {
                b.root.context.getString(
                    com.eve.app.R.string.exam_time_category,
                    exam.timeLimitMinutes,
                    exam.categoryOrOther
                )
            }
            com.eve.app.util.ExamImageHelper.loadExamImage(b.ivExamImage, exam.imageUrl)
            b.ivPinned.visibility = if (isPinned) View.VISIBLE else View.GONE
            b.root.isEnabled = !attempted
            b.root.alpha = if (attempted) 0.62f else 1f
            b.root.setOnClickListener(if (attempted) null else { { onClick(exam) } })

            b.root.setOnLongClickListener { view ->
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onLongClick?.invoke(exam, isPinned, view)
                true
            }
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
            is HomeListItem.ExamRow -> (holder as ExamVH).bind(item.exam, item.attempted, item.isPinned)
        }
    }

    override fun getItemCount() = items.size
}
