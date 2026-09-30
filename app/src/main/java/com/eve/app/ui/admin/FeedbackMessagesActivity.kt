package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.FeedbackMessage
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityFeedbackMessagesBinding
import com.eve.app.ui.common.ErrorStateView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Task 2: Admin activity to review and manage all student feedback messages.
 */
class FeedbackMessagesActivity : EveBaseActivity() {

    private lateinit var binding: ActivityFeedbackMessagesBinding
    private val repo = FeedbackRepository()
    private lateinit var adapter: FeedbackMessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadMessages() }
        binding.swipeRefresh.setOnRefreshListener { loadMessages() }

        adapter = FeedbackMessagesAdapter(
            onItemClick = { message ->
                markMessageRead(message)
                showReplyDialog(message)
            },
            onReplyClick = { message ->
                showReplyDialog(message)
            },
            onDeleteClick = { message -> confirmDeleteMessage(message) }
        )

        binding.rvFeedbackMessages.layoutManager = LinearLayoutManager(this)
        binding.rvFeedbackMessages.adapter = adapter

        loadMessages()
    }

    private fun loadMessages() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.hide()
        binding.errorStateView.hide()

        lifecycleScope.launch {
            try {
                val messages = repo.getFeedbackMessages()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (messages.isEmpty()) {
                    adapter.submitList(emptyList())
                    binding.emptyStateView.show(
                        title = "No feedback messages yet",
                        message = "Messages sent by students will appear here."
                    )
                } else {
                    binding.emptyStateView.hide()
                    adapter.submitList(messages)
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.errorStateView.show(
                    type = ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Failed to load messages: ${e.localizedMessage.orEmpty()}",
                    onRetry = { loadMessages() }
                )
            }
        }
    }

    private fun markMessageRead(message: FeedbackMessage) {
        if (message.read) return
        lifecycleScope.launch {
            try {
                repo.markAsRead(message.id)
                loadMessages()
            } catch (_: Exception) { }
        }
    }

    private fun confirmDeleteMessage(message: FeedbackMessage) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Feedback")
            .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val originalList = adapter.currentList.toMutableList()
                val filtered = originalList.filter { it.id != message.id }
                adapter.submitList(filtered)
                if (filtered.isEmpty()) {
                    binding.emptyStateView.show(
                        title = "No feedback messages yet",
                        message = "Messages sent by students will appear here."
                    )
                }

                AppUndoBar.show(
                    context = this@FeedbackMessagesActivity,
                    message = "Message deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        adapter.submitList(originalList)
                        binding.emptyStateView.hide()
                        AppBulletin.show(this@FeedbackMessagesActivity, "Delete cancelled")
                    },
                    onExecuteDelete = {
                        deleteMessage(message.id)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(id: String) {
        lifecycleScope.launch {
            try {
                val result = repo.deleteFeedbackMessage(id)
                result.onSuccess {
                    AppBulletin.showSuccess(this@FeedbackMessagesActivity, "Message deleted")
                    loadMessages()
                }.onFailure { err ->
                    AppBulletin.showError(this@FeedbackMessagesActivity, "Failed to delete: ${err.message}")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@FeedbackMessagesActivity, "Error: ${e.message}")
            }
        }
    }

    private fun showReplyDialog(message: FeedbackMessage) {
        val dialogBinding = com.eve.app.databinding.DialogReplyFeedbackBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())

        dialogBinding.tvOriginalSenderName.text = message.userName.ifBlank { "Student" }
        dialogBinding.tvOriginalSenderEmail.text = message.userEmail.ifBlank { "No email provided" }
        dialogBinding.tvOriginalTimestamp.text = dateFormat.format(java.util.Date(message.timestamp))

        val bodyText = if (!message.postTitle.isNullOrEmpty()) {
            "📌 In response to: ${message.postTitle}\n\n${message.message}"
        } else {
            message.message
        }
        dialogBinding.tvOriginalMessageText.text = bodyText

        dialogBinding.btnCancelReply.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSendReply.setOnClickListener {
            val replyText = dialogBinding.etReplyText.text?.toString()?.trim().orEmpty()
            if (replyText.isBlank()) {
                dialogBinding.tilReply.error = "Reply cannot be empty"
                return@setOnClickListener
            }
            dialogBinding.tilReply.error = null

            dialogBinding.btnSendReply.isEnabled = false
            lifecycleScope.launch {
                try {
                    val result = repo.replyFeedbackMessage(message.id, replyText)
                    result.onSuccess {
                        AppBulletin.showSuccess(this@FeedbackMessagesActivity, "Reply sent successfully!")
                        dialog.dismiss()
                        loadMessages()
                    }.onFailure { err ->
                        dialogBinding.btnSendReply.isEnabled = true
                        AppBulletin.showError(this@FeedbackMessagesActivity, "Failed to send reply: ${err.message}")
                    }
                } catch (e: Exception) {
                    dialogBinding.btnSendReply.isEnabled = true
                    AppBulletin.showError(this@FeedbackMessagesActivity, "Error: ${e.message}")
                }
            }
        }

        dialog.show()
    }
}
