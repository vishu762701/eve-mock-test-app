package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
class FeedbackMessagesActivity : AppCompatActivity() {

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
            onItemClick = { message -> markMessageRead(message) },
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
                deleteMessage(message.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(id: String) {
        lifecycleScope.launch {
            try {
                val result = repo.deleteFeedbackMessage(id)
                result.onSuccess {
                    Toast.makeText(this@FeedbackMessagesActivity, "Message deleted", Toast.LENGTH_SHORT).show()
                    loadMessages()
                }.onFailure { err ->
                    Toast.makeText(this@FeedbackMessagesActivity, "Failed to delete: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackMessagesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
