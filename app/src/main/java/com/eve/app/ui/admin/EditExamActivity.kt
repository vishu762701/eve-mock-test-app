package com.eve.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityEditExamBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import com.eve.app.util.Constants
import com.eve.app.util.QuestionImportHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EditExamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExamBinding
    private val examRepo = ExamRepository()

    private var examId: String = ""
    private var examName: String = ""
    private var examCategory: String = ""
    private var timeLimit: Int = 30

    private var currentExam: Exam? = null
    private var currentQuestions: MutableList<Question> = mutableListOf()

    private val questionAdapter = QuestionManageAdapter(
        onEdit = { question -> showQuestionDetails(question) },
        onDelete = { question -> confirmDeleteQuestion(question) }
    )

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleFileSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityEditExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()
        examCategory = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY).orEmpty()
        timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 30)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        setupCategorySpinner()
        setupForm()

        binding.btnSaveExamDetails.setOnClickListener { saveExamDetails() }
        binding.btnAddQuestion.setOnClickListener { showAddQuestionDialog() }
        binding.btnBulkUpload.setOnClickListener {
            filePicker.launch("*/*")
        }

        loadExamData()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Constants.CATEGORIES
        )
        binding.spCategory.adapter = adapter
    }

    private fun setupForm() {
        binding.etExamName.setText(examName)
        binding.etExamMinutes.setText(timeLimit.toString())

        val catIndex = Constants.CATEGORIES.indexOfFirst { it.equals(examCategory, ignoreCase = true) }
        if (catIndex >= 0) {
            binding.spCategory.setSelection(catIndex)
        }
    }

    private fun loadExamData() {
        if (examId.isBlank()) return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val exams = examRepo.getExams()
                currentExam = exams.find { it.id == examId }
                currentExam?.let {
                    examName = it.examName
                    examCategory = it.categoryOrOther
                    timeLimit = it.timeLimitMinutes
                    setupForm()
                }

                loadQuestions()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Failed to load exam: ${e.message}")
            }
        }
    }

    private fun loadQuestions() {
        lifecycleScope.launch {
            try {
                val questions = examRepo.getQuestions(examId)
                currentQuestions = questions.toMutableList()
                binding.progressBar.visibility = View.GONE

                binding.tvQuestionsHeader.text = "Questions (${currentQuestions.size})"
                questionAdapter.submit(currentQuestions)

                binding.tvNoQuestions.visibility =
                    if (currentQuestions.isEmpty()) View.VISIBLE else View.GONE
                binding.rvQuestions.visibility =
                    if (currentQuestions.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Failed to load questions: ${e.message}")
            }
        }
    }

    private fun saveExamDetails() {
        val newName = binding.etExamName.text?.toString()?.trim().orEmpty()
        val newMinutes = binding.etExamMinutes.text?.toString()?.trim()?.toIntOrNull()
        val selectedCategory = binding.spCategory.selectedItem?.toString() ?: Constants.CATEGORY_OTHER

        if (newName.length < 2) {
            AppBulletin.showError(this, "Exam name must be at least 2 characters")
            return
        }
        if (newMinutes == null || newMinutes <= 0) {
            AppBulletin.showError(this, "Please enter a valid time limit in minutes")
            return
        }

        val baseExam = currentExam ?: Exam(id = examId)
        val updatedExam = baseExam.copy(
            id = examId,
            examName = newName,
            timeLimitMinutes = newMinutes,
            category = selectedCategory,
            questionCount = currentQuestions.size
        )

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                examRepo.updateExam(updatedExam)
                currentExam = updatedExam
                examName = newName
                examCategory = selectedCategory
                timeLimit = newMinutes
                binding.progressBar.visibility = View.GONE
                AppBulletin.showSuccess(this@EditExamActivity, "Exam details updated successfully")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Failed to update exam: ${e.message}")
            }
        }
    }

    private fun handleFileSelected(uri: Uri) {
        val fileName = getFileName(uri) ?: "upload.csv"
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val result = QuestionImportHelper.parseQuestions(stream, fileName, examId)
                showImportPreviewDialog(fileName, result)
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to open file: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = it.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun showImportPreviewDialog(
        fileName: String,
        result: QuestionImportHelper.ImportResult
    ) {
        val validCount = result.validQuestions.size
        val errorCount = result.errors.size

        val message = buildString {
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

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle("Bulk Question Import Preview")
            .setMessage(message)

        if (validCount > 0) {
            builder.setPositiveButton("Import ($validCount questions)") { _, _ ->
                importQuestions(result.validQuestions)
            }
            builder.setNegativeButton("Cancel", null)
        } else {
            builder.setPositiveButton("OK", null)
        }

        builder.show()
    }

    private fun importQuestions(questions: List<Question>) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val added = examRepo.addQuestions(questions)
                binding.progressBar.visibility = View.GONE
                AppBulletin.showSuccess(this@EditExamActivity, "Successfully imported $added questions!")
                loadQuestions()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Import failed: ${e.message}")
            }
        }
    }

    private fun showAddQuestionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_question, null)
        val etQuestion = dialogView.findViewById<EditText>(R.id.etQuestionText)
        val etOptA = dialogView.findViewById<EditText>(R.id.etOptionA)
        val etOptB = dialogView.findViewById<EditText>(R.id.etOptionB)
        val etOptC = dialogView.findViewById<EditText>(R.id.etOptionC)
        val etOptD = dialogView.findViewById<EditText>(R.id.etOptionD)
        val rgCorrect = dialogView.findViewById<RadioGroup>(R.id.rgCorrectAnswer)
        val etExplanation = dialogView.findViewById<EditText>(R.id.etExplanation)
        val etTopic = dialogView.findViewById<EditText>(R.id.etTopic)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Question")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val qText = etQuestion.text?.toString()?.trim().orEmpty()
                val a = etOptA.text?.toString()?.trim().orEmpty()
                val b = etOptB.text?.toString()?.trim().orEmpty()
                val c = etOptC.text?.toString()?.trim().orEmpty()
                val d = etOptD.text?.toString()?.trim().orEmpty()
                val exp = etExplanation.text?.toString()?.trim().orEmpty()
                val topic = etTopic.text?.toString()?.trim().orEmpty()

                val correct = when (rgCorrect.checkedRadioButtonId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> "A"
                }

                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    AppBulletin.showError(this, "Question and all 4 options are required")
                    return@setPositiveButton
                }

                val newQuestion = Question(
                    id = "",
                    examId = examId,
                    questionText = qText,
                    optionA = a,
                    optionB = b,
                    optionC = c,
                    optionD = d,
                    correctAnswer = correct,
                    explanation = exp,
                    topic = topic
                )

                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        examRepo.addQuestion(newQuestion)
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showSuccess(this@EditExamActivity, "Question added")
                        loadQuestions()
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@EditExamActivity, "Failed to add question: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        MaterialAlertDialogBuilder(this)
            .setTitle("Question Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDeleteQuestion(q: Question) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Question?")
            .setMessage("Are you sure? This cannot be undone.\n\n\"${q.questionText}\"")
            .setPositiveButton("Delete") { _, _ ->
                val prevList = currentQuestions.toMutableList()
                val updated = prevList.filter { it.id != q.id }
                currentQuestions = updated.toMutableList()
                questionAdapter.submit(currentQuestions)
                binding.tvQuestionsHeader.text = "Questions (${currentQuestions.size})"

                AppUndoBar.show(
                    context = this@EditExamActivity,
                    message = "Question deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        currentQuestions = prevList
                        questionAdapter.submit(currentQuestions)
                        binding.tvQuestionsHeader.text = "Questions (${currentQuestions.size})"
                        AppBulletin.show(this@EditExamActivity, "Delete cancelled")
                    },
                    onExecuteDelete = {
                        lifecycleScope.launch {
                            try {
                                examRepo.deleteQuestion(q.id)
                                AppBulletin.showSuccess(this@EditExamActivity, "Question deleted")
                            } catch (e: Exception) {
                                AppBulletin.showError(this@EditExamActivity, "Failed to delete question: ${e.message}")
                                loadQuestions()
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
