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
    private val onReplyClick: (FeedbackMessage) -> Unit,
    private val onDeleteClick: (FeedbackMessage) -> Unit
) : ListAdapter<FeedbackMessage, FeedbackMessagesAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemFeedbackMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedbackMessage) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(binding.root.context)
            binding.tvSenderName.text = item.userName.ifBlank { "Student" }
            binding.tvSenderName.setTextColor(palette.textPrimary)
            binding.tvSenderEmail.text = item.userEmail.ifBlank { "No email provided" }
            binding.tvSenderEmail.setTextColor(palette.textSecondary)
            val bodyText = if (!item.postTitle.isNullOrEmpty()) {
                "📌 In response to: ${item.postTitle}\n\n${item.message}"
            } else {
                item.message
            }
            binding.tvMessageBody.text = bodyText
            binding.tvMessageBody.setTextColor(palette.textPrimary)
            binding.tvTimestamp.text = dateFormat.format(Date(item.timestamp))
            binding.tvTimestamp.setTextColor(palette.textSecondary)

            if (!item.read) {
                binding.tvUnreadBadge.visibility = View.VISIBLE
                binding.tvUnreadBadge.setTextColor(palette.accent)
                binding.cardMessage.strokeWidth = 0
                binding.cardMessage.setStrokeColor(palette.hairline)
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
                binding.cardMessage.strokeWidth = 0
                binding.cardMessage.setStrokeColor(palette.hairline)
            }

            binding.cardMessage.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            binding.btnReply.rippleColor = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            binding.btnDeleteMessage.background = com.eve.app.designsystem.TelegramThemeSelectors.getCircleSelectorDrawable(
                palette.actionSelector
            )

            binding.cardMessage.setOnClickListener { onItemClick(item) }
            binding.btnReply.setOnClickListener { onReplyClick(item) }
            binding.btnDeleteMessage.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedbackMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.cardMessage.rippleColor = android.content.res.ColorStateList.valueOf(
            com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
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
