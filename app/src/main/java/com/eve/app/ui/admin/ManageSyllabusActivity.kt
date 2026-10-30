package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageSyllabusBinding
import com.eve.app.databinding.ItemAdminSyllabusBinding
import com.eve.app.designsystem.UiArcFileUploadView
import com.eve.app.designsystem.UiArcMorphSelectView
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.util.AppBulletin
import com.eve.app.util.SecurityHelper
import com.eve.app.util.AdminAccessGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageSyllabusActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageSyllabusBinding
    private val examRepo = ExamRepository()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private var allExams: List<Exam> = emptyList()
    private var selectedExamForNewSyllabus: Exam? = null
    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String = ""
    private var selectedPdfBytes: ByteArray? = null
    private var adminAccessGranted = false
    private var uploadInFlight = false
    private var uploadSucceeded = false
    private var uploadFailed = false
    private var selectedExamRestoreId: String? = null
    private var newPdfReadJob: Job? = null
    private var newPdfGeneration = 0L
    private var replacementJob: Job? = null
    private var replacementGeneration = 0L

    private var targetExamForReplace: Exam? = null
    private val replacePdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val exam = targetExamForReplace ?: return@registerForActivityResult
            handleReplacePdfSelected(exam, uri)
        }
    }

    private val newPdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            handleNewPdfSelected(uri)
        }
    }

    private lateinit var syllabusAdapter: AdminSyllabusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedExamRestoreId = savedInstanceState?.getString("syllabus_selected_exam_id")
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityManageSyllabusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            adminAccessGranted = true
            setupRecyclerView()
            setupAddSyllabusForm()
            setupSearch()
            loadExamsAndSyllabuses()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("syllabus_selected_exam_id", selectedExamForNewSyllabus?.id)
    }

    private fun updateUploadStepper() {
        val validExam = selectedExamForNewSyllabus?.id?.let { id ->
            id.isNotBlank() && allExams.any { it.id == id }
        } ?: false
        val validPdf = selectedPdfUri != null &&
            selectedPdfBytes?.let {
                it.isNotEmpty() && it.size.toLong() <= UiArcFileUploadView.MAX_FILE_SIZE_BYTES
            } == true
        binding.stepperNewSyllabus.update(
            validExam, validExam && validPdf, uploadInFlight, uploadSucceeded, uploadFailed
        )
    }

    private fun setupRecyclerView() {
        syllabusAdapter = AdminSyllabusAdapter(
            onReplace = { exam ->
                targetExamForReplace = exam
                replacePdfPicker.launch(arrayOf("application/pdf"))
            },
            onReassign = { exam ->
                showReassignDialog(exam)
            },
            onDelete = { exam ->
                confirmDeleteSyllabus(exam)
            }
        )
        binding.rvSyllabuses.layoutManager = LinearLayoutManager(this)
        binding.rvSyllabuses.adapter = syllabusAdapter
    }

    private fun setupAddSyllabusForm() {
        binding.btnChoosePdf.setOnClickListener {
            newPdfPicker.launch(arrayOf("application/pdf"))
        }

        binding.btnClearSelectedPdf.setOnClickListener {
            // Cancel an in-flight ContentProvider read, otherwise its late result can
            // silently restore a PDF after the user explicitly cleared selection.
            newPdfGeneration++
            newPdfReadJob?.cancel()
            newPdfReadJob = null
            selectedPdfUri = null
            selectedPdfName = ""
            selectedPdfBytes = null
            binding.btnChoosePdf.isEnabled = true
            binding.btnUploadSyllabus.isEnabled = true
            binding.fileUploadView.clearSelectedFile()
            uploadSucceeded = false
            uploadFailed = false
            updateUploadStepper()
        }

        binding.btnUploadSyllabus.setOnClickListener {
            if (uploadInFlight) return@setOnClickListener
            val exam = selectedExamForNewSyllabus
            if (exam == null) {
                binding.morphSelectExam.setError("Please select an exam first")
                binding.tilSelectExam.error = "Please select an exam first"
                AppBulletin.showError(this, "Please select an exam first")
                return@setOnClickListener
            }
            binding.morphSelectExam.setError(null)
            binding.tilSelectExam.error = null

            val bytes = selectedPdfBytes
            val fileName = selectedPdfName.ifBlank { "syllabus.pdf" }
            if (bytes == null || bytes.isEmpty()) {
                AppBulletin.showError(this, "Please choose a valid PDF file to upload.")
                return@setOnClickListener
            }

            uploadInFlight = true
            uploadSucceeded = false
            uploadFailed = false
            updateUploadStepper()
            binding.progressBarUpload.visibility = View.VISIBLE
            binding.btnUploadSyllabus.isEnabled = false
            binding.btnChoosePdf.isEnabled = false
            binding.btnClearSelectedPdf.isEnabled = false
            binding.fileUploadView.setUploading()

            lifecycleScope.launch {
                try {
                    examRepo.uploadSyllabusPdf(exam.id, fileName, bytes)
                    uploadInFlight = false
                    uploadSucceeded = true
                    updateUploadStepper()
                    binding.progressBarUpload.visibility = View.GONE
                    binding.btnUploadSyllabus.isEnabled = true
                    binding.fileUploadView.setUploadSuccess {
                        resetSyllabusUploadForm()
                        AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus uploaded and linked to ${exam.examName}")
                        loadExamsAndSyllabuses()
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (e: Exception) {
                    uploadFailed = true
                    binding.progressBarUpload.visibility = View.GONE
                    binding.btnUploadSyllabus.isEnabled = true
                    binding.fileUploadView.setUploadFailed()
                    AppBulletin.showError(this@ManageSyllabusActivity, "Upload failed: ${e.localizedMessage}")
                } finally {
                    uploadInFlight = false
                    binding.btnChoosePdf.isEnabled = true
                    binding.btnClearSelectedPdf.isEnabled = true
                    updateUploadStepper()
                }
            }
        }
    }

    private fun resetSyllabusUploadForm() {
        selectedPdfUri = null
        selectedPdfName = ""
        selectedPdfBytes = null
        binding.fileUploadView.reset()
        binding.actvSelectExam.setText("", false)
        binding.morphSelectExam.clearSelection()
        binding.morphSelectExam.setError(null)
        selectedExamForNewSyllabus = null
        uploadSucceeded = false
        uploadFailed = false
        updateUploadStepper()
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            filterSyllabuses(text?.toString().orEmpty())
        }
    }

    private fun loadExamsAndSyllabuses() {
        binding.progressBarTop.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                allExams = examRepo.getExams()
                binding.progressBarTop.visibility = View.GONE
                updateExamDropdown()
                filterSyllabuses(binding.etSearch.text?.toString().orEmpty())
            } catch (e: Exception) {
                binding.progressBarTop.visibility = View.GONE
                AppBulletin.showError(this@ManageSyllabusActivity, "Failed to load exams: ${e.localizedMessage}")
            }
        }
    }

    private fun updateExamDropdown() {
        if (selectedExamRestoreId != null) {
            selectedExamForNewSyllabus =
                allExams.firstOrNull { it.id.isNotBlank() && it.id == selectedExamRestoreId }
            selectedExamRestoreId = null
        }
        val morphOptions = allExams.map {
            com.eve.app.designsystem.UiArcMorphSelectView.Option(
                id = it.id,
                label = it.examName,
                meta = it.categoryOrOther,
                iconRes = R.drawable.ic_topic
            )
        }
        binding.morphSelectExam.setOptions(morphOptions, selectedExamForNewSyllabus?.id)
        binding.morphSelectExam.onOptionSelectedListener = { opt ->
            selectedExamForNewSyllabus = allExams.find { it.id == opt.id }
            binding.morphSelectExam.setError(null)
            binding.tilSelectExam.error = null
            binding.actvSelectExam.setText(opt.label, false)
            uploadSucceeded = false
            updateUploadStepper()
        }

        val dropdownItems = allExams.map { "${it.examName} (${it.categoryOrOther})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dropdownItems)
        binding.actvSelectExam.setAdapter(adapter)

        binding.actvSelectExam.setOnItemClickListener { parent, _, position, _ ->
            val selectedText = parent.getItemAtPosition(position)?.toString() ?: ""
            selectedExamForNewSyllabus = allExams.firstOrNull { exam ->
                val itemStr = "${exam.examName} (${exam.categoryOrOther})"
                itemStr == selectedText || exam.examName == selectedText
            }
            binding.morphSelectExam.setError(null)
            binding.tilSelectExam.error = null
            selectedExamForNewSyllabus?.let { exam ->
                binding.morphSelectExam.setOptions(morphOptions, exam.id)
            }
            uploadSucceeded = false
            updateUploadStepper()
        }
        updateUploadStepper()
    }

    private fun filterSyllabuses(query: String) {
        val syllabusExams = allExams.filter { it.syllabusUrl.isNotBlank() }
        val filtered = if (query.isBlank()) {
            syllabusExams
        } else {
            syllabusExams.filter {
                it.examName.contains(query, ignoreCase = true) ||
                    it.syllabusFileName.contains(query, ignoreCase = true) ||
                    it.categoryOrOther.contains(query, ignoreCase = true)
            }
        }

        binding.tvSyllabusCount.text = "${syllabusExams.size} Active"
        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvSyllabuses.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvSyllabuses.visibility = View.VISIBLE
            syllabusAdapter.submit(filtered)
        }
    }

    private fun safePdfDisplayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)?.takeIf { it.isNotBlank() }
            } ?: "syllabus.pdf"
        } catch (_: Exception) {
            "syllabus.pdf"
        }
    }

    private suspend fun readPdfWithinLimit(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { input ->
            PdfSelectionReader.requirePdfContent(
                PdfSelectionReader.readBounded(input, UiArcFileUploadView.MAX_FILE_SIZE_BYTES)
            )
        } ?: throw java.io.IOException("Unable to open the selected PDF.")
    }

    private fun handleNewPdfSelected(uri: Uri) {
        if (!adminAccessGranted) return
        val displayName = safePdfDisplayName(uri)
        newPdfReadJob?.cancel()
        val generation = ++newPdfGeneration
        selectedPdfUri = null
        selectedPdfName = ""
        selectedPdfBytes = null
        binding.fileUploadView.clearSelectedFile()
        uploadSucceeded = false
        uploadFailed = false
        updateUploadStepper()
        binding.btnChoosePdf.isEnabled = false
        binding.btnUploadSyllabus.isEnabled = false

        newPdfReadJob = lifecycleScope.launch {
            try {
                // Bounded streaming prevents reading an oversized provider document into RAM.
                // File I/O must not block the main thread or the UIArc picker animation.
                val bytes = readPdfWithinLimit(uri)
                selectedPdfUri = uri
                selectedPdfName = displayName
                selectedPdfBytes = bytes
                binding.fileUploadView.bindSelectedFile(displayName, bytes.size.toLong())
                updateUploadStepper()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (tooLarge: PdfSelectionReader.FileTooLargeException) {
                AppBulletin.showError(this@ManageSyllabusActivity, "PDF exceeds 20MB limit.")
            } catch (error: Exception) {
                AppBulletin.showError(this@ManageSyllabusActivity,
                    "Failed to read PDF file: ${error.localizedMessage}")
            } finally {
                if (generation == newPdfGeneration) {
                    binding.btnChoosePdf.isEnabled = true
                    binding.btnUploadSyllabus.isEnabled = true
                    updateUploadStepper()
                }
            }
        }
    }

    private fun handleReplacePdfSelected(exam: Exam, uri: Uri) {
        if (!adminAccessGranted) return
        val displayName = safePdfDisplayName(uri)
        replacementJob?.cancel()
        val generation = ++replacementGeneration
        binding.progressBarTop.visibility = View.VISIBLE

        replacementJob = lifecycleScope.launch {
            try {
                val bytes = readPdfWithinLimit(uri)
                examRepo.uploadSyllabusPdf(exam.id, displayName, bytes)
                AppBulletin.showSuccess(this@ManageSyllabusActivity,
                    "Syllabus updated for ${exam.examName}")
                loadExamsAndSyllabuses()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (tooLarge: PdfSelectionReader.FileTooLargeException) {
                AppBulletin.showError(this@ManageSyllabusActivity, "PDF exceeds 20MB limit.")
            } catch (error: Exception) {
                AppBulletin.showError(this@ManageSyllabusActivity,
                    "Replace failed: ${error.localizedMessage}")
            } finally {
                if (generation == replacementGeneration) {
                    binding.progressBarTop.visibility = View.GONE
                }
            }
        }
    }

    private fun showReassignDialog(currentExam: Exam) {
        val targetExams = allExams.filter { it.id != currentExam.id }
        if (targetExams.isEmpty()) {
            AppBulletin.showError(this, "No other exams available to reassign.")
            return
        }

        val examLabels = targetExams.map { "${it.examName} (${it.categoryOrOther})" }.toTypedArray()
        var selectedIdx = 0

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Reassign Syllabus to Another Exam")
            .setSingleChoiceItems(examLabels, 0, dismissOnSelection = false) { _, which ->
                selectedIdx = which
            }
            .setPositiveButton("Reassign") { _, _ ->
                val target = targetExams.getOrNull(selectedIdx) ?: return@setPositiveButton
                binding.progressBarTop.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        // Transfer syllabus metadata to target exam
                        examRepo.updateExamFullSettings(
                            examId = target.id,
                            examName = target.examName,
                            testNumber = target.testNumber,
                            questionCount = target.questionCount,
                            autoGenEnabled = target.autoGenEnabled,
                            autoGenTime = target.autoGenTime,
                            syllabusUrl = currentExam.syllabusUrl,
                            syllabusFileName = currentExam.syllabusFileName,
                            generationPrompt = target.generationPrompt,
                            timeLimitMinutes = target.timeLimitMinutes,
                            negativeMarkingText = target.negativeMarkingText,
                            negativeMarkingValue = target.negativeMarkingValue,
                            parentExamId = target.parentExamId,
                            publishMode = target.publishMode
                        )
                        // Clear syllabus from previous exam
                        examRepo.updateExamFullSettings(
                            examId = currentExam.id,
                            examName = currentExam.examName,
                            testNumber = currentExam.testNumber,
                            questionCount = currentExam.questionCount,
                            autoGenEnabled = currentExam.autoGenEnabled,
                            autoGenTime = currentExam.autoGenTime,
                            syllabusUrl = "",
                            syllabusFileName = "",
                            generationPrompt = currentExam.generationPrompt,
                            timeLimitMinutes = currentExam.timeLimitMinutes,
                            negativeMarkingText = currentExam.negativeMarkingText,
                            negativeMarkingValue = currentExam.negativeMarkingValue,
                            parentExamId = currentExam.parentExamId,
                            publishMode = currentExam.publishMode
                        )
                        binding.progressBarTop.visibility = View.GONE
                        AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus reassigned to ${target.examName}")
                        loadExamsAndSyllabuses()
                    } catch (e: Exception) {
                        binding.progressBarTop.visibility = View.GONE
                        AppBulletin.showError(this@ManageSyllabusActivity, "Reassign failed: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeleteSyllabus(exam: Exam) {
        deleteFlow.confirmRemoval("syllabus:${exam.id}", "Syllabus", { examRepo.removeSyllabusPdf(exam.id, exam.syllabusUrl) }) { loadExamsAndSyllabuses() }
    }

}
