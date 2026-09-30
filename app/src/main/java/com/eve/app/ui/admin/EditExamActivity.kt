package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

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
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityEditExamBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import com.eve.app.util.Constants
import com.eve.app.util.QuestionImportHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.data.repository.ApiUsageRepository
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class EditExamActivity : EveBaseActivity() {

    private lateinit var binding: ActivityEditExamBinding
    private val examRepo = ExamRepository()
    private val auditLogRepo = AuditLogRepository()
    private val apiUsageRepo = ApiUsageRepository()

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

                    // Populate category cutoffs
                    binding.etCutoffGeneral.setText(it.cutoffs["General"]?.toString().orEmpty())
                    binding.etCutoffObc.setText(it.cutoffs["OBC"]?.toString().orEmpty())
                    binding.etCutoffSc.setText(it.cutoffs["SC"]?.toString().orEmpty())
                    binding.etCutoffSt.setText(it.cutoffs["ST"]?.toString().orEmpty())
                    binding.etCutoffEws.setText(it.cutoffs["EWS"]?.toString().orEmpty())
                }

                // If cutoffs weren't in API, fallback query from Firestore
                try {
                    val snap = FirebaseFirestore.getInstance().collection("exams").document(examId).get().await()
                    val cMap = snap.get("cutoffs") as? Map<String, Any>
                    if (cMap != null && currentExam?.cutoffs?.isEmpty() != false) {
                        val loadedCutoffs = cMap.mapNotNull { (k, v) ->
                            (v as? Number)?.toDouble()?.let { k to it }
                        }.toMap()
                        currentExam = currentExam?.copy(cutoffs = loadedCutoffs)
                        binding.etCutoffGeneral.setText(loadedCutoffs["General"]?.toString().orEmpty())
                        binding.etCutoffObc.setText(loadedCutoffs["OBC"]?.toString().orEmpty())
                        binding.etCutoffSc.setText(loadedCutoffs["SC"]?.toString().orEmpty())
                        binding.etCutoffSt.setText(loadedCutoffs["ST"]?.toString().orEmpty())
                        binding.etCutoffEws.setText(loadedCutoffs["EWS"]?.toString().orEmpty())
                    }
                } catch (_: Exception) {}

                loadQuestions()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Failed to load exam: ${e.toUserFriendlyMessage()}")
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
                AppBulletin.showError(this@EditExamActivity, "Failed to load questions: ${e.toUserFriendlyMessage()}")
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

        val cutoffsMap = mutableMapOf<String, Double>()
        binding.etCutoffGeneral.text?.toString()?.trim()?.toDoubleOrNull()?.let { cutoffsMap["General"] = it }
        binding.etCutoffObc.text?.toString()?.trim()?.toDoubleOrNull()?.let { cutoffsMap["OBC"] = it }
        binding.etCutoffSc.text?.toString()?.trim()?.toDoubleOrNull()?.let { cutoffsMap["SC"] = it }
        binding.etCutoffSt.text?.toString()?.trim()?.toDoubleOrNull()?.let { cutoffsMap["ST"] = it }
        binding.etCutoffEws.text?.toString()?.trim()?.toDoubleOrNull()?.let { cutoffsMap["EWS"] = it }

        val baseExam = currentExam ?: Exam(id = examId)
        val updatedExam = baseExam.copy(
            id = examId,
            examName = newName,
            timeLimitMinutes = newMinutes,
            category = selectedCategory,
            questionCount = currentQuestions.size,
            cutoffs = cutoffsMap
        )

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                examRepo.updateExam(updatedExam)
                try {
                    FirebaseFirestore.getInstance().collection("exams").document(examId)
                        .set(mapOf("cutoffs" to cutoffsMap), SetOptions.merge())
                        .await()
                } catch (_: Exception) {}

                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_EXAM_EDITED,
                    "Updated exam details for '$newName'"
                )
                apiUsageRepo.incrementDocumentWrites(1)
                currentExam = updatedExam
                examName = newName
                examCategory = selectedCategory
                timeLimit = newMinutes
                binding.progressBar.visibility = View.GONE
                AppBulletin.showSuccess(this@EditExamActivity, "Exam details updated successfully")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@EditExamActivity, "Failed to update exam: ${e.toUserFriendlyMessage()}")
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
                AppBulletin.showError(this@EditExamActivity, "Import failed: ${e.toUserFriendlyMessage()}")
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
                        AppBulletin.showError(this@EditExamActivity, "Failed to add question: ${e.toUserFriendlyMessage()}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQuestionDetails(q: Question) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_question_details, null)
        val etQuestionText = dialogView.findViewById<EditText>(R.id.etQuestionText)
        val etOptionA = dialogView.findViewById<EditText>(R.id.etOptionA)
        val etOptionB = dialogView.findViewById<EditText>(R.id.etOptionB)
        val etOptionC = dialogView.findViewById<EditText>(R.id.etOptionC)
        val etOptionD = dialogView.findViewById<EditText>(R.id.etOptionD)
        val rgCorrectAnswer = dialogView.findViewById<RadioGroup>(R.id.rgCorrectAnswer)
        val btnReformatAi = dialogView.findViewById<MaterialButton>(R.id.btnReformatAi)
        val pbReformat = dialogView.findViewById<ProgressBar>(R.id.pbReformat)
        val etExplanation = dialogView.findViewById<EditText>(R.id.etExplanation)
        val etTopic = dialogView.findViewById<EditText>(R.id.etTopic)

        etQuestionText.setText(q.questionText)
        etOptionA.setText(q.optionA)
        etOptionB.setText(q.optionB)
        etOptionC.setText(q.optionC)
        etOptionD.setText(q.optionD)
        etExplanation.setText(q.explanation)
        etTopic.setText(q.topic)

        when (q.correctAnswer.uppercase().trim()) {
            "A", "1" -> rgCorrectAnswer.check(R.id.rbA)
            "B", "2" -> rgCorrectAnswer.check(R.id.rbB)
            "C", "3" -> rgCorrectAnswer.check(R.id.rbC)
            "D", "4" -> rgCorrectAnswer.check(R.id.rbD)
            else -> rgCorrectAnswer.check(R.id.rbA)
        }

        btnReformatAi.setOnClickListener {
            val currentExp = etExplanation.text?.toString()?.trim().orEmpty()
            val correctOpt = when (rgCorrectAnswer.checkedRadioButtonId) {
                R.id.rbA -> "A"
                R.id.rbB -> "B"
                R.id.rbC -> "C"
                R.id.rbD -> "D"
                else -> "A"
            }
            val qText = etQuestionText.text?.toString()?.trim().orEmpty()
            val optA = etOptionA.text?.toString()?.trim().orEmpty()
            val optB = etOptionB.text?.toString()?.trim().orEmpty()
            val optC = etOptionC.text?.toString()?.trim().orEmpty()
            val optD = etOptionD.text?.toString()?.trim().orEmpty()

            pbReformat.visibility = View.VISIBLE
            btnReformatAi.isEnabled = false

            lifecycleScope.launch {
                try {
                    val functions = FirebaseFunctions.getInstance()
                    val payload = hashMapOf(
                        "questionText" to qText,
                        "optionA" to optA,
                        "optionB" to optB,
                        "optionC" to optC,
                        "optionD" to optD,
                        "correctAnswer" to correctOpt,
                        "explanation" to currentExp
                    )
                    val result = functions.getHttpsCallable("reformatQuestionExplanation")
                        .call(payload)
                        .await()
                    val resMap = result.data as? Map<*, *>
                    val reformatted = resMap?.get("reformattedExplanation") as? String
                    if (!reformatted.isNullOrBlank()) {
                        etExplanation.setText(reformatted)
                        AppBulletin.showSuccess(this@EditExamActivity, "Restructured to Key Points format!")
                    } else {
                        val fallback = formatKeyPointsLocally(correctOpt, currentExp)
                        etExplanation.setText(fallback)
                        AppBulletin.show(this@EditExamActivity, "Restructured to Key Points format")
                    }
                } catch (e: Exception) {
                    val fallback = formatKeyPointsLocally(correctOpt, currentExp)
                    etExplanation.setText(fallback)
                    AppBulletin.show(this@EditExamActivity, "Restructured to Key Points format")
                } finally {
                    pbReformat.visibility = View.GONE
                    btnReformatAi.isEnabled = true
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Question")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newQText = etQuestionText.text?.toString()?.trim().orEmpty()
                val newOptA = etOptionA.text?.toString()?.trim().orEmpty()
                val newOptB = etOptionB.text?.toString()?.trim().orEmpty()
                val newOptC = etOptionC.text?.toString()?.trim().orEmpty()
                val newOptD = etOptionD.text?.toString()?.trim().orEmpty()
                val newExp = etExplanation.text?.toString()?.trim().orEmpty()
                val newTopic = etTopic.text?.toString()?.trim().orEmpty()
                val newCorrect = when (rgCorrectAnswer.checkedRadioButtonId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> q.correctAnswer
                }

                if (newQText.isBlank() || newOptA.isBlank() || newOptB.isBlank() || newOptC.isBlank() || newOptD.isBlank()) {
                    AppBulletin.showError(this, "Question and all options are required")
                    return@setPositiveButton
                }

                val updatedQ = q.copy(
                    questionText = newQText,
                    optionA = newOptA,
                    optionB = newOptB,
                    optionC = newOptC,
                    optionD = newOptD,
                    correctAnswer = newCorrect,
                    explanation = newExp,
                    topic = newTopic
                )

                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        examRepo.updateQuestion(updatedQ)
                        val idx = currentQuestions.indexOfFirst { it.id == q.id }
                        if (idx >= 0) {
                            currentQuestions[idx] = updatedQ
                            questionAdapter.submit(currentQuestions.toList())
                        }
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showSuccess(this@EditExamActivity, "Question updated successfully")
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@EditExamActivity, "Failed to update question: ${e.toUserFriendlyMessage()}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        fun formatKeyPointsLocally(correctAnswer: String, rawExplanation: String): String {
            val optNum = when (correctAnswer.uppercase().trim()) {
                "A" -> "1"
                "B" -> "2"
                "C" -> "3"
                "D" -> "4"
                else -> correctAnswer
            }
            val summary = "**The correct answer is Option $optNum ($correctAnswer).**"
            val cleanedExp = rawExplanation.trim()
            val bullets = if (cleanedExp.isBlank()) {
                "• **Core Concept:** Option $correctAnswer is the correct answer.\n• **Explanation:** Verified against the standard exam syllabus."
            } else {
                val lines = cleanedExp.lines()
                    .map { it.trim().trimStart('•', '-', '*').trim() }
                    .filter { it.isNotBlank() && !it.startsWith("Key Points", ignoreCase = true) && !it.contains("The correct answer is", ignoreCase = true) }
                if (lines.size > 1) {
                    lines.joinToString("\n") { line ->
                        if (line.startsWith("**") && line.contains(":**")) {
                            "• $line"
                        } else {
                            "• **Key Concept:** $line"
                        }
                    }
                } else if (lines.size == 1) {
                    "• **Key Concept:** ${lines[0]}\n• **Explanation:** Option $optNum ($correctAnswer) aligns with official solution criteria."
                } else {
                    "• **Core Concept:** Option $correctAnswer is correct."
                }
            }
            return "$summary\n\nKey Points:\n$bullets"
        }
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

                lifecycleScope.launch {
                    try {
                        examRepo.deleteQuestion(q.id)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@EditExamActivity, "Failed to delete question: ${e.toUserFriendlyMessage()}")
                    }
                }

                AppUndoBar.show(
                    context = this@EditExamActivity,
                    message = "Question deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        currentQuestions = prevList
                        questionAdapter.submit(currentQuestions)
                        binding.tvQuestionsHeader.text = "Questions (${currentQuestions.size})"
                        lifecycleScope.launch {
                            try {
                                examRepo.updateQuestion(q)
                                AppBulletin.showSuccess(this@EditExamActivity, "Question restored")
                            } catch (e: Exception) {
                                AppBulletin.showError(this@EditExamActivity, "Failed to restore question: ${e.toUserFriendlyMessage()}")
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
