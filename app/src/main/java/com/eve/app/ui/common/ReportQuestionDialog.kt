package com.eve.app.ui.common

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.FlaggedQuestion
import com.eve.app.data.model.Question
import com.eve.app.data.repository.FlaggedQuestionRepository
import com.eve.app.util.AppBulletin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReportQuestionDialog {

    private val flaggedRepo by lazy { FlaggedQuestionRepository() }

    fun show(
        activity: Activity,
        question: Question,
        examId: String,
        examName: String = ""
    ) {
        show(
            activity = activity,
            questionId = question.id,
            questionText = question.questionText,
            examId = examId,
            examName = examName
        )
    }

    fun show(
        activity: Activity,
        item: AnswerItem,
        examId: String,
        examName: String = ""
    ) {
        show(
            activity = activity,
            questionId = item.questionId,
            questionText = item.questionText,
            examId = examId,
            examName = examName
        )
    }

    fun show(
        activity: Activity,
        questionId: String,
        questionText: String,
        examId: String,
        examName: String = "",
        onSubmitted: ((Boolean) -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_report_question, null)
        val rgReason = dialogView.findViewById<RadioGroup>(R.id.rgReportReason)
        val etComment = dialogView.findViewById<EditText>(R.id.etReportComment)
        val tvError = dialogView.findViewById<TextView>(R.id.tvCommentError)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Report Question")
            .setView(dialogView)
            .setPositiveButton("Submit Report", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        val btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnSubmit.setOnClickListener {
            val selectedReason = when (rgReason.checkedRadioButtonId) {
                R.id.rbReasonWrongQuestion -> "Wrong Question"
                R.id.rbReasonNoSolution -> "No Solution"
                R.id.rbReasonWrongTranslation -> "Wrong Translation"
                R.id.rbReasonOutOfSyllabus -> "Out of Syllabus"
                R.id.rbReasonNotVisible -> "Question and Options not visible"
                R.id.rbReasonBlinking -> "Blinking Screen Issue"
                R.id.rbReasonFormatting -> "Formatting Issues"
                R.id.rbReasonScroll -> "Scroll Not Working"
                R.id.rbReasonDarkMode -> "Dark Mode Issue"
                R.id.rbReasonQuestionMissingOptionsVisible -> "Question not visible but Options visible"
                else -> "Other"
            }

            val comment = etComment.text?.toString()?.trim().orEmpty()

            tvError.visibility = View.GONE
            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            val scope = (activity as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main)
            scope.launch {
                val result = flaggedRepo.flagQuestionResult(
                    questionId = questionId,
                    examId = examId,
                    examName = examName,
                    questionText = questionText,
                    reason = selectedReason,
                    comment = comment
                )
                if (result.isSuccess) {
                    val isContent = FlaggedQuestion.isContentIssue(selectedReason)
                    val msg = if (isContent) {
                        "Thank you! Content issue reported for review."
                    } else {
                        "Thank you! Technical bug report submitted to engineering."
                    }
                    AppBulletin.showSuccess(activity, msg)
                    onSubmitted?.invoke(true)
                    dialog.dismiss()
                } else {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Report"
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Could not submit report. Please try again."
                    tvError.visibility = View.VISIBLE
                    tvError.text = err
                    AppBulletin.showError(activity, err)
                    onSubmitted?.invoke(false)
                }
            }
        }
    }
}
