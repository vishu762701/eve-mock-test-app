package com.eve.app.ui.admin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Poll
import com.eve.app.databinding.ItemPollAdminBinding
import com.eve.app.designsystem.UiArcMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PollsAdminAdapter(
    private val onToggleStatus: (Poll) -> Unit,
    private val onDelete: (Poll) -> Unit
) : RecyclerView.Adapter<PollsAdminAdapter.PollViewHolder>() {

    private val polls = mutableListOf<Poll>()
    private val disclosureState = PollDisclosureState()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    fun getItems(): List<Poll> = polls.toList()

    fun submitList(list: List<Poll>) {
        polls.clear()
        polls.addAll(list)
        disclosureState.retain(list.map { it.id })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollViewHolder {
        val binding = ItemPollAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PollViewHolder, position: Int) {
        holder.bind(polls[position])
    }

    override fun getItemCount(): Int = polls.size

    override fun onViewRecycled(holder: PollViewHolder) {
        holder.cancelDisclosureAnimation()
        super.onViewRecycled(holder)
    }

    inner class PollViewHolder(
        private val binding: ItemPollAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var disclosureAnimator: ValueAnimator? = null

        fun cancelDisclosureAnimation() {
            disclosureAnimator?.cancel()
            disclosureAnimator = null
            binding.layoutResults.layoutParams = binding.layoutResults.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.layoutResults.alpha = 1f
        }

        fun bind(poll: Poll) {
            cancelDisclosureAnimation()
            val context = binding.root.context
            binding.tvPollQuestion.text = poll.question
            binding.tvTotalVotes.text = "${poll.totalVotes} total votes"

            if (poll.endsAt > 0) {
                binding.tvExpiresAt.visibility = View.VISIBLE
                binding.tvExpiresAt.text = "Ends: ${dateFormat.format(Date(poll.endsAt))}"
            } else {
                binding.tvExpiresAt.visibility = View.GONE
            }

            // Status chip
            when {
                poll.isExpired -> {
                    binding.tvPollStatus.text = "Expired"
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.system_orange))
                    binding.btnToggleActive.isEnabled = false
                    binding.btnToggleActive.text = "Expired"
                }
                poll.active -> {
                    binding.tvPollStatus.text = "Active"
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.system_green))
                    binding.btnToggleActive.isEnabled = true
                    binding.btnToggleActive.text = "Close Poll"
                }
                else -> {
                    binding.tvPollStatus.text = "Closed"
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    binding.btnToggleActive.isEnabled = true
                    binding.btnToggleActive.text = "Reopen Poll"
                }
            }

            binding.btnToggleActive.setOnClickListener {
                onToggleStatus(poll)
            }

            binding.btnDeletePoll.setOnClickListener {
                onDelete(poll)
            }

            // Results are keyed by stable Poll ID, not transient adapter positions.
            val isExpanded = disclosureState.isExpanded(poll.id)
            binding.layoutResults.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.tvTapToView.text = if (isExpanded) "Tap to hide results" else "Tap to view results"
            binding.tvTapToView.contentDescription = "${if (isExpanded) "Hide" else "View"} results for ${poll.question}"
            if (isExpanded) renderOptionResults(poll)

            binding.layoutResults.clipChildren = true
            binding.layoutResults.clipToPadding = true
            val toggle = View.OnClickListener {
                // Do not rebind/recycle the row during the disclosure animation.
                val expanded = disclosureState.toggle(poll.id)
                binding.tvTapToView.text = if (expanded) "Tap to hide results" else "Tap to view results"
                binding.tvTapToView.contentDescription =
                    "${if (expanded) "Hide" else "View"} results for ${poll.question}"
                if (expanded) renderOptionResults(poll)
                animateResults(expanded)
            }
            binding.root.setOnClickListener(toggle)
            binding.tvTapToView.setOnClickListener(toggle)
            binding.tvTapToView.isFocusable = true
        }

        private fun animateResults(expanded: Boolean) {
            val panel = binding.layoutResults
            val startHeight = if (panel.visibility == View.VISIBLE) panel.height else 0
            disclosureAnimator?.cancel()
            disclosureAnimator = null
            val availableWidth = binding.root.width - binding.root.paddingLeft - binding.root.paddingRight
            panel.visibility = View.VISIBLE
            if (availableWidth <= 0 || !panel.isAttachedToWindow ||
                !UiArcMotion.isMotionEnabled(panel.context)) {
                panel.layoutParams = panel.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                panel.visibility = if (expanded) View.VISIBLE else View.GONE
                panel.alpha = 1f
                return
            }
            panel.measure(
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val destination = if (expanded) panel.measuredHeight else 0
            val start = startHeight.coerceAtLeast(0)
            if (start == destination) {
                panel.layoutParams = panel.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                panel.visibility = if (expanded) View.VISIBLE else View.GONE
                panel.alpha = 1f
                return
            }
            panel.layoutParams = panel.layoutParams.apply { height = start }
            val animator = ValueAnimator.ofInt(start, destination).apply {
                duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
                interpolator = UiArcMotion.Spring.SMOOTH // Non-overshooting height.
                addUpdateListener { frame ->
                    val next = frame.animatedValue as Int
                    if (panel.layoutParams.height != next) {
                        panel.layoutParams = panel.layoutParams.apply { height = next }
                    }
                    panel.alpha = if (expanded) frame.animatedFraction else 1f - frame.animatedFraction
                }
                var cancelled = false
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) { cancelled = true }
                    override fun onAnimationEnd(animation: Animator) {
                        if (cancelled) return
                        panel.layoutParams = panel.layoutParams.apply {
                            height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }
                        panel.visibility = if (expanded) View.VISIBLE else View.GONE
                        panel.alpha = 1f
                        disclosureAnimator = null
                    }
                })
            }
            disclosureAnimator = animator
            animator.start()
        }

        private fun renderOptionResults(poll: Poll) {
            binding.optionsContainer.removeAllViews()
            val inflater = LayoutInflater.from(binding.root.context)

            poll.options.forEachIndexed { index, optionText ->
                val rowView = inflater.inflate(R.layout.item_poll_option_result, binding.optionsContainer, false)
                val tvTitle = rowView.findViewById<TextView>(R.id.tvOptionTitle)
                val tvPercent = rowView.findViewById<TextView>(R.id.tvOptionPercent)
                val pbOption = rowView.findViewById<ProgressBar>(R.id.pbOption)
                val tvVotes = rowView.findViewById<TextView>(R.id.tvOptionVotes)

                val votes = poll.getVotesForOption(index)
                val percent = poll.getPercentageForOption(index)

                tvTitle.text = optionText
                tvPercent.text = "$percent%"
                pbOption.progress = percent
                tvVotes.text = "$votes votes"

                binding.optionsContainer.addView(rowView)
            }
        }
    }
}
