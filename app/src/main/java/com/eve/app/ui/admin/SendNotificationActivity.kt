package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.databinding.ActivitySendNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SendNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSend.setOnClickListener {
            validateAndConfirm()
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

                Toast.makeText(
                    this@SendNotificationActivity,
                    "Notification broadcast successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
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
