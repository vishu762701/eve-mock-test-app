package com.eve.app.ui.admin

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PollsAdminAdapter(
    private val onToggleStatus: (Poll) -> Unit,
    private val onDelete: (Poll) -> Unit
) : RecyclerView.Adapter<PollsAdminAdapter.PollViewHolder>() {

    private val polls = mutableListOf<Poll>()
    private val expandedPollIds = mutableSetOf<String>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    fun submitList(list: List<Poll>) {
        polls.clear()
        polls.addAll(list)
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

    inner class PollViewHolder(
        private val binding: ItemPollAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(poll: Poll) {
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
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_gold))
                    binding.btnToggleActive.isEnabled = false
                    binding.btnToggleActive.text = "Expired"
                }
                poll.active -> {
                    binding.tvPollStatus.text = "Active"
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_green))
                    binding.btnToggleActive.isEnabled = true
                    binding.btnToggleActive.text = "Close Poll"
                }
                else -> {
                    binding.tvPollStatus.text = "Closed"
                    binding.tvPollStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
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

            // Results expand / collapse
            val isExpanded = expandedPollIds.contains(poll.id)
            binding.layoutResults.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.tvTapToView.text = if (isExpanded) "Tap to hide results" else "Tap to view results"

            if (isExpanded) {
                renderOptionResults(poll)
            }

            binding.root.setOnClickListener {
                if (expandedPollIds.contains(poll.id)) {
                    expandedPollIds.remove(poll.id)
                } else {
                    expandedPollIds.add(poll.id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }
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
