package com.eve.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.eve.app.util.AppBulletin
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageExamsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.eve.app.data.remote.ApiClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageExamsBinding
    private val examRepo = ExamRepository()
    private var examList: MutableList<Exam> = mutableListOf()
    private var selectedIndex: Int = -1

    private val genTestAdapter = GeneratedTestAdapter(
        onStatusToggle = { test, isLive ->
            toggleTestStatus(test, isLive)
        },
        onPreview = { test ->
            previewTest(test)
        },
        onDelete = { test ->
            confirmDeleteTest(test)
        }
    )

    // State of currently loaded exam for dirty checking
    private var initialExam: Exam? = null
    private var currentExamId: String = ""
    private var currentSyllabusUrl: String = ""
    private var currentSyllabusFileName: String = ""
    private var currentSyllabusUploadedAt: Long = 0L
    private var currentAutoGenTime: String = "00:00"

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            handlePdfSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityManageExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { handleBack() }
        binding.compactErrorView.displayMode = com.eve.app.ui.common.ErrorStateView.DisplayMode.COMPACT

        binding.rvGeneratedTestsForExam.layoutManager = LinearLayoutManager(this)
        binding.rvGeneratedTestsForExam.adapter = genTestAdapter

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == selectedIndex) return
                if (isDirty()) {
                    val prevIdx = selectedIndex
                    AlertDialog.Builder(this@ManageExamsActivity)
                        .setTitle("Unsaved Changes")
                        .setMessage("You have unsaved changes. Do you want to save before switching?")
                        .setPositiveButton("Save") { _, _ ->
                            saveExamSettings {
                                switchExam(position)
                            }
                        }
                        .setNegativeButton("Discard") { _, _ ->
                            switchExam(position)
                        }
                        .setNeutralButton("Cancel") { _, _ ->
                            if (prevIdx >= 0) binding.spExam.setSelection(prevIdx)
                        }
                        .show()
                } else {
                    switchExam(position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.switchAutoGen.setOnCheckedChangeListener { _, isChecked ->
            updateTimePickerState(isChecked)
        }

        binding.btnPickTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnUploadPdf.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        binding.btnReplacePdf.setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        binding.btnRemovePdf.setOnClickListener {
            confirmRemovePdf()
        }

        binding.btnGenerateNow.setOnClickListener {
            triggerGenerateNow()
        }

        binding.btnSave.setOnClickListener {
            saveExamSettings()
        }

        loadExams()
    }

    private fun loadExams() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val fetched = examRepo.getExams()
                examList = fetched.sortedWith(
                    compareBy<Exam> { it.categoryOrOther }.thenBy { it.examName }
                ).toMutableList()

                binding.progressBar.visibility = View.GONE
                populateSpinner()

                if (examList.isNotEmpty()) {
                    switchExam(0)
                } else {
                    // Only + Add new exam option
                    switchExam(0)
                }
                binding.compactErrorView.hide()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.compactErrorView.show(
                    type = com.eve.app.ui.common.ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Failed to load exams: ${e.localizedMessage}",
                    onRetry = { loadExams() }
                )
            }
        }
    }

    private fun populateSpinner() {
        val titles = examList.map { "[${it.categoryOrOther}] ${it.examName}" }.toMutableList()
        titles.add("+ Add new exam")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            titles
        )
        binding.spExam.adapter = adapter
    }

    private fun switchExam(position: Int) {
        selectedIndex = position
        if (position < examList.size) {
            val exam = examList[position]
            currentExamId = exam.id
            currentSyllabusUrl = exam.syllabusUrl
            currentSyllabusFileName = exam.syllabusFileName
            currentSyllabusUploadedAt = exam.syllabusUploadedAt
            currentAutoGenTime = exam.autoGenTime.ifBlank { "00:00" }

            binding.etExamName.setText(exam.examName)
            binding.etTestNumber.setText(exam.testNumber.ifBlank { "Test 1" })
            binding.etQuestionCount.setText(if (exam.questionCount > 0) exam.questionCount.toString() else "20")
            
            // Saved exam: auto-generation toggle can be toggled
            binding.switchAutoGen.isEnabled = true
            binding.switchAutoGen.alpha = 1.0f
            binding.switchAutoGen.isChecked = exam.autoGenerationEnabled
            updateTimePickerState(exam.autoGenerationEnabled)
            binding.btnPickTime.text = "Time: $currentAutoGenTime (IST)"

            updateLastRunUi(exam)
            updateSyllabusUi(currentSyllabusFileName, currentSyllabusUrl, currentSyllabusUploadedAt)

            binding.etGenerationPrompt.setText(
                if (exam.generationPrompt.isNotBlank()) exam.generationPrompt else exam.customPromptNotes
            )

            initialExam = getCurrentFormAsExam()
            binding.btnGenerateNow.visibility = View.VISIBLE
            loadGeneratedTestsForExam()
        } else {
            // "+ Add new exam" entry selected:
            // Auto-generation toggle must default to OFF and be disabled until saved
            currentExamId = ""
            currentSyllabusUrl = ""
            currentSyllabusFileName = ""
            currentSyllabusUploadedAt = 0L
            currentAutoGenTime = "00:00"

            binding.etExamName.setText("")
            binding.etTestNumber.setText("Test 1")
            binding.etQuestionCount.setText("20")
            
            binding.switchAutoGen.isChecked = false
            binding.switchAutoGen.isEnabled = false
            binding.switchAutoGen.alpha = 0.5f
            updateTimePickerState(false)
            binding.btnPickTime.text = "Time: 00:00 (IST)"

            binding.tvLastRunStatus.text = "New exam — save exam before enabling auto-generation"
            updateSyllabusUi("", "", 0L)
            binding.etGenerationPrompt.setText("")

            initialExam = getCurrentFormAsExam()
            binding.btnGenerateNow.visibility = View.GONE
            loadGeneratedTestsForExam()
        }
    }

    private fun updateTimePickerState(enabled: Boolean) {
        binding.btnPickTime.isEnabled = enabled
        binding.btnPickTime.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun showTimePicker() {
        val parts = currentAutoGenTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText("Select Auto-generation Time (IST)")
            .build()

        picker.addOnPositiveButtonClickListener {
            val formatted = String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute)
            currentAutoGenTime = formatted
            binding.btnPickTime.text = "Time: $formatted (IST)"
        }

        picker.show(supportFragmentManager, "time_picker")
    }

    private fun updateLastRunUi(exam: Exam) {
        val status = exam.lastGenerationStatus
        val timeStr = if (exam.lastGenerationTime > 0) dateFormat.format(Date(exam.lastGenerationTime)) else exam.lastGeneratedDate
        binding.tvLastRunStatus.text = when {
            status.equals("success", ignoreCase = true) -> "Last run: Success on $timeStr"
            status.equals("failed", ignoreCase = true) -> "Last run: Failed (${exam.lastGenerationError}) - $timeStr"
            status.equals("running", ignoreCase = true) -> "Last run: Currently generating..."
            else -> "Last run: Not run yet"
        }
    }

    private fun updateSyllabusUi(fileName: String, url: String, uploadedAt: Long = 0L) {
        if (url.isNotBlank() && fileName.isNotBlank()) {
            binding.layoutSyllabusUploaded.visibility = View.VISIBLE
            binding.btnUploadPdf.visibility = View.GONE
            binding.tvSyllabusFileName.text = fileName
            binding.tvSyllabusUploadDate.text = if (uploadedAt > 0) {
                "Uploaded: ${dateFormat.format(Date(uploadedAt))}"
            } else {
                "Uploaded syllabus PDF"
            }
        } else {
            binding.layoutSyllabusUploaded.visibility = View.GONE
            binding.btnUploadPdf.visibility = View.VISIBLE
        }
    }

    private fun handlePdfSelected(uri: Uri) {
        var displayName = "syllabus.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
            }
        }

        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            if (bytes.size > 20 * 1024 * 1024) {
                AppBulletin.showError(this, "PDF exceeds 20MB limit.")
                return
            }

            if (currentExamId.isBlank()) {
                AppBulletin.showError(this, "Please save the exam name first before uploading syllabus.")
                return
            }

            binding.progressBarPdf.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val url = examRepo.uploadSyllabusPdf(currentExamId, displayName, bytes)
                    currentSyllabusUrl = url
                    currentSyllabusFileName = displayName
                    binding.progressBarPdf.visibility = View.GONE
                    updateSyllabusUi(displayName, url)
                    AppBulletin.showSuccess(this@ManageExamsActivity, "Syllabus uploaded successfully!")
                } catch (e: Exception) {
                    binding.progressBarPdf.visibility = View.GONE
                    AppBulletin.showError(this@ManageExamsActivity, "Upload failed: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to read PDF file: ${e.localizedMessage}")
        }
    }

    private fun confirmRemovePdf() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Syllabus?")
            .setMessage("Are you sure you want to remove the uploaded syllabus PDF?")
            .setPositiveButton("Remove") { _, _ ->
                binding.progressBarPdf.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        examRepo.removeSyllabusPdf(currentExamId, currentSyllabusUrl)
                        currentSyllabusUrl = ""
                        currentSyllabusFileName = ""
                        currentSyllabusUploadedAt = 0L
                        binding.progressBarPdf.visibility = View.GONE
                        updateSyllabusUi("", "", 0L)
                        AppBulletin.showSuccess(this@ManageExamsActivity, "Syllabus removed")
                    } catch (e: Exception) {
                        binding.progressBarPdf.visibility = View.GONE
                        AppBulletin.showError(this@ManageExamsActivity, "Failed to remove: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun triggerGenerateNow() {
        if (currentExamId.isBlank()) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Generate Test Now?")
            .setMessage("This will trigger AI to generate a fresh test for '${binding.etExamName.text}' immediately using the configured prompt and question count.")
            .setPositiveButton("Generate") { _, _ ->
                binding.btnGenerateNow.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val count = binding.etQuestionCount.text?.toString()?.toIntOrNull() ?: 20
                        val data = mapOf<String, Any>(
                            "examId" to currentExamId,
                            "questionCount" to count,
                            "testNumber" to binding.etTestNumber.text?.toString()?.trim().orEmpty(),
                            "customPromptNotes" to binding.etGenerationPrompt.text?.toString()?.trim().orEmpty()
                        )
                        val res = ApiClient.apiService.triggerAiTestGeneration(data)
                        binding.btnGenerateNow.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        binding.compactErrorView.hide()

                        if (!res.success) {
                            val err = res.error ?: "Generation failed"
                            MaterialAlertDialogBuilder(this@ManageExamsActivity)
                                .setTitle("Generation Failed")
                                .setMessage("Failed to generate test questions:\n\n$err")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            val generatedCount = res.data?.get("count") ?: count
                            MaterialAlertDialogBuilder(this@ManageExamsActivity)
                                .setTitle("Generation Successful")
                                .setMessage("Successfully generated $generatedCount questions for '${binding.etExamName.text}'.\n\nYou can review or publish them in the Generated Tests section below.")
                                .setPositiveButton("OK", null)
                                .show()
                            loadExams()
                            loadGeneratedTestsForExam()
                        }
                    } catch (e: Exception) {
                        binding.btnGenerateNow.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        val err = e.localizedMessage ?: "Unknown error"
                        MaterialAlertDialogBuilder(this@ManageExamsActivity)
                            .setTitle("Generation Failed")
                            .setMessage("Failed to generate test questions:\n\n$err")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadGeneratedTestsForExam() {
        if (currentExamId.isBlank()) {
            genTestAdapter.submit(emptyList())
            binding.progressBarGenTests.visibility = View.GONE
            binding.tvNoGenTests.visibility = View.VISIBLE
            binding.tvNoGenTests.text = "Save exam first to view and generate test batches."
            return
        }

        binding.progressBarGenTests.visibility = View.VISIBLE
        binding.tvNoGenTests.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val tests = examRepo.getGeneratedTests(currentExamId)
                binding.progressBarGenTests.visibility = View.GONE
                genTestAdapter.submit(tests)
                if (tests.isEmpty()) {
                    binding.tvNoGenTests.visibility = View.VISIBLE
                    binding.tvNoGenTests.text = "No generated tests for this exam yet."
                } else {
                    binding.tvNoGenTests.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.progressBarGenTests.visibility = View.GONE
                binding.tvNoGenTests.visibility = View.VISIBLE
                binding.tvNoGenTests.text = "Error loading tests: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    private fun toggleTestStatus(test: GeneratedTest, isLive: Boolean) {
        val newStatus = if (isLive) "live" else "paused"
        lifecycleScope.launch {
            try {
                examRepo.updateGeneratedTestStatus(test.id, newStatus)
                AppBulletin.showSuccess(
                    this@ManageExamsActivity,
                    "Test is now ${newStatus.uppercase()}"
                )
                loadGeneratedTestsForExam()
            } catch (e: Exception) {
                AppBulletin.showError(
                    this@ManageExamsActivity,
                    "Failed to update status: ${e.localizedMessage}"
                )
                loadGeneratedTestsForExam()
            }
        }
    }

    private fun previewTest(test: GeneratedTest) {
        if (test.questions.isEmpty()) {
            AppBulletin.showError(this, "No questions found in this test payload.")
            return
        }

        val formattedText = StringBuilder()
        test.questions.forEachIndexed { index, q ->
            formattedText.append("Q${index + 1}: ${q.questionText}\n")
            formattedText.append("A) ${q.optionA}\n")
            formattedText.append("B) ${q.optionB}\n")
            formattedText.append("C) ${q.optionC}\n")
            formattedText.append("D) ${q.optionD}\n")
            formattedText.append("Correct: ${q.correctAnswer}\n")
            if (q.explanation.isNotBlank()) {
                formattedText.append("Explanation: ${q.explanation}\n")
            }
            formattedText.append("\n--------------------\n\n")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(test.displayTitle)
            .setMessage(formattedText.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun confirmDeleteTest(test: GeneratedTest) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Generated Test?")
            .setMessage("Are you sure you want to permanently delete '${test.displayTitle}'? This batch of questions will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBarGenTests.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        examRepo.deleteGeneratedTest(test.id)
                        AppBulletin.showSuccess(this@ManageExamsActivity, "Test batch deleted")
                        loadGeneratedTestsForExam()
                    } catch (e: Exception) {
                        binding.progressBarGenTests.visibility = View.GONE
                        AppBulletin.showError(this@ManageExamsActivity, "Failed to delete: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentFormAsExam(): Exam {
        val count = binding.etQuestionCount.text?.toString()?.toIntOrNull() ?: 20
        return Exam(
            id = currentExamId,
            examName = binding.etExamName.text?.toString()?.trim().orEmpty(),
            testNumber = binding.etTestNumber.text?.toString()?.trim().orEmpty(),
            questionCount = count,
            autoGenerationEnabled = binding.switchAutoGen.isChecked,
            autoGenTime = currentAutoGenTime,
            syllabusUrl = currentSyllabusUrl,
            syllabusFileName = currentSyllabusFileName,
            syllabusUploadedAt = currentSyllabusUploadedAt,
            generationPrompt = binding.etGenerationPrompt.text?.toString()?.trim().orEmpty()
        )
    }

    private fun isDirty(): Boolean {
        val current = getCurrentFormAsExam()
        val init = initialExam ?: return false
        return current.examName != init.examName ||
            current.testNumber != init.testNumber ||
            current.questionCount != init.questionCount ||
            current.autoGenerationEnabled != init.autoGenerationEnabled ||
            current.autoGenTime != init.autoGenTime ||
            current.syllabusUrl != init.syllabusUrl ||
            current.generationPrompt != init.generationPrompt
    }

    private fun saveExamSettings(onSuccess: (() -> Unit)? = null) {
        val name = binding.etExamName.text?.toString()?.trim().orEmpty()
        if (name.length < 2) {
            binding.tilExamName.error = "Exam name must be at least 2 characters"
            binding.etExamName.requestFocus()
            return
        } else {
            binding.tilExamName.error = null
        }

        val testNumber = binding.etTestNumber.text?.toString()?.trim().orEmpty().ifBlank { "Test 1" }
        val count = binding.etQuestionCount.text?.toString()?.toIntOrNull() ?: 0
        if (count !in 1..200) {
            binding.tilQuestionCount.error = "Question count must be between 1 and 200"
            binding.etQuestionCount.requestFocus()
            return
        } else {
            binding.tilQuestionCount.error = null
        }

        val autoGen = binding.switchAutoGen.isChecked
        val prompt = binding.etGenerationPrompt.text?.toString()?.trim().orEmpty()

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                if (currentExamId.isNotBlank()) {
                    // Update existing
                    examRepo.updateExamFullSettings(
                        examId = currentExamId,
                        examName = name,
                        testNumber = testNumber,
                        questionCount = count,
                        autoGenEnabled = autoGen,
                        autoGenTime = currentAutoGenTime,
                        syllabusUrl = currentSyllabusUrl,
                        syllabusFileName = currentSyllabusFileName,
                        generationPrompt = prompt
                    )
                    AppBulletin.showSuccess(this@ManageExamsActivity, "Settings saved for '$name'")
                } else {
                    // Create new
                    val newId = examRepo.addExam(
                        name = name,
                        minutes = 30,
                        category = "Other",
                        testNumber = testNumber,
                        questionCount = count,
                        autoGenEnabled = autoGen,
                        autoGenTime = currentAutoGenTime,
                        generationPrompt = prompt
                    )
                    currentExamId = newId
                    // Once saved at least once, enable auto-generation toggle!
                    binding.switchAutoGen.isEnabled = true
                    binding.switchAutoGen.alpha = 1.0f
                    binding.btnGenerateNow.visibility = View.VISIBLE
                    AppBulletin.showSuccess(this@ManageExamsActivity, "New exam '$name' created!")
                }

                initialExam = getCurrentFormAsExam()
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.compactErrorView.hide()
                loadExams()
                loadGeneratedTestsForExam()
                onSuccess?.invoke()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.compactErrorView.show(
                    type = com.eve.app.ui.common.ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Failed to save: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleBack() {
        if (isDirty()) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save before leaving?")
                .setPositiveButton("Save") { _, _ ->
                    saveExamSettings { finish() }
                }
                .setNegativeButton("Discard") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }
}
