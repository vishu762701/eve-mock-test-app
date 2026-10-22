package com.eve.app.ui.admin

import android.animation.ValueAnimator
import android.view.InputDevice
import android.view.KeyEvent
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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

    private val pendingEntranceIds = mutableSetOf<String>()

    fun prepareEntrances(newIds: Set<String>) {
        pendingEntranceIds.clear()
        pendingEntranceIds.addAll(newIds)
    }

    inner class ViewHolder(private val binding: ItemExistingExamAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val actionAnimators = mutableMapOf<View, ValueAnimator>()
        private val expandedWidths = mutableMapOf<View, Int>()
        private val density get() = binding.root.resources.displayMetrics.density
        private val touchMin get() = (48f * density).toInt()
        private val widthSpring = UiArcMotion.Spring.MORPH
        private val originalNamePadding = binding.tvExamName.paddingLeft
        private var boundExamId: String? = null
        private var boundExpanded = false
        private val hoverSpring = UiArcMotion.SpringCurve(
            visualDuration = 0.26f, bounce = 0f, settlingDurationMs = 360L
        )
        private val actionButtons get() =
            listOf(binding.btnQuickPreview, binding.btnQuickEdit, binding.btnMoreOptions)

        fun resetHover() {
            binding.root.animate().cancel()
            binding.root.translationY = 0f
            binding.root.alpha = 1f
            binding.btnExpandExam.animate().cancel()
            actionAnimators.values.forEach { it.cancel() }
            actionAnimators.clear()
            expandedWidths.clear()
            binding.layoutExamQuickActions.setOnHoverListener(null)
            for (button in actionButtons) {
                button.setOnHoverListener(null)
                button.onFocusChangeListener = null
                button.setOnKeyListener(null)
            }
            binding.btnQuickPreview.text = "Preview"
            binding.btnQuickEdit.text = "Edit"
            binding.btnQuickPreview.layoutParams = binding.btnQuickPreview.layoutParams.apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.btnQuickEdit.layoutParams = binding.btnQuickEdit.layoutParams.apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.btnMoreOptions.layoutParams = binding.btnMoreOptions.layoutParams.apply { width = touchMin }
            binding.tvMoreLabel.layoutParams = binding.tvMoreLabel.layoutParams.apply { width = 0 }
            binding.tvMoreLabel.alpha = 0f
        }

        private fun animateActionWidth(view: View, targetWidth: Int) {
            actionAnimators.remove(view)?.cancel()
            val oldWidth = view.width.coerceAtLeast(0)
            val target = targetWidth.coerceAtLeast(0)
            if (!UiArcMotion.isMotionEnabled(view.context) || oldWidth == 0) {
                view.layoutParams = view.layoutParams.apply { width = target }
                return
            }
            ValueAnimator.ofInt(oldWidth, target).apply {
                duration = widthSpring.settlingDurationMs
                interpolator = widthSpring
                addUpdateListener {
                    val width = it.animatedValue as Int
                    if (view.layoutParams.width != width) {
                        view.layoutParams = view.layoutParams.apply { this.width = width }
                    }
                }
                actionAnimators[view] = this
                start()
            }
        }

        private fun revealAction(active: View?) {
            val preview = binding.btnQuickPreview
            val edit = binding.btnQuickEdit
            val more = binding.btnMoreOptions
            // Touch remains direct and fully labelled. Compact interaction is only
            // enabled by a real mouse hover or hardware keyboard focus.
            if (active == null) {
                preview.text = "Preview"
                edit.text = "Edit"
                animateActionWidth(preview, expandedWidths[preview] ?: touchMin)
                animateActionWidth(edit, expandedWidths[edit] ?: touchMin)
                animateActionWidth(more, touchMin)
                animateActionWidth(binding.tvMoreLabel, 0)
            } else {
                val revealPreview = active === preview
                val revealEdit = active === edit
                preview.text = if (revealPreview) "Preview" else ""
                edit.text = if (revealEdit) "Edit" else ""
                animateActionWidth(preview, if (revealPreview) expandedWidths[preview] ?: touchMin else touchMin)
                animateActionWidth(edit, if (revealEdit) expandedWidths[edit] ?: touchMin else touchMin)
                animateActionWidth(more, touchMin)
                val moreWidth = (binding.tvMoreLabel.paint.measureText("More") + 12f * density).toInt()
                animateActionWidth(binding.tvMoreLabel, if (active === more) moreWidth else 0)
            }
            binding.tvMoreLabel.animate().cancel()
            binding.tvMoreLabel.animate().alpha(if (active === more) 1f else 0f)
                .setDuration(if (UiArcMotion.isMotionEnabled(binding.root.context)) UiArcMotion.Duration.FAST else 0L)
                .start()
        }

        private fun bindActionMorph() {
            val buttons = actionButtons
            // Measured natural widths are used, not guesses about Inter label size.
            binding.layoutExamQuickActions.post {
                if (!binding.root.isAttachedToWindow) return@post
                for (button in buttons) if (button !== binding.btnMoreOptions) {
                    if (button.width > touchMin) expandedWidths[button] = button.width
                }
            }
            binding.layoutExamQuickActions.setOnHoverListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_HOVER_EXIT &&
                    buttons.none { it.hasFocus() }) revealAction(null)
                false
            }
            for ((index, button) in buttons.withIndex()) {
                button.setOnHoverListener { _, event ->
                    if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return@setOnHoverListener false
                    when (event.actionMasked) {
                        MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE ->
                            if (event.x >= 4f * density && event.x <= button.width - 4f * density)
                                revealAction(button)
                        MotionEvent.ACTION_HOVER_EXIT ->
                            if (buttons.none { it.hasFocus() }) revealAction(null)
                    }
                    false
                }
                button.onFocusChangeListener = View.OnFocusChangeListener { _, focused ->
                    if (focused) revealAction(button)
                    else if (buttons.none { it.hasFocus() }) revealAction(null)
                }
                button.setOnKeyListener { _, keyCode, event ->
                    if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                    val rtl = binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val next = when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> if (rtl) index + 1 else index - 1
                        KeyEvent.KEYCODE_DPAD_RIGHT -> if (rtl) index - 1 else index + 1
                        KeyEvent.KEYCODE_MOVE_HOME -> 0
                        KeyEvent.KEYCODE_MOVE_END -> buttons.lastIndex
                        else -> return@setOnKeyListener false
                    }
                    buttons[(next + buttons.size) % buttons.size].requestFocus()
                    true
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
                originalNamePadding + (depth * 14f * binding.root.resources.displayMetrics.density).toInt(),
                binding.tvExamName.paddingTop, binding.tvExamName.paddingRight,
                binding.tvExamName.paddingBottom
            )
            val animatedExpansion = boundExamId == exam.id && boundExpanded != item.isExpanded
            boundExamId = exam.id
            boundExpanded = item.isExpanded
            binding.btnExpandExam.visibility = if (item.hasChildren) View.VISIBLE else View.GONE
            val targetAngle = if (item.isExpanded) 90f else 0f
            if (animatedExpansion && UiArcMotion.isMotionEnabled(binding.root.context)) {
                binding.btnExpandExam.animate().rotation(targetAngle)
                    .setDuration(UiArcMotion.Spring.SNAPPY.settlingDurationMs)
                    .setInterpolator(UiArcMotion.Spring.SNAPPY).start()
            } else binding.btnExpandExam.rotation = targetAngle
            val state = if (item.isExpanded) "Expanded" else "Collapsed"
            binding.btnExpandExam.contentDescription =
                (if (item.isExpanded) "Collapse" else "Expand") + " sub-exams under " + exam.examName
            ViewCompat.setStateDescription(binding.btnExpandExam, state)
            binding.tvExamName.contentDescription = "Level " + (item.treeDepth + 1) + ", " + exam.examName
            binding.btnExpandExam.setOnClickListener { onExpandToggle(exam) }
            binding.root.isFocusable = true
            binding.root.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnKeyListener false
                val rtl = binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
                val expandKey = if (rtl) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                val collapseKey = if (rtl) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                when (keyCode) {
                    expandKey -> {
                        if (item.hasChildren && !item.isExpanded) onExpandToggle(exam)
                        else if (item.hasChildren && position + 1 < itemCount)
                            focusTreeRow(position + 1)
                        true
                    }
                    collapseKey -> {
                        if (item.hasChildren && item.isExpanded) onExpandToggle(exam)
                        else {
                            val parentIndex = currentList.indexOfFirst { it.exam.id == exam.parentExamId }
                            if (parentIndex >= 0) focusTreeRow(parentIndex)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> { focusTreeRow((position + 1).coerceAtMost(itemCount - 1)); true }
                    KeyEvent.KEYCODE_DPAD_UP -> { focusTreeRow((position - 1).coerceAtLeast(0)); true }
                    KeyEvent.KEYCODE_MOVE_HOME -> { focusTreeRow(0); true }
                    KeyEvent.KEYCODE_MOVE_END -> { focusTreeRow(itemCount - 1); true }
                    else -> false
                }
            }
            val a11yAction = if (item.isExpanded)
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE
            else AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND
            val inverseAction = if (item.isExpanded)
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_EXPAND
            else AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_COLLAPSE
            ViewCompat.removeAccessibilityAction(binding.root, inverseAction.id)
            if (item.hasChildren) {
                ViewCompat.replaceAccessibilityAction(binding.root, a11yAction,
                    if (item.isExpanded) "Collapse sub-exams" else "Expand sub-exams",
                    androidx.core.view.accessibility.AccessibilityViewCommand { _, _ ->
                        onExpandToggle(exam)
                        true
                    }
                )
            } else ViewCompat.removeAccessibilityAction(binding.root, a11yAction.id)
            binding.tvCategoryBadge.text = exam.categoryOrOther
            binding.tvTimeLimit.text = "⏱️ ${exam.timeLimitMinutes} mins"
            val qCount = if (item.questionCount > 0) item.questionCount else exam.questionCount
            binding.tvQuestionCount.text = "📝 $qCount Qs"
            binding.tvAttemptCount.text = "📊 ${item.attemptCount} student attempts"

            bindActionMorph()
            if (item.treeDepth > 0 && pendingEntranceIds.remove(exam.id) &&
                UiArcMotion.isMotionEnabled(binding.root.context)) {
                binding.root.alpha = 0f
                binding.root.translationY = 6f * density
                binding.root.animate().alpha(1f).translationY(0f)
                    .setStartDelay(((item.treeDepth - 1).coerceAtMost(4) * UiArcMotion.Stagger.ITEM_MS))
                    .setDuration(UiArcMotion.Spring.SMOOTH.settlingDurationMs)
                    .setInterpolator(UiArcMotion.Spring.SMOOTH).start()
            }
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

    private fun focusTreeRow(position: Int) {
        if (position !in 0 until itemCount) return
        val recycler = attachedRecycler ?: return
        recycler.scrollToPosition(position)
        recycler.post {
            val holder = recycler.findViewHolderForAdapterPosition(position)
            holder?.itemView?.requestFocus()
        }
    }

    private var attachedRecycler: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecycler = recyclerView
    }
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        attachedRecycler = null
        super.onDetachedFromRecyclerView(recyclerView)
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
