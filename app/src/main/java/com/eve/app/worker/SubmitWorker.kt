package com.eve.app.worker

import android.content.Context
import androidx.work.*
import com.eve.app.data.local.PendingSubmissionStore
import com.eve.app.data.remote.ApiClient
import com.eve.app.util.NotificationHelper
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class SubmitWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val clientAttemptId = inputData.getString(KEY_CLIENT_ATTEMPT_ID) ?: return Result.failure()
        val store = PendingSubmissionStore(applicationContext)
        val pending = store.get(clientAttemptId) ?: return Result.success()

        val currentUser = resolveCurrentUser()
        val authState = SubmissionRetryPolicy.evaluateAuthState(pending.userId, currentUser?.uid)

        // Only the verified original student can submit this payload.
        // Never submit under another student's account; retain unowned or mismatched files safely for the original student.
        if (authState != SubmissionRetryPolicy.AuthState.ELIGIBLE) {
            return Result.failure()
        }

        val body = mutableMapOf<String, Any>(
            "examId" to pending.examId,
            "expectedUid" to pending.userId,
            "practice" to pending.practice,
            "examName" to pending.examName,
            "category" to pending.category,
            "clientAttemptId" to pending.clientAttemptId,
            "answers" to pending.answers.map {
                mapOf(
                    "questionId" to it.questionId,
                    "number" to it.number,
                    "selected" to it.selected,
                    "isBookmarked" to it.isBookmarked,
                    "timeTakenSeconds" to it.timeTakenSeconds
                )
            }
        )
        if (!pending.topic.isNullOrBlank()) {
            body["topic"] = pending.topic
        }
        if (pending.pyqYear != null && pending.pyqYear > 0) {
            body["pyqYear"] = pending.pyqYear
        }
        if (!pending.pyqPaper.isNullOrBlank()) {
            body["pyqPaper"] = pending.pyqPaper
        }

        return try {
            val response = ApiClient.api.submitAttempt(body)
            if (response.success && response.data != null) {
                store.remove(clientAttemptId)
                com.eve.app.data.local.TestSessionStore(applicationContext).clearSession(pending.examId, pending.userId)
                if (!pending.practice && pending.topic.isNullOrBlank() && (pending.pyqYear ?: 0) == 0) {
                    com.eve.app.util.AttemptLimitManager.recordAttempt(applicationContext, pending.examId, response.data.attemptId, pending.userId)
                }
                NotificationHelper.showSubmissionResult(applicationContext, pending.examName, success = true)
                Result.success()
            } else {
                NotificationHelper.showSubmissionResult(applicationContext, pending.examName, success = false)
                Result.failure()
            }
        } catch (e: HttpException) {
            val code = e.code()
            if (com.eve.app.util.SubmissionRetryPolicy.shouldRetryHttp(code)) return Result.retry()
            if (code in 400..499) {
                NotificationHelper.showSubmissionResult(applicationContext, pending.examName, success = false)
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun resolveCurrentUser(): FirebaseUser? {
        val immediate = FirebaseAuth.getInstance().currentUser
        if (immediate != null) return immediate

        // Allow cold-start Firebase Auth internal credential hydration to complete before evaluating eligibility.
        return try {
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { cont ->
                    val auth = FirebaseAuth.getInstance()
                    var listener: FirebaseAuth.AuthStateListener? = null
                    listener = FirebaseAuth.AuthStateListener { fa ->
                        val user = fa.currentUser
                        if (user != null && cont.isActive) {
                            listener?.let { auth.removeAuthStateListener(it) }
                            cont.resume(user)
                        }
                    }
                    auth.addAuthStateListener(listener)
                    cont.invokeOnCancellation {
                        listener?.let { auth.removeAuthStateListener(it) }
                    }
                }
            } ?: FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            FirebaseAuth.getInstance().currentUser
        }
    }

    companion object {
        const val KEY_CLIENT_ATTEMPT_ID = "client_attempt_id"

        fun enqueue(context: Context, clientAttemptId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SubmitWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(workDataOf(KEY_CLIENT_ATTEMPT_ID to clientAttemptId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "submit_$clientAttemptId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun enqueueAllPending(context: Context) {
            val store = PendingSubmissionStore(context)
            for (item in store.list()) {
                enqueue(context, item.clientAttemptId)
            }
        }
    }
}
