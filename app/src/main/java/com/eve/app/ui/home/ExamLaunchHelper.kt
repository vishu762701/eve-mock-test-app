package com.eve.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleCoroutineScope
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.HistoryRepository
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExamLaunchHelper {

    fun openPreviousAttemptResult(
        context: Context,
        scope: LifecycleCoroutineScope,
        examId: String,
        examName: String,
        exam: Exam? = null,
        onLoading: ((Boolean) -> Unit)? = null
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            AppBulletin.showError(context, "User not authenticated")
            return
        }

        onLoading?.invoke(true)
        scope.launch {
            try {
                val lockRes = ApiClient.apiService.checkAttemptLock(examId)
                val lockTimestamp = lockRes.data?.timestamp
                val attempts = HistoryRepository().getAttempts(user.uid).filter { it.examId == examId }
                val targetAttempt = (if (lockTimestamp != null) {
                    attempts.find { it.timestamp == lockTimestamp }
                } else null) ?: attempts.maxByOrNull { it.timestamp } ?: HomeViewModel.getCachedAttempt(examId)

                if (targetAttempt != null) {
                    ResultDataHolder.setAnswers(targetAttempt.answers)
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val intent = Intent(context, ResultActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, examId)
                        putExtra(Constants.EXTRA_EXAM_NAME, examName)
                        putExtra(Constants.EXTRA_ATTEMPT_DATE, dateFormat.format(Date(targetAttempt.timestamp)))
                        putExtra(Constants.EXTRA_FROM_HISTORY, true)
                        putExtra(Constants.EXTRA_CAN_REATTEMPT, true)
                        if (exam != null) {
                            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
                            putExtra(Constants.EXTRA_NEGATIVE_MARKING, exam.negativeMarkingValue)
                            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                        }
                    }
                    context.startActivity(intent)
                } else {
                    AppBulletin.showError(context, "Couldn't load previous attempt")
                }
            } catch (e: Exception) {
                val fallback = HomeViewModel.getCachedAttempt(examId)
                if (fallback != null) {
                    ResultDataHolder.setAnswers(fallback.answers)
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val intent = Intent(context, ResultActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, examId)
                        putExtra(Constants.EXTRA_EXAM_NAME, examName)
                        putExtra(Constants.EXTRA_ATTEMPT_DATE, dateFormat.format(Date(fallback.timestamp)))
                        putExtra(Constants.EXTRA_FROM_HISTORY, true)
                        putExtra(Constants.EXTRA_CAN_REATTEMPT, true)
                        if (exam != null) {
                            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
                            putExtra(Constants.EXTRA_NEGATIVE_MARKING, exam.negativeMarkingValue)
                            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                        }
                    }
                    context.startActivity(intent)
                } else {
                    AppBulletin.showError(context, "Couldn't load previous attempt: ${e.localizedMessage}")
                }
            } finally {
                onLoading?.invoke(false)
            }
        }
    }
}
