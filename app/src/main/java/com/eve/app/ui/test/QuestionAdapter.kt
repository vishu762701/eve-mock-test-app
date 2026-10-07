package com.eve.app.ui.test

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ItemQuestionBinding
import com.eve.app.util.HapticHelper
import com.eve.app.util.TestThinMaterialPillHelper
import eightbitlab.com.blurview.BlurView

class QuestionAdapter(
    private val questions: List<Question>,
    private val getSelected: (Int) -> String,
    private val onSelect: (Int, String) -> Unit,
    private val getBookmarked: (Int) -> Boolean,
    private val onToggleBookmark: (Int) -> Unit,
    private val isHindi: () -> Boolean,
    private val onReport: (Question) -> Unit,
    private val getQuestionTime: (Int) -> Long,
    private val isMarked: (Int) -> Boolean = { false },
    private val onToggleMark: (Int) -> Unit = {},
    private val studioConfig: () -> com.eve.app.data.model.uistudio.UiStudioConfig = {
        com.eve.app.data.repository.UiStudioRepository.getInstance().currentConfig
    }
) : RecyclerView.Adapter<QuestionAdapter.VH>() {

    companion object {
        const val PAYLOAD_TIMER = "PAYLOAD_TIMER"

        fun formatQuestionTime(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) {
                String.format("%d:%02d:%02d", h, m, s)
            } else {
                String.format("%d:%02d", m, s)
            }
        }
    }

    inner class VH(
        private val b: ItemQuestionBinding,
        blurRoot: ViewGroup
    ) : RecyclerView.ViewHolder(b.root) {
        private var currentAnimator: ValueAnimator? = null
        private val optionPills = listOf(
            b.rbA to b.blurOptionA,
            b.rbB to b.blurOptionB,
            b.rbC to b.blurOptionC,
            b.rbD to b.blurOptionD
        )

        init {
            optionPills.forEach { (button, surface) ->
                button.enableTestThinMaterialStyle()
                TestThinMaterialPillHelper.attach(surface, blurRoot, b.root.context, drawStroke = false)
                button.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOptionPillBounds() }
            }
            b.rgOptions.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOptionPillBounds() }
            b.optionsMaterialHost.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOptionPillBounds() }
            b.optionsMaterialHost.post { updateOptionPillBounds() }
        }

        private fun updateOptionPillBounds() {
            if (b.optionsMaterialHost.width == 0 || b.rgOptions.height == 0) return
            optionPills.forEach { (button, surface) ->
                if (button.width == 0 || button.height == 0) return@forEach
                val oldParams = surface.layoutParams as? FrameLayout.LayoutParams
                val left = b.rgOptions.left + button.left
                val top = b.rgOptions.top + button.top
                if (oldParams == null || oldParams.width != button.width || oldParams.height != button.height ||
                    oldParams.leftMargin != left || oldParams.topMargin != top
                ) {
                    val params = FrameLayout.LayoutParams(button.width, button.height).apply {
                        leftMargin = left
                        topMargin = top
                    }
                    surface.layoutParams = params
                    TestThinMaterialPillHelper.setSelected(
                        surface,
                        b.rgOptions.checkedRadioButtonId == button.id,
                        b.root.context,
                        drawStroke = false
                    )
                }
            }
        }

        private fun updateOptionSelection(checkedId: Int) {
            optionPills.forEach { (button, surface) ->
                TestThinMaterialPillHelper.setSelected(
                    surface,
                    checkedId == button.id,
                    b.root.context,
                    drawStroke = false
                )
            }
        }

        fun updateTimer(seconds: Long) {
            b.tvQuestionTimer.text = formatQuestionTime(seconds)
        }

        fun bind(position: Int, q: Question) {
            updateTimer(getQuestionTime(position))
            b.btnReport.setOnClickListener {
                onReport(q)
            }

            val hindi = isHindi()
            val questionText = "Q${position + 1}. ${q.displayQuestionText(hindi)}"
            b.tvQuestion.text = questionText
            b.rbA.text = "A. ${q.displayOptionText("A", hindi)}"
            b.rbB.text = "B. ${q.displayOptionText("B", hindi)}"
            b.rbC.text = "C. ${q.displayOptionText("C", hindi)}"
            b.rbD.text = "D. ${q.displayOptionText("D", hindi)}"

            com.eve.app.uistudio.StudioRenderer.apply(b.root, "test", studioConfig(), force = true)

            // Reset scroll position and auto-fit question text and options to the viewport
            b.svQuestion.scrollTo(0, 0)
            val optionsList = listOf(b.rbA, b.rbB, b.rbC, b.rbD)
            QuestionFitHelper.fitQuestionAndOptions(
                tvQuestion = b.tvQuestion,
                svQuestion = b.svQuestion,
                ivScrollHint = b.ivScrollHint,
                options = optionsList,
                questionText = questionText,
                questionContainer = b.questionContainer,
                rgOptions = b.rgOptions
            )

            // Cancel any in-flight star animation on recycled view and restore clean baseline
            currentAnimator?.cancel()
            currentAnimator = null
            b.sparkleView.reset()
            b.btnBookmark.rotation = 0f
            b.btnBookmark.scaleX = 1f
            b.btnBookmark.scaleY = 1f
            b.btnBookmark.setImageResource(
                if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            b.btnBookmark.setOnClickListener {
                onToggleBookmark(position)
                val nowBookmarked = getBookmarked(position)
                currentAnimator?.cancel()
                currentAnimator = null
                b.sparkleView.reset()

                if (nowBookmarked) {
                    // Bookmark ON: 360° spin + scale pop (1 -> 1.3 -> 1) with sparkle burst and light haptic
                    HapticHelper.performLight(b.btnBookmark)
                    b.sparkleView.startSparkle()

                    var iconSwapped = false
                    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 420L
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            b.btnBookmark.rotation = fraction * 360f

                            val scale = if (fraction < 0.4f) {
                                1f + (fraction / 0.4f) * 0.3f
                            } else {
                                val popFraction = (fraction - 0.4f) / 0.6f
                                1.3f - popFraction * 0.3f
                            }
                            b.btnBookmark.scaleX = scale
                            b.btnBookmark.scaleY = scale

                            if (!iconSwapped && fraction >= 0.4f) {
                                iconSwapped = true
                                b.btnBookmark.setImageResource(R.drawable.ic_star_filled)
                            }
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                b.btnBookmark.rotation = 0f
                                b.btnBookmark.scaleX = 1f
                                b.btnBookmark.scaleY = 1f
                                b.btnBookmark.setImageResource(
                                    if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                                )
                                currentAnimator = null
                            }

                            override fun onAnimationCancel(animation: Animator) {
                                b.btnBookmark.rotation = 0f
                                b.btnBookmark.scaleX = 1f
                                b.btnBookmark.scaleY = 1f
                                b.btnBookmark.setImageResource(
                                    if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                                )
                                currentAnimator = null
                            }
                        })
                    }
                    currentAnimator = anim
                    anim.start()
                } else {
                    // Bookmark OFF: quick reverse feel (~180ms: small scale down + counter-rotation)
                    b.btnBookmark.setImageResource(R.drawable.ic_star_outline)
                    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 180L
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            b.btnBookmark.rotation = -30f * (1f - fraction)
                            val scale = if (fraction < 0.5f) {
                                1f - (fraction / 0.5f) * 0.15f
                            } else {
                                0.85f + ((fraction - 0.5f) / 0.5f) * 0.15f
                            }
                            b.btnBookmark.scaleX = scale
                            b.btnBookmark.scaleY = scale
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                b.btnBookmark.rotation = 0f
                                b.btnBookmark.scaleX = 1f
                                b.btnBookmark.scaleY = 1f
                                b.btnBookmark.setImageResource(
                                    if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                                )
                                currentAnimator = null
                            }

                            override fun onAnimationCancel(animation: Animator) {
                                b.btnBookmark.rotation = 0f
                                b.btnBookmark.scaleX = 1f
                                b.btnBookmark.scaleY = 1f
                                b.btnBookmark.setImageResource(
                                    if (getBookmarked(position)) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                                )
                                currentAnimator = null
                            }
                        })
                    }
                    currentAnimator = anim
                    anim.start()
                }
            }

            // Clear previous listener on recycled view, then restore saved answer selection
            b.rgOptions.setOnCheckedChangeListener(null)
            b.rgOptions.clearCheck()
            when (getSelected(position)) {
                "A" -> b.rbA.isChecked = true
                "B" -> b.rbB.isChecked = true
                "C" -> b.rbC.isChecked = true
                "D" -> b.rbD.isChecked = true
            }
            updateOptionSelection(b.rgOptions.checkedRadioButtonId)
            b.rgOptions.setOnCheckedChangeListener { _, checkedId ->
                updateOptionSelection(checkedId)
                val letter = when (checkedId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }
                if (letter.isNotEmpty()) {
                    HapticHelper.performOptionSelect(b.root)
                    onSelect(position, letter)
                }
            }
        }

        fun clearSelection() {
            b.rgOptions.setOnCheckedChangeListener(null)
            b.rgOptions.clearCheck()
            updateOptionSelection(View.NO_ID)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(
            ItemQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            parent.rootView as? ViewGroup ?: parent
        )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(position, questions[position])

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TIMER)) {
            holder.updateTimer(getQuestionTime(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount() = questions.size
}
