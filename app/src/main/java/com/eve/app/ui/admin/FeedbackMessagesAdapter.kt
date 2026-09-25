package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.FeedbackMessage
import com.eve.app.databinding.ItemFeedbackMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackMessagesAdapter(
    private val onItemClick: (FeedbackMessage) -> Unit,
    private val onDeleteClick: (FeedbackMessage) -> Unit
) : ListAdapter<FeedbackMessage, FeedbackMessagesAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemFeedbackMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedbackMessage) {
            binding.tvSenderName.text = item.userName.ifBlank { "Student" }
            binding.tvSenderEmail.text = item.userEmail.ifBlank { "No email provided" }
            val bodyText = if (!item.postTitle.isNullOrEmpty()) {
                "📌 In response to: ${item.postTitle}\n\n${item.message}"
            } else {
                item.message
            }
            binding.tvMessageBody.text = bodyText
            binding.tvTimestamp.text = dateFormat.format(Date(item.timestamp))

            if (!item.read) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.cardMessage.strokeWidth = 3
                binding.cardMessage.setStrokeColor(
                    androidx.core.content.ContextCompat.getColor(binding.root.context, com.eve.app.R.color.eve_primary)
                )
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
                binding.cardMessage.strokeWidth = 1
                binding.cardMessage.setStrokeColor(
                    androidx.core.content.ContextCompat.getColor(binding.root.context, com.eve.app.R.color.eve_stroke)
                )
            }

            binding.cardMessage.setOnClickListener { onItemClick(item) }
            binding.btnDeleteMessage.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedbackMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FeedbackMessage>() {
        override fun areItemsTheSame(oldItem: FeedbackMessage, newItem: FeedbackMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FeedbackMessage, newItem: FeedbackMessage) =
            oldItem == newItem
    }
}
