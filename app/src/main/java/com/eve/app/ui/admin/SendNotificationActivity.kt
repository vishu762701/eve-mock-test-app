package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.databinding.ActivitySendNotificationBinding
import com.eve.app.ui.common.ErrorStateView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SendNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendNotificationBinding
    private var sentListener: ListenerRegistration? = null
    private lateinit var sentAdapter: SentBroadcastAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sentAdapter = SentBroadcastAdapter(
            onDelete = { broadcast ->
                if (!sentAdapter.isSelectionMode) {
                    confirmDeleteBroadcast(broadcast)
                }
            },
            onItemClick = { broadcast ->
                if (sentAdapter.isSelectionMode) {
                    sentAdapter.toggleSelection(broadcast.id)
                    updateSelectionCountUI()
                }
            },
            onItemLongClick = { broadcast ->
                if (!sentAdapter.isSelectionMode) {
                    enterSelectionMode(broadcast.id)
                } else {
                    sentAdapter.toggleSelection(broadcast.id)
                    updateSelectionCountUI()
                }
            }
        )

        binding.compactErrorView.displayMode = ErrorStateView.DisplayMode.COMPACT

        binding.btnBack.setOnClickListener { finish() }

        binding.rvSentBroadcasts.layoutManager = LinearLayoutManager(this)
        binding.rvSentBroadcasts.adapter = sentAdapter

        binding.btnSend.setOnClickListener {
            validateAndConfirm()
        }

        setupSelectionTopBar()
        setupBackPressed()
        listenToSentBroadcasts()
    }

    private fun setupSelectionTopBar() {
        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

        binding.btnSelectAll.setOnClickListener {
            val allIds = sentAdapter.currentList.map { it.id }
            if (sentAdapter.selectedIds.size == allIds.size && allIds.isNotEmpty()) {
                sentAdapter.clearSelection()
            } else {
                sentAdapter.selectAll(allIds)
            }
            updateSelectionCountUI()
        }

        binding.btnDeleteSelected.setOnClickListener {
            confirmDeleteSelected()
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (sentAdapter.isSelectionMode) {
                    exitSelectionMode()
                } else {
                    finish()
                }
            }
        })
    }

    private fun enterSelectionMode(initialSelectedId: String) {
        sentAdapter.isSelectionMode = true
        sentAdapter.selectedIds.clear()
        sentAdapter.selectedIds.add(initialSelectedId)
        sentAdapter.notifyDataSetChanged()

        binding.compactErrorView.visibility = View.GONE
        binding.normalTopBar.visibility = View.GONE
        binding.selectionTopBar.visibility = View.VISIBLE
        updateSelectionCountUI()
    }

    private fun exitSelectionMode() {
        sentAdapter.clearSelection()
        sentAdapter.isSelectionMode = false
        sentAdapter.notifyDataSetChanged()

        binding.compactErrorView.visibility = View.GONE
        binding.selectionTopBar.visibility = View.GONE
        binding.normalTopBar.visibility = View.VISIBLE
    }

    private fun updateSelectionCountUI() {
        val count = sentAdapter.selectedIds.size
        binding.tvSelectedCount.text = "$count selected"
        binding.btnDeleteSelected.isEnabled = count > 0
        binding.btnDeleteSelected.alpha = if (count > 0) 1.0f else 0.4f

        val total = sentAdapter.currentList.size
        binding.btnSelectAll.text = if (count > 0 && count == total) "Deselect all" else "Select all"
    }

    private fun confirmDeleteSelected() {
        val count = sentAdapter.selectedIds.size
        if (count == 0) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Broadcasts?")
            .setMessage("Delete $count broadcasts? They will be removed from all students' notification lists.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedBroadcasts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedBroadcasts() {
        val idsToDelete = sentAdapter.selectedIds.toList()
        if (idsToDelete.isEmpty()) return

        binding.progressBarSent.visibility = View.VISIBLE
        binding.compactErrorView.visibility = View.GONE

        lifecycleScope.launch {
            val db = FirebaseFirestore.getInstance()
            val chunks = idsToDelete.chunked(500)
            var successCount = 0
            var failCount = 0
            var lastError: Exception? = null

            for (chunk in chunks) {
                try {
                    val batch = db.batch()
                    for (id in chunk) {
                        batch.delete(db.collection("notifications").document(id))
                    }
                    batch.commit().await()
                    successCount += chunk.size
                } catch (e: Exception) {
                    failCount += chunk.size
                    lastError = e
                }
            }

            binding.progressBarSent.visibility = View.GONE

            if (failCount > 0) {
                val errorMsg = if (successCount > 0) {
                    "Deleted $successCount broadcasts, but $failCount failed: ${lastError?.localizedMessage ?: "Unknown error"}"
                } else {
                    "Failed to delete broadcasts: ${lastError?.localizedMessage ?: "Unknown error"}"
                }
                binding.compactErrorView.visibility = View.VISIBLE
                binding.compactErrorView.show(
                    type = ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = errorMsg,
                    customTitle = "Delete Failed",
                    onRetry = { deleteSelectedBroadcasts() }
                )
                Toast.makeText(this@SendNotificationActivity, errorMsg, Toast.LENGTH_LONG).show()
                // Keep remaining failed items selected
                val remainingIds = idsToDelete.takeLast(failCount)
                sentAdapter.selectAll(remainingIds)
                updateSelectionCountUI()
            } else {
                Toast.makeText(
                    this@SendNotificationActivity,
                    "Successfully deleted $successCount broadcast${if (successCount > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                exitSelectionMode()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sentListener?.remove()
    }

    private fun listenToSentBroadcasts() {
        binding.progressBarSent.visibility = View.VISIBLE
        sentListener = FirebaseFirestore.getInstance()
            .collection("notifications")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                binding.progressBarSent.visibility = View.GONE
                if (error != null) {
                    binding.compactErrorView.visibility = View.VISIBLE
                    binding.compactErrorView.show(
                        type = ErrorStateView.ErrorType.SERVER_ERROR,
                        customMessage = "Failed to load sent broadcasts: ${error.localizedMessage}",
                        customTitle = "Loading Error",
                        onRetry = { listenToSentBroadcasts() }
                    )
                    return@addSnapshotListener
                }
                binding.compactErrorView.visibility = View.GONE
                val broadcasts = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: return@mapNotNull null
                    val sentAt = doc.getTimestamp("sentAt")?.toDate()?.time
                        ?: doc.getLong("sentAt")
                        ?: 0L
                    val sentBy = doc.getString("sentBy").orEmpty()
                    val type = doc.getString("type") ?: "general"
                    BroadcastMessage(
                        id = doc.id,
                        title = title,
                        message = message,
                        sentAt = sentAt,
                        sentBy = sentBy,
                        type = type
                    )
                }.orEmpty()

                sentAdapter.submitList(broadcasts)
                binding.tvNoSentBroadcasts.visibility = if (broadcasts.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun showManageBroadcastDialog(broadcast: BroadcastMessage) {
        val title = if (broadcast.title.isNotBlank()) broadcast.title else "Broadcast Message"
        val options = arrayOf("🗑️ Delete This Message", "Cancel")
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(broadcast.message)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> confirmDeleteBroadcast(broadcast)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun confirmDeleteBroadcast(broadcast: BroadcastMessage) {
        val titleText = if (broadcast.title.isNotBlank()) "'${broadcast.title}'" else "this message"
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Broadcast?")
            .setMessage("Delete $titleText? Only this individual broadcast will be deleted, leaving all other messages intact.")
            .setPositiveButton("Delete") { _, _ ->
                deleteBroadcast(broadcast)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBroadcast(broadcast: BroadcastMessage) {
        if (broadcast.id.isBlank()) {
            Toast.makeText(this, "Cannot delete broadcast: Invalid document ID", Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBarSent.visibility = View.VISIBLE
        binding.compactErrorView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(broadcast.id)
                    .delete()
                    .await()

                Toast.makeText(this@SendNotificationActivity, "Broadcast deleted successfully", Toast.LENGTH_SHORT).show()
                val updated = sentAdapter.currentList.filter { it.id != broadcast.id }
                sentAdapter.submitList(updated)
                binding.tvNoSentBroadcasts.visibility = if (updated.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                val err = "Failed to delete broadcast: ${e.localizedMessage ?: "Unknown error"}"
                binding.compactErrorView.visibility = View.VISIBLE
                binding.compactErrorView.show(
                    type = ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = err,
                    customTitle = "Delete Error"
                )
                Toast.makeText(this@SendNotificationActivity, err, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBarSent.visibility = View.GONE
            }
        }
    }

    private fun validateAndConfirm() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val message = binding.etMessage.text?.toString()?.trim().orEmpty()

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            binding.etTitle.requestFocus()
            return
        } else {
            binding.tilTitle.error = null
        }

        if (message.isEmpty()) {
            binding.tilMessage.error = "Message cannot be empty"
            binding.etMessage.requestFocus()
            return
        } else {
            binding.tilMessage.error = null
        }

        AlertDialog.Builder(this)
            .setTitle("Broadcast Notification")
            .setMessage("Are you sure you want to send this notification to all users?")
            .setPositiveButton("Send") { _, _ ->
                sendNotification(title, message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendNotification(title: String, message: String) {
        binding.btnSend.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val adminEmail = FirebaseAuth.getInstance().currentUser?.email ?: "admin"

        val doc = hashMapOf(
            "title" to title,
            "message" to message,
            "sentAt" to FieldValue.serverTimestamp(),
            "sentBy" to adminEmail,
            "type" to "general"
        )

        lifecycleScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .add(doc)
                    .await()

                binding.btnSend.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.etTitle.text?.clear()
                binding.etMessage.text?.clear()

                Toast.makeText(
                    this@SendNotificationActivity,
                    "Notification broadcast successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                binding.btnSend.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@SendNotificationActivity,
                    "Failed to send: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
