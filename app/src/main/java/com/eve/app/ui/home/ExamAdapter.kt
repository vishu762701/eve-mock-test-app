package com.eve.app.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.TestAttempt
import com.eve.app.util.AppBulletin
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_EXAM = 1
private const val VIEW_TYPE_FEEDBACK_POST = 2

/**
 * Programmatic Telegram-native Home list adapter (Pass 3a).
 * Uses upstream org.telegram.ui.Cells.HeaderCell, EditTextBoldCursor,
 * Theme.getColor(Theme.key_*), Theme.getSelectorDrawable, and AndroidUtilities.dp/getTypeface.
 * Zero XML layout inflation.
 */
class ExamAdapter(
    private val onClick: (Exam, Boolean, TestAttempt?) -> Unit,
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
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = list.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = items[oldItemPosition]
                val new = list[newItemPosition]
                return when {
                    old is HomeListItem.Header && new is HomeListItem.Header -> old.title == new.title
                    old is HomeListItem.ExamRow && new is HomeListItem.ExamRow -> old.exam.id == new.exam.id
                    old is HomeListItem.FeedbackPostRow && new is HomeListItem.FeedbackPostRow -> old.post.id == new.post.id
                    else -> false
                }
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == list[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = list
        diffResult.dispatchUpdatesTo(this)
    }

    private fun dp(v: Float): Int = AndroidUtilities.dp(v)
    private fun mediumFont(): Typeface =
        AndroidUtilities.getTypeface("fonts/rmedium.ttf") ?: Typeface.DEFAULT_BOLD

    inner class HeaderVH(private val cell: HeaderCell) : RecyclerView.ViewHolder(cell) {
        fun bind(header: HomeListItem.Header) {
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            cell.setText(header.title)
            val categoryEmoji = com.eve.app.ui.admin.emoji.EmojiRenderer.getCategoryEmoji(header.title)
            com.eve.app.ui.admin.emoji.EmojiRenderer.attachEmoji(cell.textView, header.title, categoryEmoji)
        }
    }

    inner class ExamVH(
        val container: LinearLayout,
        val iconFrame: FrameLayout,
        val ivExamImage: ImageView,
        val lottieExamIcon: LottieAnimationView,
        val tvExamName: TextView,
        val tvExamTime: TextView,
        val ivPinned: ImageView,
        val divider: View
    ) : RecyclerView.ViewHolder(container) {
        private var currentExamId: String? = null
        private var isLottieMode: Boolean = false

        fun bind(exam: Exam, attempted: Boolean, isPinned: Boolean, attempt: TestAttempt?, position: Int) {
            currentExamId = exam.id
            val ctx = container.context
            container.background = Theme.getSelectorDrawable(true)

            com.eve.app.ui.admin.emoji.EmojiRenderer.attachEmoji(tvExamName, exam.examName, exam.emoji)
            tvExamTime.text = if (attempted) {
                ctx.getString(R.string.exam_submitted_tap_to_view)
            } else {
                ctx.getString(R.string.exam_time_category, exam.timeLimitMinutes, exam.categoryOrOther)
            }

            tvExamName.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            tvExamTime.setTextColor(
                if (attempted) Theme.getColor(Theme.key_windowBackgroundWhiteGreenText2)
                else Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
            )
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider))
            ivPinned.imageTintList = ColorStateList.valueOf(Theme.getColor(Theme.key_chats_pinnedIcon))

            val accentBg = Theme.getColor(Theme.key_avatar_backgroundBlue)
            val accentFg = Theme.getColor(Theme.key_avatar_text)
            iconFrame.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentBg)
            }
            ivExamImage.setColorFilter(accentFg)
            lottieExamIcon.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
                PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4), PorterDuff.Mode.SRC_ATOP)
            }

            if (exam.imageUrl.isNotBlank()) {
                showLogoMode()
                com.eve.app.util.ExamImageHelper.loadExamImage(ivExamImage, exam.imageUrl) {
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

            ivPinned.visibility = if (isPinned) View.VISIBLE else View.GONE
            container.contentDescription = HomeExamAccessibility.describe(
                tvExamName.text, tvExamTime.text,
                if (isPinned) ivPinned.contentDescription else null
            )
            container.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            listOf(iconFrame, ivExamImage, lottieExamIcon, ivPinned, tvExamName, tvExamTime).forEach { child ->
                child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            container.isEnabled = true
            container.alpha = 1f
            container.setOnClickListener { onClick(exam, attempted, attempt) }
            container.setOnLongClickListener { view ->
                onLongClick?.invoke(exam, isPinned, view)
                true
            }
        }

        private fun showLogoMode() {
            isLottieMode = false
            ivExamImage.visibility = View.VISIBLE
            lottieExamIcon.visibility = View.GONE
            lottieExamIcon.cancelAnimation()
        }

        private fun showLottieMode() {
            iconFrame.setBackgroundColor(Color.TRANSPARENT)
            isLottieMode = true
            ivExamImage.visibility = View.GONE
            lottieExamIcon.visibility = View.VISIBLE
            val examId = currentExamId
            if (examId != null && playedExamIds.contains(examId)) {
                lottieExamIcon.progress = 1f
            }
        }

        fun playLottieIfEligible() {
            val examId = currentExamId ?: return
            if (!isLottieMode) return
            val animScale = Settings.Global.getFloat(
                container.context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            if (animScale == 0f) {
                lottieExamIcon.progress = 1f
                playedExamIds.add(examId)
                return
            }
            if (!playedExamIds.contains(examId)) {
                playedExamIds.add(examId)
                lottieExamIcon.progress = 0f
                lottieExamIcon.playAnimation()
            } else {
                lottieExamIcon.progress = 1f
            }
        }

        fun cancelAnimation() {
            lottieExamIcon.cancelAnimation()
        }
    }

    inner class FeedbackPostVH(
        val container: LinearLayout,
        val tvTitle: TextView,
        val tvDate: TextView,
        val tvMessage: TextView,
        val etReply: EditTextBoldCursor,
        val btnSend: ImageButton,
        val divider: View
    ) : RecyclerView.ViewHolder(container) {
        fun bind(post: FeedbackPost) {
            val ctx = container.context
            container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            tvTitle.text = post.title
            tvTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            tvMessage.text = post.message
            tvMessage.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            tvDate.text = formatTimestamp(post.timestamp)
            tvDate.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            etReply.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            etReply.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
            etReply.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            etReply.setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                Theme.getColor(Theme.key_text_RedRegular)
            )
            btnSend.imageTintList = ColorStateList.valueOf(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4))
            btnSend.background = Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP)
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider))

            btnSend.setOnClickListener {
                val text = etReply.text?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    AppBulletin.showError(ctx, "Please enter your reply")
                    return@setOnClickListener
                }
                btnSend.isEnabled = false
                onSendReply?.invoke(post, text) { success ->
                    btnSend.isEnabled = true
                    if (success) {
                        etReply.text?.clear()
                        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(etReply.windowToken, 0)
                    }
                }
            }

            container.setOnLongClickListener { view ->
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
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(time))
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HomeListItem.Header -> VIEW_TYPE_HEADER
        is HomeListItem.ExamRow -> VIEW_TYPE_EXAM
        is HomeListItem.FeedbackPostRow -> VIEW_TYPE_FEEDBACK_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val ctx = parent.context
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val cell = HeaderCell(ctx)
                cell.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                HeaderVH(cell)
            }
            VIEW_TYPE_EXAM -> createExamVH(ctx)
            VIEW_TYPE_FEEDBACK_POST -> createFeedbackPostVH(ctx)
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    private fun createExamVH(ctx: Context): ExamVH {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(10f), dp(16f), dp(10f))
        }
        val iconFrame = FrameLayout(ctx)
        val ivExam = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val lottie = LottieAnimationView(ctx).apply {
            setAnimation(R.raw.pencil_writing_on_book)
        }
        iconFrame.addView(ivExam, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        iconFrame.addView(lottie, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), 0, dp(8f), 0)
        }
        val tvName = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = mediumFont()
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val tvTime = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        textCol.addView(tvName)
        textCol.addView(tvTime, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 0f))

        val ivPinned = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_pin)
            contentDescription = "Pinned"
            visibility = View.GONE
        }

        row.addView(iconFrame, LayoutHelper.createLinear(50, 50))
        row.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(ivPinned, LayoutHelper.createLinear(18, 18))

        val divider = View(ctx)
        root.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
            marginStart = dp(76f)
        })
        return ExamVH(root, iconFrame, ivExam, lottie, tvName, tvTime, ivPinned, divider)
    }

    private fun createFeedbackPostVH(ctx: Context): FeedbackPostVH {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(10f))
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvTitle = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = mediumFont()
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val tvDate = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        }
        topRow.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topRow.addView(tvDate)

        val tvMessage = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        }

        val replyRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val etReply = EditTextBoldCursor(ctx).apply {
            hint = "Write a reply..."
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setCursorSize(dp(18f))
            setCursorWidth(1.5f)
            background = null
        }
        val btnSend = ImageButton(ctx).apply {
            setImageResource(R.drawable.ic_send_plane)
            contentDescription = "Send reply"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        replyRow.addView(etReply, LinearLayout.LayoutParams(0, dp(42f), 1f))
        replyRow.addView(btnSend, LayoutHelper.createLinear(42, 42))

        val divider = View(ctx)
        root.addView(topRow)
        root.addView(tvMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 8f))
        root.addView(replyRow)
        root.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
            topMargin = dp(8f)
        })
        return FeedbackPostVH(root, tvTitle, tvDate, tvMessage, etReply, btnSend, divider)
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
        if (holder is ExamVH) holder.playLottieIfEligible()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ExamVH) holder.cancelAnimation()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ExamVH) holder.cancelAnimation()
    }

    override fun getItemCount() = items.size
}
