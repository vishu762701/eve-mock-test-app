package com.eve.app.ui.common

import android.content.Intent
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.designsystem.TelegramNativeAlertDialog
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.databinding.BottomSheetCompletedExamBinding
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CompletedExamBottomSheet {

    fun show(
        activity: AppCompatActivity,
        examId: String,
        examName: String,
        attempt: TestAttempt?,
        onReattemptConfirmed: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ): TelegramBottomSheet? {
        val ownerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (activity.isFinishing || activity.isDestroyed || attempt != null && attempt.userId != ownerUid) return null
        val binding = BottomSheetCompletedExamBinding.inflate(LayoutInflater.from(activity))
        val dialog = TelegramBottomSheet.Builder(activity)
            .setCustomView(binding.root)
            .create()

        binding.tvCompletedTitle.text = examName

        if (attempt != null) {
            val scoreStr = if (attempt.score % 1.0 == 0.0) attempt.score.toInt().toString() else attempt.score.toString()
            binding.tvCompletedScore.text = activity.getString(R.string.exam_completed_score, scoreStr, attempt.total)
        } else {
            binding.tvCompletedScore.text = activity.getString(R.string.exam_completed_badge)
        }

        fun openReview(att: TestAttempt): Boolean {
            if (activity.isFinishing || activity.isDestroyed || att.userId != ownerUid ||
                !ResultDataHolder.setAnswers(att.answers, ownerUid, att.examId)) return false
            val intent = Intent(activity, ResultActivity::class.java).apply {
                putExtra(Constants.EXTRA_RESULT_OWNER_UID, ownerUid)
                putExtra(Constants.EXTRA_EXAM_ID, att.examId)
                putExtra(Constants.EXTRA_EXAM_NAME, att.examName)
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                putExtra(Constants.EXTRA_ATTEMPT_DATE, sdf.format(Date(att.timestamp)))
                putExtra(Constants.EXTRA_FROM_HISTORY, true)
            }
            activity.startActivity(intent)
            return true
        }

        var reviewPending = false
        binding.btnViewResult.setOnClickListener {
            if (reviewPending || activity.isFinishing || activity.isDestroyed ||
                !dialog.isShowing || FirebaseAuth.getInstance().currentUser?.uid != ownerUid) {
                return@setOnClickListener
            }
            if (attempt != null && attempt.answers.isNotEmpty()) {
                // Start review before dismiss: the host's onDismiss callback finishes TestActivity.
                if (openReview(attempt)) dialog.dismiss()
            } else {
                reviewPending = true
                activity.lifecycleScope.launch {
                    try {
                        val attempts = HistoryRepository().getAttempts(ownerUid)
                        // The Worker request may finish after a different account signs in
                        // or the original bottom sheet has already been dismissed.
                        if (activity.isFinishing || activity.isDestroyed || !dialog.isShowing ||
                            FirebaseAuth.getInstance().currentUser?.uid != ownerUid) return@launch
                        val found = attempts.firstOrNull {
                            it.userId == ownerUid && it.examId == examId
                        }
                        if (found != null && found.answers.isNotEmpty()) {
                            if (openReview(found)) dialog.dismiss()
                        } else {
                            AppBulletin.showError(activity, "Attempt details not found.")
                        }
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing &&
                            FirebaseAuth.getInstance().currentUser?.uid == ownerUid) {
                            AppBulletin.showError(activity, "Could not load attempt details. Please retry.")
                        }
                    } finally {
                        reviewPending = false
                    }
                }
            }
        }

        var reattemptCheckPending = false
        var reattemptConfirmDialog: TelegramNativeAlertDialog? = null
        binding.btnReattempt.setOnClickListener {
            // One attempt-limit lookup and one confirmation window per tap sequence.
            if (reattemptCheckPending || reattemptConfirmDialog?.isShowing == true ||
                activity.isFinishing || activity.isDestroyed || !dialog.isShowing ||
                FirebaseAuth.getInstance().currentUser?.uid != ownerUid) {
                return@setOnClickListener
            }
            reattemptCheckPending = true
            activity.lifecycleScope.launch {
                try {
                    val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(activity, examId)
                    // An asynchronous account/attempt lookup must never display a
                    // late dialog after navigation or host Activity destruction.
                    if (activity.isFinishing || activity.isDestroyed || !dialog.isShowing ||
                        FirebaseAuth.getInstance().currentUser?.uid != ownerUid) return@launch
                    val builder = if (!canReattempt) {
                        TelegramNativeAlertDialog.Builder(activity)
                            .setTitle("Attempt Limit Reached")
                            .setMessage("You have reached the maximum limit of 3 attempts for this test. Normal users can attempt each test at most 3 times.")
                            .setPositiveButton("OK", null)
                    } else {
                        TelegramNativeAlertDialog.Builder(activity)
                            .setTitle(activity.getString(R.string.reattempt_confirm_title))
                            .setMessage("Are you sure you want to start a new attempt for this test? Each test can be attempted up to 3 times.")
                            .setNegativeButton(activity.getString(R.string.cancel), null)
                            .setPositiveButton("Start Attempt") { _, _ ->
                                if (FirebaseAuth.getInstance().currentUser?.uid != ownerUid ||
                                    activity.isFinishing || activity.isDestroyed) return@setPositiveButton
                                binding.btnReattempt.isEnabled = false
                                com.eve.app.ui.home.HomeViewModel.markAttemptCleared(examId, ownerUid)
                                dialog.dismiss()
                                onReattemptConfirmed()
                            }
                    }
                    val confirmation = builder.create()
                    confirmation.setOnDismissListener { dismissed ->
                        if (reattemptConfirmDialog === dismissed) reattemptConfirmDialog = null
                    }
                    confirmation.show()
                    reattemptConfirmDialog = confirmation
                    // Dialog uses the source-isolated Telegram UI host; BottomSheet remains separate.
                } finally {
                    reattemptCheckPending = false
                }
            }
        }

        dialog.setOnDismissListener {
            reattemptConfirmDialog?.dismiss()
            reattemptConfirmDialog = null
            onDismiss?.invoke()
        }

        dialog.show()
        return dialog
    }
}
