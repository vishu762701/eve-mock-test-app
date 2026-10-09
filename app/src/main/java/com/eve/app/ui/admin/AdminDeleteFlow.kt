package com.eve.app.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.AppBulletin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Honest permanent deletion: no optimistic removal or replacement-ID Undo. */
class AdminDeleteFlow(private val activity: AppCompatActivity, private val api: com.eve.app.data.remote.EveApiService = ApiClient.apiService) {
    private val pending = mutableSetOf<String>()
    private val repository = ExamRepository(api)

    fun confirm(id: String, name: String, exam: Boolean, refresh: () -> Unit) {
        val key = "$exam:$id"
        if (!pending.add(key)) return
        activity.lifecycleScope.launch {
            try {
                val response = if (exam) api.examDeletionInfo(id) else api.testDeletionInfo(id)
                val info = response.data
                check(response.success && info != null) { response.error ?: "Could not check deletion dependencies" }
                var processing = false
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(if (info.canDelete) "Permanently delete $name?" else "Deletion blocked")
                    .setMessage(info.message ?: "No saved attempts, sessions or dependent sub-exams were found. This permanently deletes the record and its questions. It cannot be undone.")
                    .setNegativeButton("Cancel", null)
                if (info.canDelete || info.safeAction == "unpublish") {
                    dialog.setPositiveButton(if (info.canDelete) "Delete" else "Unpublish tests") { _, _ ->
                        processing = true
                        activity.lifecycleScope.launch {
                            try {
                                if (info.canDelete) {
                                    val notice = if (exam) repository.deleteExam(id) else { repository.deleteGeneratedTest(id); null }
                                    if (notice == null) AppBulletin.showSuccess(activity, "Deleted") else AppBulletin.show(activity, notice)
                                } else {
                                    if (exam) {
                                        val result = api.unpublishExamTests(id)
                                        check(result.success) { result.error ?: "Could not unpublish tests" }
                                    } else repository.updateGeneratedTestStatus(id, "paused")
                                    AppBulletin.showSuccess(activity, "Tests unpublished. Student history is preserved.")
                                }
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { showFailure(e) }
                            finally { pending.remove(key); refresh() }
                        }
                    }
                }
                dialog.setOnDismissListener { if (!processing) pending.remove(key) }.show()
            } catch (e: CancellationException) { pending.remove(key); throw e }
            catch (e: Exception) { pending.remove(key); showFailure(e); refresh() }
        }
    }

    fun confirmRemoval(key: String, name: String, delete: suspend () -> String?, refresh: () -> Unit) {
        if (!pending.add(key)) return
        var processing = false
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permanently delete $name?")
            .setMessage("This removes the record after server confirmation. It cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                processing = true
                activity.lifecycleScope.launch {
                    try {
                        val notice = delete()
                        if (notice == null) AppBulletin.showSuccess(activity, "Deleted") else AppBulletin.show(activity, notice)
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { showFailure(e) }
                    finally { pending.remove(key); refresh() }
                }
            }
            .setOnDismissListener { if (!processing) pending.remove(key) }
            .show()
    }

    /** For screens that already displayed their destructive confirmation. */
    fun removeConfirmed(key: String, delete: suspend () -> String?, refresh: () -> Unit = {}) {
        if (!pending.add(key)) return
        activity.lifecycleScope.launch {
            try {
                val notice = delete()
                if (notice == null) AppBulletin.showSuccess(activity, "Deleted") else AppBulletin.show(activity, notice)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showFailure(e) }
            finally { pending.remove(key); refresh() }
        }
    }

    private fun showFailure(error: Exception) {
        val requestId = (error as? HttpException)?.response()?.headers()?.get("X-Request-ID")
            ?.takeIf { it.matches(Regex("[a-fA-F0-9-]{36}")) }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Operation could not be completed")
            .setMessage(error.toUserFriendlyMessage(false) + (requestId?.let { "\nRequest ID: $it" } ?: ""))
            .setPositiveButton("OK", null)
        if (requestId != null) dialog.setNeutralButton("Copy request ID") { _, _ ->
            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("Eve request ID", requestId))
        }
        dialog.show()
    }
}
