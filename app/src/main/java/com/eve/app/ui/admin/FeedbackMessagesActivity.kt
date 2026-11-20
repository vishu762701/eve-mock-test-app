package com.eve.app.ui.admin

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.FeedbackMessage
import com.eve.app.data.repository.FeedbackRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Admin Feedback Messages screen powered by real ported Telegram UI components:
 * ActionBar (with refresh), RecyclerListView, EmptyTextProgressView, and ButtonWithCounterView,
 * while preserving FeedbackMessagesAdapter, AppUndoBar, reply persistence, and RBAC.
 */
class FeedbackMessagesActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private val repo = FeedbackRepository()
    private lateinit var adapter: FeedbackMessagesAdapter
    private var replyDialog: AlertDialog? = null
    private var deleteDialog: AlertDialog? = null

    private var authorizedUid: String? = null
    private var authorizedEmail: String? = null
    private var reauthorizeOnResume = false

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Feedback Messages")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (::adapter.isInitialized) loadMessages()
                    }
                }
            })
        }
        rootView.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@FeedbackMessagesActivity)
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
            visibility = View.GONE
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No feedback messages yet\nMessages sent by students will appear here.")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        retryButton = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Retry", false)
            visibility = View.GONE
            setOnClickListener { loadMessages() }
        }
        contentFrame.addView(
            retryButton,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48f,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                24f,
                0f,
                24f,
                32f
            )
        )

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "moderate_feedback") {
            authorizedUid = FirebaseAuth.getInstance().currentUser?.uid
            authorizedEmail = FirebaseAuth.getInstance().currentUser?.email
            setupAdminControls()
            loadMessages()
        }
    }

    private fun setupAdminControls() {
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
        listView.adapter = adapter
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
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
            if (!hasItems) {
                listView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.showProgress()
            }
            AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "moderate_feedback") {
                authorizedUid = live.uid
                authorizedEmail = live.email
                loadMessages(showProgress = !hasItems)
            }
        }
    }

    private fun loadMessages(showProgress: Boolean = true) {
        val hasItems = ::adapter.isInitialized && adapter.itemCount > 0
        retryButton.visibility = View.GONE
        if (showProgress && !hasItems) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        lifecycleScope.launch {
            try {
                val messages = repo.getFeedbackMessages()
                if (messages.isEmpty()) {
                    adapter.submitList(emptyList())
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No feedback messages yet\nMessages sent by students will appear here.")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                } else {
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                    adapter.submitList(messages)
                    actionBar.setSubtitle("${messages.size} ${if (messages.size == 1) "message" else "messages"}")
                }
            } catch (e: Exception) {
                if (::adapter.isInitialized && adapter.itemCount > 0) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    AppBulletin.showError(
                        this@FeedbackMessagesActivity,
                        "Failed to refresh messages: ${e.localizedMessage.orEmpty()}"
                    )
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Failed to load messages\n${e.localizedMessage.orEmpty()}")
                    emptyView.showTextView()
                    retryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun markMessageRead(message: FeedbackMessage) {
        if (message.read) return
        lifecycleScope.launch {
            try {
                repo.markAsRead(message.id)
                loadMessages(showProgress = false)
            } catch (_: Exception) { }
        }
    }

    private fun confirmDeleteMessage(message: FeedbackMessage) {
        if (isFinishing || isDestroyed) return
        deleteDialog?.dismiss()
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Feedback")
            .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val originalList = adapter.currentList.toMutableList()
                val filtered = originalList.filter { it.id != message.id }
                adapter.submitList(filtered)
                if (filtered.isEmpty()) {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No feedback messages yet\nMessages sent by students will appear here.")
                    emptyView.showTextView()
                }

                AppUndoBar.show(
                    context = this@FeedbackMessagesActivity,
                    message = "Message deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        adapter.submitList(originalList)
                        emptyView.visibility = View.GONE
                        listView.visibility = View.VISIBLE
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
                    loadMessages(showProgress = false)
                }.onFailure { err ->
                    AppBulletin.showError(this@FeedbackMessagesActivity, "Failed to delete: ${err.message}")
                }
            } catch (e: Exception) {
                AppBulletin.showError(this@FeedbackMessagesActivity, "Error: ${e.message}")
            }
        }
    }

    private fun showReplyDialog(message: FeedbackMessage) {
        if (isFinishing || isDestroyed || replyDialog?.isShowing == true) return
        val dialogBinding = com.eve.app.databinding.DialogReplyFeedbackBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
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
                        loadMessages(showProgress = false)
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
    }

    override fun onDestroy() {
        replyDialog?.dismiss()
        replyDialog = null
        deleteDialog?.dismiss()
        deleteDialog = null
        super.onDestroy()
    }
}
