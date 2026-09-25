package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.BroadcastMessage
import com.eve.app.databinding.ActivitySendNotificationBinding
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
    private val sentAdapter = SentBroadcastAdapter(
        onManage = { broadcast -> showManageBroadcastDialog(broadcast) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvSentBroadcasts.layoutManager = LinearLayoutManager(this)
        binding.rvSentBroadcasts.adapter = sentAdapter

        binding.btnSend.setOnClickListener {
            validateAndConfirm()
        }

        listenToSentBroadcasts()
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
                    Toast.makeText(this, "Failed to load sent broadcasts: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
        lifecycleScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(broadcast.id)
                    .delete()
                    .await()

                Toast.makeText(this@SendNotificationActivity, "Broadcast deleted successfully", Toast.LENGTH_SHORT).show()
                // Optimistically update adapter immediately
                val updated = sentAdapter.currentList.filter { it.id != broadcast.id }
                sentAdapter.submitList(updated)
                binding.tvNoSentBroadcasts.visibility = if (updated.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(
                    this@SendNotificationActivity,
                    "Failed to delete broadcast: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
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
