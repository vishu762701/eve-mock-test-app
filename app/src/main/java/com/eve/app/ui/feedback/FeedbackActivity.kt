package com.eve.app.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityFeedbackBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Task 2: Free-text feedback submission activity.
 */
class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val repo = FeedbackRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val postId = intent.getStringExtra("post_id")
        val postTitle = intent.getStringExtra("post_title")
        if (!postTitle.isNullOrEmpty()) {
            binding.tvFeedbackSubtitle.text = "Responding to post: $postTitle"
        }

        binding.btnSendFeedback.setOnClickListener {
            val message = binding.etFeedbackMessage.text?.toString()?.trim().orEmpty()
            if (message.isEmpty()) {
                binding.tilFeedback.error = "Please enter your message"
                return@setOnClickListener
            }
            if (message.length > 1000) {
                binding.tilFeedback.error = "Message cannot exceed 1000 characters"
                return@setOnClickListener
            }
            binding.tilFeedback.error = null

            val user = FirebaseAuth.getInstance().currentUser
            val uid = user?.uid ?: return@setOnClickListener
            val name = user.displayName ?: "Student"
            val email = user.email ?: ""

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSendFeedback.isEnabled = false

            lifecycleScope.launch {
                val result = repo.sendFeedback(uid, name, email, message, postId, postTitle)
                binding.progressBar.visibility = View.GONE
                binding.btnSendFeedback.isEnabled = true

                result.onSuccess {
                    Toast.makeText(this@FeedbackActivity, "Thank you! Your feedback has been sent.", Toast.LENGTH_LONG).show()
                    binding.etFeedbackMessage.text = null
                    finish()
                }.onFailure { err ->
                    Toast.makeText(this@FeedbackActivity, "Failed to send: ${err.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
