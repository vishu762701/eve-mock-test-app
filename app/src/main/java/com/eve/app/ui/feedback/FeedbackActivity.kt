package com.eve.app.ui.feedback

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.TelegramShake
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Feedback submission screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, EditTextBoldCursor, ShadowSectionCell,
 * TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class FeedbackActivity : EveBaseActivity() {

    private val repo = FeedbackRepository()
    private val feedbackAuth = FirebaseAuth.getInstance()
    private var feedbackOwnerUid: String? = null
    private var sendFeedbackJob: kotlinx.coroutines.Job? = null
    private var uiInitialized = false

    private lateinit var etMessage: EditTextBoldCursor
    private lateinit var btnSend: ButtonWithCounterView

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

        Theme.syncFromEvePalette(this)

        val postId = intent.getStringExtra("post_id")
        val postTitle = intent.getStringExtra("post_title")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        // 1. Real Telegram ActionBar
        val actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Send Feedback")
            if (!postTitle.isNullOrEmpty()) {
                setSubtitle("Responding to: $postTitle")
            }
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        root.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(content, matchWrap())
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 2. Message Input Card
        content.addView(HeaderCell(this).apply {
            setText("Your Message")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val editorContainer = FrameLayout(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(21f)
            val padV = AndroidUtilities.dp(8f)
            setPadding(padH, padV, padH, AndroidUtilities.dp(16f))
        }
        etMessage = EditTextBoldCursor(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)
            background = null
            hint = "Share suggestions, report an issue, or ask a question..."
            gravity = Gravity.TOP or Gravity.START
            minLines = 5
            maxLines = 10
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(1000))
        }
        editorContainer.addView(
            etMessage,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        content.addView(editorContainer, matchWrap())

        content.addView(ShadowSectionCell(this), matchWrap())
        content.addView(TextInfoPrivacyCell(this).apply {
            setText("Maximum 1000 characters. Your message goes directly to the Eve team.")
        }, matchWrap())

        // 3. Real Telegram ButtonWithCounterView CTA
        btnSend = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Send Feedback", false)
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(48f)
        ).apply {
            leftMargin = AndroidUtilities.dp(16f)
            rightMargin = AndroidUtilities.dp(16f)
            topMargin = AndroidUtilities.dp(8f)
        }
        content.addView(btnSend, btnParams)

        uiInitialized = true

        btnSend.setOnClickListener {
            if (!hasFeedbackOwner()) { finish(); return@setOnClickListener }
            val message = etMessage.text?.toString()?.trim().orEmpty()
            if (message.isEmpty()) {
                TelegramShake.shakeViewSpring(etMessage)
                AppBulletin.showError(this, "Please enter your message")
                return@setOnClickListener
            }
            if (message.length > 1000) {
                TelegramShake.shakeViewSpring(etMessage)
                AppBulletin.showError(this, "Message cannot exceed 1000 characters")
                return@setOnClickListener
            }

            val user = feedbackAuth.currentUser
            val uid = user?.uid ?: return@setOnClickListener
            val name = user.displayName ?: "Student"
            val email = user.email ?: ""

            btnSend.setLoading(true)

            sendFeedbackJob?.cancel()
            sendFeedbackJob = lifecycleScope.launch {
                val result = repo.sendFeedback(uid, name, email, message, postId, postTitle)
                if (isFinishing || isDestroyed) return@launch
                btnSend.setLoading(false)

                result.onSuccess {
                    AppBulletin.showSuccess(this@FeedbackActivity, "Thank you! Your feedback has been sent.")
                    etMessage.text = null
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
        if (!hasFeedbackOwner()) { finish(); return }
        if (!uiInitialized) return
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("feedback_owner_uid", feedbackOwnerUid)
    }

    override fun onDestroy() {
        sendFeedbackJob?.cancel()
        sendFeedbackJob = null
        if (uiInitialized) {
            btnSend.setLoading(false)
        }
        super.onDestroy()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
