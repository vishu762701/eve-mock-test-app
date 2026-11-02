package com.eve.app.ui.admin

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageQuestionsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.ExamImageHelper
import com.eve.app.util.QuestionImportHelper
import kotlinx.coroutines.launch
import androidx.core.view.doOnLayout

class ManageQuestionsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageQuestionsBinding
    private val viewModel: AdminViewModel by viewModels()
    private val examRepo = ExamRepository()

    private var exams: List<Exam> = emptyList()
    private var selectedExamForImageUpdate: Exam? = null
    private var addQuestionDialog: TelegramNativeAlertDialog? = null
    private var renameExamDialog: TelegramNativeAlertDialog? = null

    private val pickEditExamImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val exam = selectedExamForImageUpdate ?: return@registerForActivityResult
        if (uri != null) {
            val base64 = ExamImageHelper.uriToBase64(this, uri)
            if (base64 != null) {
                viewModel.updateExamImage(exam.id, base64) {
                    selectedExamForImageUpdate = null
                }
            } else {
                AppBulletin.showError(this, "Failed to load image")
            }
        }
    }

    private val pickBulkQuestionsLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleBulkUploadForSelectedExam(uri)
        }
    }

    private val questionAdapter = QuestionManageAdapter(
        onEdit = { q -> showQuestionDetails(q) },
        onDelete = { q -> confirmDeleteQuestion(q) },
        canManage = { !isFinishing && !isDestroyed }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.telegramActionBar.setTitle("Manage Questions")
        binding.telegramActionBar.onBackPressed = { finish() }

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_questions") { permissions ->
            val canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
            for (control in listOf(binding.btnEditExamImage, binding.btnRenameExam, binding.btnDeleteExam)) {
                control.visibility = if (canManageExams) View.VISIBLE else View.GONE
            }
            setupUi()
            setupObservers()
            viewModel.loadExams()
        }
    }

    private fun setupUi() {
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        binding.btnEditExamImage.setOnClickListener { showEditExamImageDialog() }
        binding.btnRenameExam.setOnClickListener { promptRenameExam() }
        binding.btnDeleteExam.setOnClickListener { confirmDeleteExam() }

        binding.btnAddQuestionManual.setOnClickListener {
            val selectedExam = exams.getOrNull(binding.spExam.selectedItemPosition)
            if (selectedExam == null) {
                AppBulletin.showError(this, "Please select an exam first")
                return@setOnClickListener
            }
            showAddQuestionDialog(selectedExam)
        }

        binding.btnBulkUploadSelectedExam.setOnClickListener {
            val selectedExam = exams.getOrNull(binding.spExam.selectedItemPosition)
            if (selectedExam == null) {
                AppBulletin.showError(this, "Please select an exam first")
                return@setOnClickListener
            }
            pickBulkQuestionsLauncher.launch("*/*")
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exams.collect { examList ->
                        exams = examList
                        bindQuestionExamChoices(binding.spExam, examList, viewModel)
                        if (examList.isEmpty()) updateQuestionsUi(emptyList())
                    }
                }
                launch {
                    viewModel.questions.collect { questions ->
                        updateQuestionsUi(questions)
                    }
                }
            }
        }
    }

    private fun updateQuestionsUi(questions: List<Question>) {
        if (exams.isEmpty()) {
            binding.layoutEmptyQuestions.visibility = View.VISIBLE
            binding.tvEmptyQuestionsTitle.text = "No exams available"
            binding.tvEmptyQuestionsSubtitle.text = "Create an exam first before managing questions."
            binding.rvQuestions.visibility = View.GONE
            binding.tvNoQuestions.visibility = View.GONE
        } else if (questions.isEmpty()) {
            binding.layoutEmptyQuestions.visibility = View.GONE
            binding.tvNoQuestions.visibility = View.VISIBLE
            binding.rvQuestions.visibility = View.GONE
            questionAdapter.submit(emptyList())
        } else {
            binding.layoutEmptyQuestions.visibility = View.GONE
            binding.tvNoQuestions.visibility = View.GONE
            binding.rvQuestions.visibility = View.VISIBLE
            questionAdapter.submit(questions)
        }
    }

    private fun showAddQuestionDialog(selectedExam: Exam) {
        if (isFinishing || isDestroyed || addQuestionDialog?.isShowing == true) return
        val dialog = createAddQuestionDialog(this, selectedExam, isBusy = { viewModel.busy.value }, submit = viewModel::addQuestion)
        addQuestionDialog = dialog
        dialog.setOnDismissListener { if (addQuestionDialog === dialog) addQuestionDialog = null }
        dialog.show()
    }

    private fun showEditExamImageDialog() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (exam == null) {
            AppBulletin.showError(this, "Please select an exam first")
            return
        }
        selectedExamForImageUpdate = exam
        val hasImage = exam.imageUrl.isNotBlank()
        val options = if (hasImage) {
            arrayOf("Change Image", "Remove Image")
        } else {
            arrayOf("Choose Image")
        }
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Exam Image: ${exam.examName}")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Choose Image", "Change Image" -> {
                        pickEditExamImageLauncher.launch("image/*")
                    }
                    "Remove Image" -> {
                        viewModel.updateExamImage(exam.id, "") {
                            selectedExamForImageUpdate = null
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRenameExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (exam == null) {
            AppBulletin.showError(this, "Please select an exam to rename")
            return
        }

        if (isFinishing || isDestroyed || renameExamDialog?.isShowing == true) return
        val dialog = createRenameExamDialog(this, exam, { exams }, viewModel)
        renameExamDialog = dialog
        dialog.setOnDismissListener { if (renameExamDialog === dialog) renameExamDialog = null }
        dialog.show()
    }

    private fun confirmDeleteExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition) ?: return
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Delete ${exam.examName}?")
            .setMessage("Delete ${exam.examName}? This will permanently remove the exam and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExam(exam.id) {
                    AppBulletin.showSuccess(this@ManageQuestionsActivity, "Exam '${exam.examName}' deleted")
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
    }

    private fun showQuestionDetails(q: Question) {
        val details = buildString {
            append("Q: ${q.questionText}\n\n")
            append("A: ${q.optionA}\n")
            append("B: ${q.optionB}\n")
            append("C: ${q.optionC}\n")
            append("D: ${q.optionD}\n\n")
            append("Correct Answer: ${q.correctAnswer}\n")
            if (q.explanation.isNotBlank()) append("Explanation: ${q.explanation}\n")
            if (q.topic.isNotBlank()) append("Topic: ${q.topic}\n")
        }
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Question Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDeleteQuestion(q: Question) {
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Delete Question?")
            .setMessage("Delete this question? This will permanently remove the question and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuestion(q)
                AppBulletin.showSuccess(this@ManageQuestionsActivity, "Question deleted")
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
    }

    private fun handleBulkUploadForSelectedExam(uri: Uri) {
        val selectedExam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (selectedExam == null) {
            AppBulletin.showError(this, "No exam selected to upload questions")
            return
        }
        val fileName = getFileName(uri) ?: "questions.csv"
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val result = QuestionImportHelper.parseQuestions(stream, fileName, selectedExam.id)
                showBulkImportPreviewDialog(fileName, selectedExam, result)
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to read file: ${e.message}")
        }
    }

    private fun showBulkImportPreviewDialog(
        fileName: String,
        selectedExam: Exam,
        result: QuestionImportHelper.ImportResult
    ) {
        val validCount = result.validQuestions.size
        val errorCount = result.errors.size

        val message = buildString {
            append("Exam: ${selectedExam.examName}\n")
            append("File: $fileName\n\n")
            append("✅ $validCount questions ready to import.\n")
            if (errorCount > 0) {
                append("⚠️ $errorCount rows skipped due to errors:\n\n")
                result.errors.take(8).forEach { err ->
                    append("• Row ${err.rowNumber}: ${err.reason}\n")
                }
                if (errorCount > 8) {
                    append("...and ${errorCount - 8} more errors.\n")
                }
            }
            if (validCount == 0) {
                append("\nNo valid questions found to import. Please check file format.")
            }
        }

        val builder = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Bulk Question Import Preview")
            .setMessage(message)

        if (validCount > 0) {
            builder.setPositiveButton("Import ($validCount questions)") { _, _ ->
                importQuestionsForSelectedExam(selectedExam.id, result.validQuestions)
            }
            builder.setNegativeButton("Cancel", null)
        } else {
            builder.setPositiveButton("OK", null)
        }

        builder.show()
    }

    private fun importQuestionsForSelectedExam(examId: String, questions: List<Question>) {
        lifecycleScope.launch {
            try {
                val added = examRepo.addQuestions(questions)
                AppBulletin.showSuccess(this@ManageQuestionsActivity, "Successfully imported $added questions!")
                viewModel.refreshQuestions(examId)
            } catch (e: Exception) {
                AppBulletin.showError(this@ManageQuestionsActivity, "Import failed: ${e.message}")
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
    override fun onDestroy() {
        renameExamDialog?.dismiss()
        renameExamDialog = null
        addQuestionDialog?.dismiss()
        addQuestionDialog = null
        super.onDestroy()
    }

}

/** The single production spinner binding; identity survives adapter rebuilds and reordering. */
internal fun bindQuestionExamChoices(spinner: android.widget.Spinner, exams: List<Exam>,
                                    viewModel: AdminViewModel) {
    val selectedId = viewModel.questionExamId
    spinner.onItemSelectedListener = null
    spinner.adapter = ArrayAdapter(spinner.context, android.R.layout.simple_spinner_item,
        exams.map { "${it.examName} (${it.categoryOrOther})" }).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    if (exams.isEmpty()) {
        viewModel.loadQuestions("")
    } else {
        val position = exams.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        spinner.setSelection(position)
        viewModel.loadQuestions(exams[position].id)
    }
    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val exam = exams.getOrNull(position) ?: return
            if (exam.id != viewModel.questionExamId) viewModel.loadQuestions(exam.id)
        }
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}

/** Canonical question form presentation; authorization stays at the Activity/Worker entry points. */
internal fun createAddQuestionDialog(activity: android.app.Activity, selectedExam: Exam,
                                     isBusy: () -> Boolean = { false },
                                     title: CharSequence = "Add Question to ${selectedExam.examName}",
                                     submit: (Question, (String) -> Unit, () -> Unit) -> Unit): TelegramNativeAlertDialog {
        var submitting = false
        lateinit var dialog: TelegramNativeAlertDialog
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_add_question, null)
        val errorLabel = dialogView.findViewById<android.widget.TextView>(R.id.tvQuestionFormError)
        fun showFormError(message: String) {
            errorLabel.text = message
            errorLabel.visibility = View.VISIBLE
            errorLabel.doOnLayout { label ->
                if (dialog.isShowing) label.requestRectangleOnScreen(
                    android.graphics.Rect(0, 0, label.width, label.height), true)
            }
        }
        val etQuestion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuestionText)
        val etOptA = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionA)
        val etOptB = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionB)
        val etOptC = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionC)
        val etOptD = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionD)
        val rgCorrect = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgCorrectAnswer)
        val etExplanation = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExplanation)
        val etTopic = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTopic)

        dialog = TelegramNativeAlertDialog.Builder(activity)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                if (submitting || isBusy()) return@setPositiveButton
                val qText = etQuestion.text?.toString()?.trim().orEmpty()
                val a = etOptA.text?.toString()?.trim().orEmpty()
                val b = etOptB.text?.toString()?.trim().orEmpty()
                val c = etOptC.text?.toString()?.trim().orEmpty()
                val d = etOptD.text?.toString()?.trim().orEmpty()
                val exp = etExplanation.text?.toString()?.trim().orEmpty()
                val topic = etTopic.text?.toString()?.trim().orEmpty()

                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    showFormError("Question text and all 4 options are required")
                    return@setPositiveButton
                }

                val correct = when (rgCorrect.checkedRadioButtonId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }
                if (correct.isBlank()) {
                    showFormError("Please select the correct option")
                    return@setPositiveButton
                }

                val newQuestion = Question(
                    id = "",
                    examId = selectedExam.id,
                    questionText = qText,
                    optionA = a,
                    optionB = b,
                    optionC = c,
                    optionD = d,
                    correctAnswer = correct,
                    explanation = exp,
                    topic = topic
                )

                submitting = true
                errorLabel.visibility = View.GONE
                val addButton = dialog.getActionButton(android.content.DialogInterface.BUTTON_POSITIVE)
                val addLabel = addButton?.text
                addButton?.isEnabled = false
                addButton?.setText(R.string.question_form_submitting)
                dialog.getActionButton(android.content.DialogInterface.BUTTON_NEGATIVE)?.isEnabled = false
                dialog.setCancelable(false)
                submit(newQuestion, { message ->
                    submitting = false
                    if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                        addButton?.text = addLabel
                        addButton?.isEnabled = true
                        dialog.getActionButton(android.content.DialogInterface.BUTTON_NEGATIVE)?.isEnabled = true
                        dialog.setCancelable(true)
                        showFormError(message)
                    }
                }) {
                    if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                        dialog.dismiss()
                        AppBulletin.showSuccess(activity, "Question added successfully!")
                    }
                }
            }
            .setNegativeButton("Cancel") { owner, _ -> if (!submitting) owner.dismiss() }
            .create()
        dialog.setDismissDialogByButtons(false)
        return dialog
}

