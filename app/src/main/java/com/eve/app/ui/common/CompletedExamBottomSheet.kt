package com.eve.app.ui.common

import android.content.Intent
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.databinding.BottomSheetCompletedExamBinding
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    ) {
        val dialog = BottomSheetDialog(activity)
        val binding = BottomSheetCompletedExamBinding.inflate(LayoutInflater.from(activity))
        dialog.setContentView(binding.root)

        binding.tvCompletedTitle.text = examName

        if (attempt != null) {
            val scoreStr = if (attempt.score % 1.0 == 0.0) attempt.score.toInt().toString() else attempt.score.toString()
            binding.tvCompletedScore.text = activity.getString(R.string.exam_completed_score, scoreStr, attempt.total)
        } else {
            binding.tvCompletedScore.text = activity.getString(R.string.exam_completed_badge)
        }

        fun openReview(att: TestAttempt) {
            ResultDataHolder.setAnswers(att.answers)
            val intent = Intent(activity, ResultActivity::class.java).apply {
                putExtra(Constants.EXTRA_EXAM_ID, att.examId)
                putExtra(Constants.EXTRA_EXAM_NAME, att.examName)
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                putExtra(Constants.EXTRA_ATTEMPT_DATE, sdf.format(Date(att.timestamp)))
                putExtra(Constants.EXTRA_FROM_HISTORY, true)
            }
            activity.startActivity(intent)
        }

        binding.btnViewResult.setOnClickListener {
            if (attempt != null && attempt.answers.isNotEmpty()) {
                dialog.dismiss()
                openReview(attempt)
            } else {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                activity.lifecycleScope.launch {
                    val attempts = HistoryRepository().getAttempts(uid)
                    val found = attempts.firstOrNull { it.examId == examId }
                    if (found != null && found.answers.isNotEmpty()) {
                        dialog.dismiss()
                        openReview(found)
                    } else {
                        AppBulletin.showError(activity, "Attempt details not found.")
                    }
                }
            }
        }

        binding.btnReattempt.setOnClickListener {
            activity.lifecycleScope.launch {
                val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(activity, examId)
                if (!canReattempt) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Attempt Limit Reached")
                        .setMessage("You have reached the maximum limit of 3 attempts for this test. Normal users can attempt each test at most 3 times.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.reattempt_confirm_title)
                    .setMessage("Are you sure you want to start a new attempt for this test? Each test can be attempted up to 3 times.")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton("Start Attempt") { _, _ ->
                        binding.btnReattempt.isEnabled = false
                        com.eve.app.ui.home.HomeViewModel.markAttemptCleared(examId)
                        dialog.dismiss()
                        onReattemptConfirmed()
                    }
                    .show()
            }
        }

        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()
    }
}
