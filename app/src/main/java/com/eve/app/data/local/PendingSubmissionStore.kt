package com.eve.app.data.local

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class PendingAnswer(
    val questionId: String,
    val number: Int,
    val selected: String,
    val isBookmarked: Boolean,
    val timeTakenSeconds: Long
)

data class PendingSubmission(
    val clientAttemptId: String,
    val examId: String,
    val examName: String,
    val category: String,
    val answers: List<PendingAnswer>,
    val topic: String? = null,
    val pyqYear: Int? = null,
    val pyqPaper: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

class PendingSubmissionStore(private val context: Context) {

    private val gson = Gson()

    private val dir: File
        get() = File(context.filesDir, "pending_submissions").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    fun save(submission: PendingSubmission) {
        try {
            val file = File(dir, "${submission.clientAttemptId}.json")
            file.writeText(gson.toJson(submission))
        } catch (_: Exception) {}
    }

    fun remove(clientAttemptId: String) {
        try {
            val file = File(dir, "$clientAttemptId.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    fun list(): List<PendingSubmission> {
        return try {
            val files = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
            files.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), PendingSubmission::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(clientAttemptId: String): PendingSubmission? {
        val file = File(dir, "$clientAttemptId.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), PendingSubmission::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