/** Existing active rename form; server permissions remain independent of local validation. */
internal fun createRenameExamDialog(activity: android.app.Activity, exam: Exam,
                                   exams: () -> List<Exam>, viewModel: AdminViewModel): TelegramNativeAlertDialog {
    var submitting = false
    lateinit var dialog: TelegramNativeAlertDialog
    val input = android.widget.EditText(activity).apply {
        setText(exam.examName)
        setSelection(text.length)
        hint = activity.getString(R.string.exam_rename_hint)
        val padding = resources.getDimensionPixelSize(R.dimen.design_input_padding)
        setPadding(padding, 0, padding, 0)
    }
    fun showError(message: String) {
        input.error = message
        input.requestFocus()
    }
    dialog = TelegramNativeAlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.exam_rename_title))
        .setMessage(activity.getString(R.string.exam_rename_message, exam.examName, exam.categoryOrOther))
        .setView(input)
        .setPositiveButton(activity.getString(R.string.exam_rename_action)) { _, _ ->
            if (submitting || viewModel.busy.value) return@setPositiveButton
            val newName = input.text.toString().trim()
            if (newName.length < 2) {
                showError(activity.getString(R.string.exam_rename_too_short))
                return@setPositiveButton
            }
            if (exams().any { it.id != exam.id && it.categoryOrOther.equals(exam.categoryOrOther, ignoreCase = true) && it.examName.trim().equals(newName, ignoreCase = true) }) {
                showError(activity.getString(R.string.exam_rename_duplicate, newName, exam.categoryOrOther))
                return@setPositiveButton
            }
            submitting = true
            input.error = null
            input.isEnabled = false
            val button = dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)
            button?.isEnabled = false
            button?.setText(R.string.exam_renaming)
            dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = false
            dialog.setCancelable(false)
            viewModel.renameExam(exam.id, newName, onFailure = { message ->
                submitting = false
                if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    input.isEnabled = true
                    button?.setText(R.string.exam_rename_action)
                    button?.isEnabled = true
                    dialog.getActionButton(DialogInterface.BUTTON_NEGATIVE)?.isEnabled = true
                    dialog.setCancelable(true)
                    showError(message)
                }
            }) {
                if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    dialog.dismiss()
                    AppBulletin.showSuccess(activity, activity.getString(R.string.exam_rename_success, newName))
                }
            }
        }
        .setNegativeButton(activity.getString(R.string.cancel)) { owner, _ -> if (!submitting) owner.dismiss() }
        .create()
    dialog.setDismissDialogByButtons(false)
    return dialog
}
