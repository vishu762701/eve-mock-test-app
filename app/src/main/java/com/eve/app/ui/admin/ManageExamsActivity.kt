package com.eve.app.ui.admin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageExamsBinding
import com.eve.app.util.AppBulletin
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageExamsBinding
    private val examRepo = ExamRepository()
    private val auditLogRepo = AuditLogRepository()
    private val apiUsageRepo = ApiUsageRepository()

    private var examList: MutableList<Exam> = mutableListOf()
    private lateinit var dropdownAdapter: ExamDropdownAdapter

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
    private var currentParentExamId: String = ""

    private var isNewMode: Boolean = false
    private var previouslySelectedExamId: String = ""

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            handlePdfSelected(uri)
        }
    }

    data class ExamDropdownEntry(val exam: Exam, val displayLabel: String) {
        override fun toString(): String = displayLabel
    }

    inner class ExamDropdownAdapter(context: Context) :
        ArrayAdapter<ExamDropdownEntry>(context, android.R.layout.simple_dropdown_item_1line), Filterable {

        private val allEntries = mutableListOf<ExamDropdownEntry>()
        private var currentFiltered = mutableListOf<ExamDropdownEntry>()

        fun setExams(exams: List<Exam>) {
            allEntries.clear()
            val entries = exams.map { exam ->
                val label = if (exam.parentExamId.isNotBlank()) {
                    val parent = exams.find { it.id == exam.parentExamId }
                    val parentName = parent?.examName ?: "Main"
                    "$parentName \u203A ${exam.examName}"
                } else {
                    exam.examName
                }
                ExamDropdownEntry(exam, label)
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel })

            allEntries.addAll(entries)
            currentFiltered.clear()
            currentFiltered.addAll(entries)
            clear()
            addAll(currentFiltered)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = currentFiltered.size
        override fun getItem(position: Int): ExamDropdownEntry? = currentFiltered.getOrNull(position)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent) as TextView
            val item = getItem(position)
            view.text = item?.displayLabel.orEmpty()
            view.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.eve_text))
            view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.eve_surface))
            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.trim().orEmpty()
                    val filtered = if (query.isEmpty()) {
                        allEntries
                    } else {
                        allEntries.filter { it.displayLabel.contains(query, ignoreCase = true) }
                    }
                    val results = FilterResults()
                    results.values = filtered
                    results.count = filtered.size
                    return results
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    currentFiltered = (results?.values as? List<ExamDropdownEntry>)?.toMutableList() ?: mutableListOf()
                    clear()
                    addAll(currentFiltered)
                    notifyDataSetChanged()
                }

                override fun convertResultToString(resultValue: Any?): CharSequence {
                    return (resultValue as? ExamDropdownEntry)?.displayLabel ?: super.convertResultToString(resultValue)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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

        // Dropdown setup
        dropdownAdapter = ExamDropdownAdapter(this)
        binding.actvExam.setAdapter(dropdownAdapter)
        binding.actvExam.threshold = 0

        binding.actvExam.setOnClickListener {
            if (!isNewMode && !binding.actvExam.isPopupShowing) {
                binding.actvExam.showDropDown()
            }
        }

        binding.actvExam.setOnItemClickListener { _, _, position, _ ->
            val item = dropdownAdapter.getItem(position) ?: return@setOnItemClickListener
            handleItemSelection(item.exam)
        }

        binding.btnNewExam.setOnClickListener {
            if (isNewMode) {
                if (isDirty()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Unsaved Changes")
                        .setMessage("You have unsaved changes. Do you want to save before canceling?")
                        .setPositiveButton("Save") { _, _ ->
                            saveExamSettings {
                                exitNewModeAndRestore()
                            }
                        }
                        .setNegativeButton("Discard") { _, _ ->
                            exitNewModeAndRestore()
                        }
                        .setNeutralButton("Cancel", null)
                        .show()
                } else {
                    exitNewModeAndRestore()
                }
            } else {
                if (isDirty()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Unsaved Changes")
                        .setMessage("You have unsaved changes. Do you want to save before creating a new exam?")
                        .setPositiveButton("Save") { _, _ ->
                            saveExamSettings {
                                enterNewMode()
                            }
                        }
                        .setNegativeButton("Discard") { _, _ ->
                            enterNewMode()
                        }
                        .setNeutralButton("Cancel", null)
                        .show()
                } else {
                    enterNewMode()
                }
            }
        }

        binding.actvParentExam.setOnClickListener {
            if (binding.tilParentExam.isEnabled && !binding.actvParentExam.isPopupShowing) {
                binding.actvParentExam.showDropDown()
            }
        }

        // AI Prompt internal scrolling inside parent ScrollView
        binding.etGenerationPrompt.setOnTouchListener { v, event ->
            if (v.canScrollVertically(1) || v.canScrollVertically(-1)) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            false
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

    private fun handleItemSelection(exam: Exam) {
        if (isDirty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save before switching?")
                .setPositiveButton("Save") { _, _ ->
                    saveExamSettings {
                        loadExam(exam)
                    }
                }
                .setNegativeButton("Discard") { _, _ ->
                    loadExam(exam)
                }
                .setNeutralButton("Cancel") { _, _ ->
                    binding.actvExam.setText(initialExam?.examName.orEmpty(), false)
                }
                .setOnCancelListener {
                    binding.actvExam.setText(initialExam?.examName.orEmpty(), false)
                }
                .show()
        } else {
            loadExam(exam)
        }
    }

    private fun loadExams(targetExamId: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val fetched = examRepo.getExams()
                examList = fetched.toMutableList()

                binding.progressBar.visibility = View.GONE
                dropdownAdapter.setExams(examList)

                if (targetExamId != null) {
                    val target = examList.find { it.id == targetExamId }
                    if (target != null) {
                        loadExam(target)
                    } else if (examList.isNotEmpty()) {
                        loadExam(examList[0])
                    } else {
                        enterNewMode()
                    }
                } else if (currentExamId.isNotBlank()) {
                    val current = examList.find { it.id == currentExamId }
                    if (current != null) {
                        loadExam(current)
                    } else if (examList.isNotEmpty()) {
                        loadExam(examList[0])
                    } else {
                        enterNewMode()
                    }
                } else if (examList.isNotEmpty()) {
                    loadExam(examList[0])
                } else {
                    enterNewMode()
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

    private fun loadExam(exam: Exam) {
        exitNewMode()

        currentExamId = exam.id
        currentParentExamId = exam.parentExamId
        currentSyllabusUrl = exam.syllabusUrl
        currentSyllabusFileName = exam.syllabusFileName
        currentSyllabusUploadedAt = exam.syllabusUploadedAt
        currentAutoGenTime = exam.autoGenTime.ifBlank { "00:00" }

        binding.tilSelectExam.error = null
        binding.tilParentExam.error = null
        binding.tilDuration.error = null
        binding.tilTestNumber.error = null
        binding.tilQuestionCount.error = null
        binding.tilNegativeMarking.error = null

        binding.actvExam.setText(exam.examName, false)
        updateParentExamDropdown()
        binding.etDuration.setText(if (exam.timeLimitMinutes > 0) exam.timeLimitMinutes.toString() else "30")
        binding.etTestNumber.setText(exam.testNumber.ifBlank { "Test 1" })
        binding.etQuestionCount.setText(if (exam.questionCount > 0) exam.questionCount.toString() else "20")
        binding.etNegativeMarking.setText(exam.negativeMarkingText.ifBlank { "0" })

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

        binding.btnGenerateNow.visibility = View.VISIBLE
        initialExam = getCurrentFormAsExam()
        loadGeneratedTestsForExam()
    }

    private fun enterNewMode() {
        isNewMode = true
        previouslySelectedExamId = currentExamId
        currentExamId = ""
        currentParentExamId = ""
        currentSyllabusUrl = ""
        currentSyllabusFileName = ""
        currentSyllabusUploadedAt = 0L
        currentAutoGenTime = "00:00"

        binding.btnNewExam.text = "Cancel"
        binding.tilSelectExam.hint = "Enter new exam name"
        binding.tilSelectExam.endIconMode = TextInputLayout.END_ICON_NONE
        binding.actvExam.setAdapter(null)
        binding.actvExam.setText("", false)
        binding.actvExam.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        updateParentExamDropdown()

        binding.etDuration.setText("")
        binding.etTestNumber.setText("")
        binding.etQuestionCount.setText("")
        binding.etNegativeMarking.setText("")

        binding.switchAutoGen.isChecked = false
        binding.switchAutoGen.isEnabled = true
        binding.switchAutoGen.alpha = 1.0f
        updateTimePickerState(false)
        binding.btnPickTime.text = "Time: 00:00 (IST)"

        binding.tvLastRunStatus.text = "New exam — save exam before generating tests"
        updateSyllabusUi("", "", 0L)
        binding.etGenerationPrompt.setText("")

        genTestAdapter.submit(emptyList())
        binding.progressBarGenTests.visibility = View.GONE
        binding.tvNoGenTests.visibility = View.VISIBLE
        binding.tvNoGenTests.text = "Save exam first to view and generate test batches."

        binding.btnGenerateNow.visibility = View.GONE

        binding.tilSelectExam.error = null
        binding.tilParentExam.error = null
        binding.tilDuration.error = null
        binding.tilTestNumber.error = null
        binding.tilQuestionCount.error = null
        binding.tilNegativeMarking.error = null

        initialExam = getCurrentFormAsExam()

        binding.actvExam.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.actvExam, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun exitNewModeAndRestore() {
        exitNewMode()
        val targetId = previouslySelectedExamId.ifBlank { examList.firstOrNull()?.id.orEmpty() }
        val target = examList.find { it.id == targetId } ?: examList.firstOrNull()
        if (target != null) {
            loadExam(target)
        } else {
            enterNewMode()
        }
    }

    private fun exitNewMode() {
        isNewMode = false
        binding.btnNewExam.text = "+ New Exam"
        binding.tilSelectExam.hint = "Select exam"
        binding.tilSelectExam.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
        binding.actvExam.setAdapter(dropdownAdapter)
    }

    private fun updateParentExamDropdown() {
        val hasSubExams = currentExamId.isNotBlank() && examList.any { it.parentExamId == currentExamId }

        if (hasSubExams) {
            binding.tilParentExam.isEnabled = false
            binding.actvParentExam.isEnabled = false
            currentParentExamId = ""
            binding.actvParentExam.setText("None (this is a main exam)", false)
            binding.tilParentExam.helperText = "This exam has sub-exams, so it cannot be a sub-exam."
            return
        }

        binding.tilParentExam.isEnabled = true
        binding.actvParentExam.isEnabled = true
        binding.tilParentExam.helperText = "Leave as None for a main exam. Choose a main exam to make this a sub-exam (e.g. a subject under 3rd Grade)."

        val eligibleParents = examList.filter { it.parentExamId.isBlank() && (currentExamId.isBlank() || it.id != currentExamId) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.examName })

        val options = mutableListOf("None (this is a main exam)")
        options.addAll(eligibleParents.map { it.examName })

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        binding.actvParentExam.setAdapter(adapter)

        val selectedName = if (currentParentExamId.isNotBlank()) {
            eligibleParents.find { it.id == currentParentExamId }?.examName ?: "None (this is a main exam)"
        } else {
            "None (this is a main exam)"
        }
        binding.actvParentExam.setText(selectedName, false)

        binding.actvParentExam.setOnItemClickListener { _, _, position, _ ->
            currentParentExamId = if (position == 0) {
                ""
            } else {
                eligibleParents.getOrNull(position - 1)?.id.orEmpty()
            }
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
                AppBulletin.showError(this, "Please save the exam first before uploading syllabus.")
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

        val examName = binding.actvExam.text?.toString()?.trim().orEmpty()
        MaterialAlertDialogBuilder(this)
            .setTitle("Generate Test Now?")
            .setMessage("This will trigger AI to generate a fresh test for '$examName' immediately using the configured prompt and question count.")
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
                            auditLogRepo.recordLog(
                                AdminAuditLog.ACTION_GENERATE_NOW_TRIGGERED,
                                "Triggered manual generation for '$examName': $generatedCount questions"
                            )
                            apiUsageRepo.incrementGeminiCalls()
                            MaterialAlertDialogBuilder(this@ManageExamsActivity)
                                .setTitle("Generation Successful")
                                .setMessage("Successfully generated $generatedCount questions for '$examName'.\n\nYou can review or publish them in the Generated Tests section below.")
                                .setPositiveButton("OK", null)
                                .show()
                            loadExams(targetExamId = currentExamId)
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
        val duration = binding.etDuration.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val count = binding.etQuestionCount.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val negText = binding.etNegativeMarking.text?.toString()?.trim().orEmpty()
        val parsedNeg = parseNegativeMarking(negText)
        val negVal = parsedNeg?.second ?: 0.0

        return Exam(
            id = currentExamId,
            examName = binding.actvExam.text?.toString()?.trim().orEmpty(),
            timeLimitMinutes = duration,
            testNumber = binding.etTestNumber.text?.toString()?.trim().orEmpty(),
            questionCount = count,
            negativeMarkingText = negText,
            negativeMarkingValue = negVal,
            parentExamId = currentParentExamId,
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
            current.parentExamId != init.parentExamId ||
            current.timeLimitMinutes != init.timeLimitMinutes ||
            current.testNumber != init.testNumber ||
            current.questionCount != init.questionCount ||
            current.negativeMarkingText != init.negativeMarkingText ||
            current.autoGenerationEnabled != init.autoGenerationEnabled ||
            current.autoGenTime != init.autoGenTime ||
            current.syllabusUrl != init.syllabusUrl ||
            current.generationPrompt != init.generationPrompt
    }

    private fun saveExamSettings(onSuccess: (() -> Unit)? = null) {
        // 1. Exam Name Validation: trim, min 2 chars, unique case-insensitively among exams with the SAME parent
        val name = binding.actvExam.text?.toString()?.trim().orEmpty()
        if (name.length < 2) {
            binding.tilSelectExam.error = "Exam name must be at least 2 characters"
            binding.actvExam.requestFocus()
            return
        }
        val isDuplicate = examList.any {
            it.id != currentExamId &&
                it.parentExamId == currentParentExamId &&
                it.examName.trim().equals(name, ignoreCase = true)
        }
        if (isDuplicate) {
            binding.tilSelectExam.error = "An exam with this name already exists here"
            binding.actvExam.requestFocus()
            return
        }
        binding.tilSelectExam.error = null

        // 2. Main Exam Validation
        if (currentParentExamId.isNotBlank()) {
            val parent = examList.find { it.id == currentParentExamId }
            if (parent == null || parent.parentExamId.isNotBlank() || parent.id == currentExamId) {
                binding.tilParentExam.error = "Invalid parent exam"
                binding.actvParentExam.requestFocus()
                return
            }
        }
        binding.tilParentExam.error = null

        // 3. Test Duration Validation: empty -> "Enter minutes between 1 and 600"
        val durationRaw = binding.etDuration.text?.toString()?.trim().orEmpty()
        val duration = durationRaw.toIntOrNull()
        if (durationRaw.isEmpty() || duration == null || duration !in 1..600) {
            binding.tilDuration.error = "Enter minutes between 1 and 600"
            binding.etDuration.requestFocus()
            return
        }
        binding.tilDuration.error = null

        // 4. Test Number Validation: empty -> "Enter test number, e.g. Test 1"
        val testNumber = binding.etTestNumber.text?.toString()?.trim().orEmpty()
        if (testNumber.isEmpty()) {
            binding.tilTestNumber.error = "Enter test number, e.g. Test 1"
            binding.etTestNumber.requestFocus()
            return
        }
        binding.tilTestNumber.error = null

        // 5. Question Count Validation: integer 1..200
        val countRaw = binding.etQuestionCount.text?.toString()?.trim().orEmpty()
        val count = countRaw.toIntOrNull()
        if (countRaw.isEmpty() || count == null || count !in 1..200) {
            binding.tilQuestionCount.error = "Question count must be between 1 and 200"
            binding.etQuestionCount.requestFocus()
            return
        }
        binding.tilQuestionCount.error = null

        // 6. Negative Marking Validation: empty -> "Enter like 1/3, 1/4, 0.25 or 0"
        val negStr = binding.etNegativeMarking.text?.toString()?.trim().orEmpty()
        val parsedNeg = parseNegativeMarking(negStr)
        if (negStr.isEmpty() || parsedNeg == null) {
            binding.tilNegativeMarking.error = "Enter like 1/3, 1/4, 0.25 or 0"
            binding.etNegativeMarking.requestFocus()
            return
        }
        binding.tilNegativeMarking.error = null

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
                        generationPrompt = prompt,
                        timeLimitMinutes = duration,
                        negativeMarkingText = parsedNeg.first,
                        negativeMarkingValue = parsedNeg.second,
                        parentExamId = currentParentExamId
                    )
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_EXAM_EDITED,
                        "Updated exam settings & AI config for '$name'"
                    )
                    apiUsageRepo.incrementDocumentWrites(1)
                    AppBulletin.showSuccess(this@ManageExamsActivity, "Settings saved for '$name'")
                } else {
                    // Create new
                    val newId = examRepo.addExam(
                        name = name,
                        minutes = duration,
                        category = "Other",
                        testNumber = testNumber,
                        questionCount = count,
                        autoGenEnabled = autoGen,
                        autoGenTime = currentAutoGenTime,
                        generationPrompt = prompt,
                        negativeMarkingText = parsedNeg.first,
                        negativeMarkingValue = parsedNeg.second,
                        parentExamId = currentParentExamId
                    )
                    currentExamId = newId
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_EXAM_CREATED,
                        "Created exam '$name' with AI generation settings"
                    )
                    apiUsageRepo.incrementDocumentWrites(1)
                    binding.btnGenerateNow.visibility = View.VISIBLE
                    exitNewMode()
                    AppBulletin.showSuccess(this@ManageExamsActivity, "New exam '$name' created!")
                }

                initialExam = getCurrentFormAsExam()
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.compactErrorView.hide()
                loadExams(targetExamId = currentExamId)
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
            MaterialAlertDialogBuilder(this)
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

    companion object {
        fun parseNegativeMarking(raw: String): Pair<String, Double>? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.contains(" ")) return null
            if (trimmed == "0" || trimmed == "0.0" || trimmed == "0.00") {
                return Pair(trimmed, 0.0)
            }
            if (trimmed.contains("/")) {
                val parts = trimmed.split("/")
                if (parts.size != 2) return null
                if (!parts[0].all { it.isDigit() } || !parts[1].all { it.isDigit() }) return null
                val a = parts[0].toIntOrNull() ?: return null
                val b = parts[1].toIntOrNull() ?: return null
                if (a < 1 || b < 1 || a > b || b > 100) return null
                return Pair(trimmed, a.toDouble() / b.toDouble())
            }
            val num = trimmed.toDoubleOrNull() ?: return null
            if (num < 0.0 || num > 1.0) return null
            return Pair(trimmed, num)
        }
    }
}
