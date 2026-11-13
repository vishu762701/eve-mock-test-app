package com.eve.app.ui.feedback

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityFeedbackBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Task 2: Free-text feedback submission activity.
 */
class FeedbackActivity : EveBaseActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val repo = FeedbackRepository()
    private val feedbackAuth = FirebaseAuth.getInstance()
    private var feedbackOwnerUid: String? = null
    private var sendFeedbackJob: kotlinx.coroutines.Job? = null

    private fun hasFeedbackOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(feedbackOwnerUid, feedbackAuth.currentUser?.uid)

    private val feedbackAuthListener = FirebaseAuth.AuthStateListener {
        if (feedbackOwnerUid != null && !hasFeedbackOwner()) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = feedbackAuth.currentUser?.uid
        feedbackOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("feedback_owner_uid")
        if (!hasFeedbackOwner()) { finish(); return }

        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) { finish() }

        val postId = intent.getStringExtra("post_id")
        val postTitle = intent.getStringExtra("post_title")
        if (!postTitle.isNullOrEmpty()) {
            binding.tvFeedbackSubtitle.text = "Responding to post: $postTitle"
        }

        com.eve.app.designsystem.DesignPress.bind(binding.btnSendFeedback)
        binding.btnSendFeedback.setOnClickListener {
            if (!hasFeedbackOwner()) { finish(); return@setOnClickListener }
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

            val user = feedbackAuth.currentUser
            val uid = user?.uid ?: return@setOnClickListener
            val name = user.displayName ?: "Student"
            val email = user.email ?: ""

            binding.progressBar.visibility = View.GONE
            binding.btnSendFeedback.setLoading(true)

            sendFeedbackJob?.cancel()
            sendFeedbackJob = lifecycleScope.launch {
                val result = repo.sendFeedback(uid, name, email, message, postId, postTitle)
                if (isFinishing || isDestroyed) return@launch
                binding.btnSendFeedback.setLoading(false)

                result.onSuccess {
                    AppBulletin.showSuccess(this@FeedbackActivity, "Thank you! Your feedback has been sent.")
                    binding.etFeedbackMessage.text = null
                    finish()
                }.onFailure { err ->
                    AppBulletin.showError(this@FeedbackActivity, "Failed to send: ${err.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasFeedbackOwner()) { finish(); return }
        feedbackAuth.addAuthStateListener(feedbackAuthListener)
    }

    override fun onStop() {
        feedbackAuth.removeAuthStateListener(feedbackAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasFeedbackOwner()) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("feedback_owner_uid", feedbackOwnerUid)
    }

    override fun onDestroy() {
        sendFeedbackJob?.cancel()
        sendFeedbackJob = null
        if (::binding.isInitialized) {
            binding.btnSendFeedback.setLoading(false)
        }
        super.onDestroy()
    }
}
