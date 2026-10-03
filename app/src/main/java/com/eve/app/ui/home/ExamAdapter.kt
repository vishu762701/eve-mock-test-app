package com.eve.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.eve.app.util.AppBulletin
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
    private val onClick: (Exam, Boolean, com.eve.app.data.model.TestAttempt?) -> Unit,
    private val onLongClick: ((Exam, Boolean, View) -> Unit)? = null,
    private val onFeedbackPostLongClick: ((FeedbackPost, View) -> Unit)? = null,
    private val onSendReply: ((post: FeedbackPost, replyText: String, onComplete: (Boolean) -> Unit) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<HomeListItem> = emptyList()
    private val playedExamIds = mutableSetOf<String>()

    fun resetPlayedAnimations() {
        playedExamIds.clear()
    }

    fun replayVisible(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val vh = recyclerView.getChildViewHolder(child) as? ExamVH
            vh?.playLottieIfEligible()
        }
    }

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
        private var currentExamId: String? = null
        private var isLottieMode: Boolean = false

        init {
            val ctx = b.root.context
            b.examIconContainer.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_exam_icon_tile_bg))
            b.examIconContainer.strokeColor = androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_exam_icon_tile_border)
            b.examIconContainer.strokeWidth = (1 * ctx.resources.displayMetrics.density).toInt()
        }

        fun bind(exam: Exam, attempted: Boolean, isPinned: Boolean, attempt: com.eve.app.data.model.TestAttempt?, position: Int) {
            currentExamId = exam.id
            b.tvExamName.text = exam.examName
            b.tvExamTime.text = if (attempted) {
                b.root.context.getString(com.eve.app.R.string.exam_submitted_tap_to_view)
            } else {
                b.root.context.getString(
                    com.eve.app.R.string.exam_time_category,
                    exam.timeLimitMinutes,
                    exam.categoryOrOther
                )
            }

            val ctx = b.root.context
            b.root.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_glass_card_fill))
            b.root.strokeColor = androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_glass_card_hairline)
            b.root.strokeWidth = (1 * ctx.resources.displayMetrics.density).toInt()
            b.tvExamName.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_text))
            b.tvExamTime.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_text_secondary))
            b.examIconContainer.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_exam_icon_tile_bg))
            b.examIconContainer.strokeColor = androidx.core.content.ContextCompat.getColor(ctx, com.eve.app.R.color.eve_exam_icon_tile_border)

            val hasLogo = exam.imageUrl.isNotBlank()
            if (hasLogo) {
                showLogoMode()
                com.eve.app.util.ExamImageHelper.loadExamImage(b.ivExamImage, exam.imageUrl) {
                    if (currentExamId == exam.id) {
                        showLottieMode()
                        if (itemView.isAttachedToWindow) {
                            playLottieIfEligible()
                        }
                    }
                }
            } else {
                showLottieMode()
            }

            b.ivPinned.visibility = if (isPinned) View.VISIBLE else View.GONE
            b.root.isEnabled = true
            b.root.alpha = 1f
            b.root.setOnClickListener { onClick(exam, attempted, attempt) }

            b.root.setOnLongClickListener { view ->
                onLongClick?.invoke(exam, isPinned, view)
                true
            }
        }

        private fun showLogoMode() {
            isLottieMode = false
            b.examIconContainer.strokeWidth = 0
            b.examIconContainer.setCardBackgroundColor(
                androidx.core.content.ContextCompat.getColor(b.root.context, android.R.color.transparent)
            )
            b.ivExamImage.visibility = View.VISIBLE
            b.lottieExamIcon.visibility = View.GONE
            b.lottieExamIcon.cancelAnimation()
        }

        private fun showLottieMode() {
            isLottieMode = true
            b.examIconContainer.strokeWidth = 0
            b.examIconContainer.setCardBackgroundColor(
                androidx.core.content.ContextCompat.getColor(b.root.context, android.R.color.transparent)
            )
            b.ivExamImage.visibility = View.GONE
            b.lottieExamIcon.visibility = View.VISIBLE
            val examId = currentExamId
            if (examId != null && playedExamIds.contains(examId)) {
                b.lottieExamIcon.progress = 1f
            }
        }

        fun playLottieIfEligible() {
            val examId = currentExamId ?: return
            if (!isLottieMode) return

            val animScale = android.provider.Settings.Global.getFloat(
                b.root.context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            if (animScale == 0f) {
                b.lottieExamIcon.progress = 1f
                playedExamIds.add(examId)
                return
            }

            if (!playedExamIds.contains(examId)) {
                playedExamIds.add(examId)
                b.lottieExamIcon.progress = 0f
                b.lottieExamIcon.playAnimation()
            } else {
                b.lottieExamIcon.progress = 1f
            }
        }

        fun cancelAnimation() {
            b.lottieExamIcon.cancelAnimation()
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
                    AppBulletin.showError(b.root.context, "Please enter your reply")
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
            is HomeListItem.ExamRow -> (holder as ExamVH).bind(item.exam, item.attempted, item.isPinned, item.attempt, position)
            is HomeListItem.FeedbackPostRow -> (holder as FeedbackPostVH).bind(item.post)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ExamVH) {
            holder.playLottieIfEligible()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ExamVH) {
            holder.cancelAnimation()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ExamVH) {
            holder.cancelAnimation()
        }
    }

    override fun getItemCount() = items.size
}
