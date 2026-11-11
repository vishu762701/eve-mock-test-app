package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import android.content.DialogInterface

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
import com.eve.app.util.AdminAccessGate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Task 2: Admin activity to review and manage all student feedback messages.
 */
class FeedbackMessagesActivity : EveBaseActivity() {

    private lateinit var binding: ActivityFeedbackMessagesBinding
    private val repo = FeedbackRepository()
    private lateinit var adapter: FeedbackMessagesAdapter
    private var replyDialog: TelegramNativeAlertDialog? = null
    private var deleteDialog: TelegramNativeAlertDialog? = null

    private var authorizedUid: String? = null
    private var authorizedEmail: String? = null
    private var reauthorizeOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.progressBar.visibility = View.VISIBLE

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "moderate_feedback") {
            authorizedUid = FirebaseAuth.getInstance().currentUser?.uid
            authorizedEmail = FirebaseAuth.getInstance().currentUser?.email
            setupAdminControls()
            loadMessages()
        }
    }

    private fun setupAdminControls() {
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
    }

    override fun onStop() {
        reauthorizeOnResume = true
        replyDialog?.dismiss()
        replyDialog = null
        deleteDialog?.dismiss()
        deleteDialog = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.emptyStateView.resetPlayOnNextShow()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
            if (!hasItems) {
                binding.progressBar.visibility = View.VISIBLE
                binding.swipeRefresh.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "moderate_feedback") {
                authorizedUid = live.uid
                authorizedEmail = live.email
                loadMessages(showProgress = !hasItems)
            }
        }
    }

    private fun loadMessages(showProgress: Boolean = true) {
        val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
        if (showProgress || !hasItems) {
            binding.progressBar.visibility = View.VISIBLE
            if (!hasItems) binding.swipeRefresh.visibility = View.GONE
        }
        binding.emptyStateView.hide()
        binding.errorStateView.hide()

        lifecycleScope.launch {
            try {
                val messages = repo.getFeedbackMessages()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.visibility = View.VISIBLE
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
                if (::adapter.isInitialized && adapter.itemCount > 0) {
                    binding.swipeRefresh.visibility = View.VISIBLE
                    AppBulletin.showError(this@FeedbackMessagesActivity, "Failed to refresh messages: ${e.localizedMessage.orEmpty()}")
                } else {
                    binding.errorStateView.show(
                        type = ErrorStateView.ErrorType.SERVER_ERROR,
                        customMessage = "Failed to load messages: ${e.localizedMessage.orEmpty()}",
                        onRetry = { loadMessages() }
                    )
                }
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
        if (isFinishing || isDestroyed) return
        deleteDialog?.dismiss()
        val dialog = TelegramNativeAlertDialog.Builder(this)
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
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
        deleteDialog = dialog
        dialog.setOnDismissListener { if (deleteDialog === dialog) deleteDialog = null }
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
        // Original AlertDialog.show(): Activity must be alive. An in-progress
        // reply owns one modal window, not a stack of duplicate confirmations.
        if (isFinishing || isDestroyed || replyDialog?.isShowing == true) return
        val dialogBinding = com.eve.app.databinding.DialogReplyFeedbackBinding.inflate(layoutInflater)
        val dialog = TelegramNativeAlertDialog.Builder(this)
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
        dialogBinding.etReplyText.setText(message.replyText)

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
                        AppBulletin.showSuccess(this@FeedbackMessagesActivity, "Reply saved.")
                        dialog.dismiss()
                        loadMessages()
                    }.onFailure { err ->
                        dialogBinding.btnSendReply.isEnabled = true
                        AppBulletin.showError(this@FeedbackMessagesActivity, "Failed to save reply: ${err.message}")
                    }
                } catch (e: Exception) {
                    dialogBinding.btnSendReply.isEnabled = true
                    AppBulletin.showError(this@FeedbackMessagesActivity, "Error: ${e.message}")
                }
            }
        }

        dialog.setOnDismissListener {
            if (replyDialog === dialog) replyDialog = null
        }
        dialog.show()
        replyDialog = dialog
        // The dependency-isolated Telegram host keeps the original custom form,
        // confirmation and asynchronous reply callback intact.
    }

    override fun onDestroy() {
        replyDialog?.dismiss()
        replyDialog = null
        deleteDialog?.dismiss()
        deleteDialog = null
        super.onDestroy()
    }
}
