package com.eve.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.databinding.ItemCategoryHeaderBinding
import com.eve.app.databinding.ItemExamBinding
import com.eve.app.databinding.ItemFeedbackPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EXAM = 1
private const val VIEW_TYPE_FEEDBACK_POST = 2

class ExamAdapter(
    private val onClick: (Exam) -> Unit,
    private val onLongClick: ((Exam, Boolean, View) -> Unit)? = null,
    private val onFeedbackPostLongClick: ((FeedbackPost, View) -> Unit)? = null,
    private val onSendReply: ((post: FeedbackPost, replyText: String, onComplete: (Boolean) -> Unit) -> Unit)? = null
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

    inner class FeedbackPostVH(private val b: ItemFeedbackPostBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(post: FeedbackPost) {
            b.tvFeedbackPostTitle.text = post.title
            b.tvFeedbackPostMessage.text = post.message
            b.tvFeedbackPostDate.text = formatTimestamp(post.timestamp)

            b.btnSendReply.setOnClickListener {
                val text = b.etReplyText.text?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    Toast.makeText(b.root.context, "Please enter your reply", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                b.btnSendReply.isEnabled = false
                onSendReply?.invoke(post, text) { success ->
                    b.btnSendReply.isEnabled = true
                    if (success) {
                        b.etReplyText.text?.clear()
                        val imm = b.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(b.etReplyText.windowToken, 0)
                    }
                }
            }

            b.root.setOnLongClickListener { view ->
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onFeedbackPostLongClick?.invoke(post, view)
                true
            }
        }

        private fun formatTimestamp(time: Long): String {
            if (time <= 0L) return ""
            val diff = System.currentTimeMillis() - time
            return when {
                diff < 60_000L -> "Just now"
                diff < 3600_000L -> "${diff / 60_000L}m ago"
                diff < 86400_000L -> "${diff / 3600_000L}h ago"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    sdf.format(Date(time))
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HomeListItem.Header -> VIEW_TYPE_HEADER
        is HomeListItem.ExamRow -> VIEW_TYPE_EXAM
        is HomeListItem.FeedbackPostRow -> VIEW_TYPE_FEEDBACK_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderVH(ItemCategoryHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_EXAM -> ExamVH(ItemExamBinding.inflate(inflater, parent, false))
            VIEW_TYPE_FEEDBACK_POST -> FeedbackPostVH(ItemFeedbackPostBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HomeListItem.Header -> (holder as HeaderVH).bind(item)
            is HomeListItem.ExamRow -> (holder as ExamVH).bind(item.exam, item.attempted, item.isPinned)
            is HomeListItem.FeedbackPostRow -> (holder as FeedbackPostVH).bind(item.post)
        }
    }

    override fun getItemCount() = items.size
}
