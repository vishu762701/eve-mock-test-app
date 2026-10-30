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
import com.eve.app.databinding.ActivityManageQuestionsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.ExamImageHelper
import com.eve.app.util.QuestionImportHelper
import kotlinx.coroutines.launch

class ManageQuestionsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageQuestionsBinding
    private val viewModel: AdminViewModel by viewModels()

    private var exams: List<Exam> = emptyList()
    private var selectedExamForImageUpdate: Exam? = null

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
        onDelete = { q -> confirmDeleteQuestion(q) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.telegramActionBar.setTitle("Manage Questions")
        binding.telegramActionBar.onBackPressed = { finish() }

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
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
                        val examNames = examList.map { "${it.examName} (${it.categoryOrOther})" }
                        val adapter = ArrayAdapter(this@ManageQuestionsActivity, android.R.layout.simple_spinner_item, examNames)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spExam.adapter = adapter

                        if (examList.isNotEmpty()) {
                            val pos = binding.spExam.selectedItemPosition.coerceAtLeast(0).coerceAtMost(examList.size - 1)
                            viewModel.loadQuestionsForExam(examList[pos].id)
                        } else {
                            updateQuestionsUi(emptyList())
                        }
                    }
                }
                launch {
                    viewModel.questions.collect { questions ->
                        updateQuestionsUi(questions)
                    }
                }
            }
        }

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in exams.indices) {
                    viewModel.loadQuestionsForExam(exams[position].id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val etQuestion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuestionText)
        val etOptA = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionA)
        val etOptB = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionB)
        val etOptC = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionC)
        val etOptD = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionD)
        val rgCorrect = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgCorrectAnswer)
        val etExplanation = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExplanation)
        val etTopic = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTopic)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Add Question to ${selectedExam.examName}")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val qText = etQuestion.text?.toString()?.trim().orEmpty()
                val a = etOptA.text?.toString()?.trim().orEmpty()
                val b = etOptB.text?.toString()?.trim().orEmpty()
                val c = etOptC.text?.toString()?.trim().orEmpty()
                val d = etOptD.text?.toString()?.trim().orEmpty()
                val exp = etExplanation.text?.toString()?.trim().orEmpty()
                val topic = etTopic.text?.toString()?.trim().orEmpty()

                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    AppBulletin.showError(this, "Question text and all 4 options are required")
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
                    AppBulletin.showError(this, "Please select the correct option")
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

                viewModel.addQuestion(newQuestion) {
                    AppBulletin.showSuccess(this@ManageQuestionsActivity, "Question added successfully!")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        val input = android.widget.EditText(this).apply {
            setText(exam.examName)
            setSelection(text.length)
            hint = "Enter new exam name"
            val padding = resources.getDimensionPixelSize(R.dimen.design_input_padding)
            setPadding(padding, 0, padding, 0)
        }

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Rename Exam")
            .setMessage("Rename '${exam.examName}' (${exam.categoryOrOther})")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.length < 2) {
                    AppBulletin.showError(this, "Exam name must be at least 2 characters")
                    return@setPositiveButton
                }
                if (exams.any { it.id != exam.id && it.categoryOrOther.equals(exam.categoryOrOther, ignoreCase = true) && it.examName.trim().equals(newName, ignoreCase = true) }) {
                    AppBulletin.showError(this, "An exam named '$newName' already exists in category '${exam.categoryOrOther}'")
                    return@setPositiveButton
                }
                viewModel.renameExam(exam.id, newName) {
                    AppBulletin.showSuccess(this@ManageQuestionsActivity, "Renamed exam to '$newName'")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        AppBulletin.showProgress(this, "Parsing $fileName...")

        lifecycleScope.launch {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parsedQuestions = QuestionImportHelper.parseQuestions(
                        context = this@ManageQuestionsActivity,
                        fileName = fileName,
                        inputStream = inputStream,
                        targetExamId = selectedExam.id
                    )

                    if (parsedQuestions.isEmpty()) {
                        AppBulletin.showError(this@ManageQuestionsActivity, "No valid questions found in file")
                        return@launch
                    }

                    AppBulletin.showProgress(this@ManageQuestionsActivity, "Uploading ${parsedQuestions.size} questions...")
                    viewModel.bulkAddQuestions(parsedQuestions) { count ->
                        AppBulletin.showSuccess(this@ManageQuestionsActivity, "Successfully imported $count questions!")
                    }
                }
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
}
