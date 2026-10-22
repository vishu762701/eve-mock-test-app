package com.eve.app.ui.admin

import android.animation.ValueAnimator
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemExistingExamAdminBinding
import com.eve.app.designsystem.UiArcMotion

data class ExamWithStats(
    val exam: Exam,
    val attemptCount: Long = 0L,
    val questionCount: Int = exam.questionCount,
    val treeDepth: Int = 0,
    val hasChildren: Boolean = false,
    val isExpanded: Boolean = false
)

class ExistingExamsAdapter(
    private val onExamClick: (Exam) -> Unit,
    private val onEditClick: (Exam) -> Unit,
    private val onPreviewClick: (Exam) -> Unit,
    private val onDeleteClick: (Exam) -> Unit,
    private val onExpandToggle: (Exam) -> Unit
) : ListAdapter<ExamWithStats, ExistingExamsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemExistingExamAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val actionAnimators = mutableMapOf<View, ValueAnimator>()
        private val actionWidths = mutableMapOf<View, Int>()
        private val widthSpring = UiArcMotion.SpringCurve(0.42f, 0f, 550L)

        private val hoverSpring = UiArcMotion.SpringCurve(
            visualDuration = 0.26f, bounce = 0f, settlingDurationMs = 360L
        )

        fun resetHover() {
            binding.root.animate().cancel()
            binding.root.translationY = 0f
            actionAnimators.values.forEach { it.cancel() }
            actionAnimators.clear()
            actionWidths.clear()
            for (button in listOf(binding.btnQuickEdit, binding.btnQuickPreview, binding.btnMoreOptions)) {
                button.layoutParams = button.layoutParams.apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                button.setOnHoverListener(null)
                button.onFocusChangeListener = null
            }
        }

        private fun animateActionWidth(button: View, expand: Boolean) {
            if (!button.isAttachedToWindow || button.width <= 0) return
            val base = actionWidths.getOrPut(button) { button.width }
            val extra = (12f * button.resources.displayMetrics.density).toInt()
            val target = base + if (expand) extra else 0
            actionAnimators.remove(button)?.cancel()
            if (!UiArcMotion.isMotionEnabled(button.context)) {
                button.layoutParams = button.layoutParams.apply { width = target }
                return
            }
            val animator = ValueAnimator.ofInt(button.width, target).apply {
                duration = widthSpring.settlingDurationMs
                interpolator = widthSpring
                addUpdateListener { frame ->
                    val width = frame.animatedValue as Int
                    if (button.layoutParams.width != width) {
                        button.layoutParams = button.layoutParams.apply { this.width = width }
                    }
                }
            }
            actionAnimators[button] = animator
            animator.start()
        }

        private fun bindActionMorph() {
            val actions = listOf(binding.btnQuickPreview, binding.btnQuickEdit, binding.btnMoreOptions)
            for (button in actions) {
                button.setOnHoverListener { _, event ->
                    if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return@setOnHoverListener false
                    when (event.actionMasked) {
                        MotionEvent.ACTION_HOVER_ENTER -> animateActionWidth(button, true)
                        MotionEvent.ACTION_HOVER_EXIT -> animateActionWidth(button, false)
                    }
                    false
                }
                button.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
                    animateActionWidth(button, focused)
                }
            }
        }

        fun bind(item: ExamWithStats) {
            resetHover()
            // UIArc Card: source hover lifts by 2px. No touch elevation/geometry change.
            binding.root.setOnHoverListener { _, event ->
                if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return@setOnHoverListener false
                val lift = when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER -> -2f * binding.root.resources.displayMetrics.density
                    MotionEvent.ACTION_HOVER_EXIT -> 0f
                    else -> return@setOnHoverListener false
                }
                binding.root.animate().cancel()
                if (UiArcMotion.isMotionEnabled(binding.root.context)) {
                    binding.root.animate().translationY(lift)
                        .setDuration(hoverSpring.settlingDurationMs)
                        .setInterpolator(hoverSpring)
                        .start()
                } else binding.root.translationY = 0f
                false // Preserve native focus, click and child quick-action dispatch.
            }
            val exam = item.exam
            val depth = item.treeDepth.coerceIn(0, 3)
            binding.tvExamName.text = exam.examName
            // Keep current card footprint. Indent only the label for nested exams.
            binding.tvExamName.setPadding(
                (depth * 14f * binding.root.resources.displayMetrics.density).toInt(),
                binding.tvExamName.paddingTop, binding.tvExamName.paddingRight,
                binding.tvExamName.paddingBottom
            )
            binding.btnExpandExam.visibility = if (item.hasChildren) View.VISIBLE else View.GONE
            binding.btnExpandExam.rotation = if (item.isExpanded) 90f else 0f
            binding.btnExpandExam.contentDescription =
                (if (item.isExpanded) "Collapse" else "Expand") + " sub-exams under " + exam.examName
            binding.btnExpandExam.setOnClickListener {
                val spring = UiArcMotion.SpringCurve(0.26f, 0f, 360L)
                if (UiArcMotion.isMotionEnabled(binding.root.context)) {
                    binding.btnExpandExam.animate().cancel()
                    binding.btnExpandExam.animate()
                        .rotation(if (item.isExpanded) 0f else 90f)
                        .setDuration(spring.settlingDurationMs)
                        .setInterpolator(spring).start()
                }
                onExpandToggle(exam)
            }
            binding.tvCategoryBadge.text = exam.categoryOrOther
            binding.tvTimeLimit.text = "⏱️ ${exam.timeLimitMinutes} mins"
            val qCount = if (item.questionCount > 0) item.questionCount else exam.questionCount
            binding.tvQuestionCount.text = "📝 $qCount Qs"
            binding.tvAttemptCount.text = "📊 ${item.attemptCount} student attempts"

            bindActionMorph()
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
                com.eve.app.designsystem.DesignPopupGlass.around(view) { popup.show() }
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

    override fun onViewRecycled(holder: ViewHolder) {
        holder.resetHover()
        super.onViewRecycled(holder)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExamWithStats>() {
        override fun areItemsTheSame(oldItem: ExamWithStats, newItem: ExamWithStats) =
            oldItem.exam.id == newItem.exam.id

        override fun areContentsTheSame(oldItem: ExamWithStats, newItem: ExamWithStats) =
            oldItem == newItem
    }
}
