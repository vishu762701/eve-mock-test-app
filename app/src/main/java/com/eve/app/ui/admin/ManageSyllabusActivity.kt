package com.eve.app.ui.admin

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
import com.eve.app.util.AppBulletin
import com.eve.app.util.SecurityHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
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
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityManageSyllabusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupAddSyllabusForm()
        setupSearch()

        loadExamsAndSyllabuses()
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
            selectedPdfUri = null
            selectedPdfName = ""
            selectedPdfBytes = null
            binding.layoutSelectedPdf.visibility = View.GONE
            binding.btnChoosePdf.visibility = View.VISIBLE
        }

        binding.btnUploadSyllabus.setOnClickListener {
            val exam = selectedExamForNewSyllabus
            if (exam == null) {
                binding.tilSelectExam.error = "Please select an exam first"
                return@setOnClickListener
            }
            binding.tilSelectExam.error = null

            val bytes = selectedPdfBytes
            val fileName = selectedPdfName.ifBlank { "syllabus.pdf" }
            if (bytes == null || bytes.isEmpty()) {
                AppBulletin.showError(this, "Please choose a valid PDF file to upload.")
                return@setOnClickListener
            }

            binding.progressBarUpload.visibility = View.VISIBLE
            binding.btnUploadSyllabus.isEnabled = false

            lifecycleScope.launch {
                try {
                    examRepo.uploadSyllabusPdf(exam.id, fileName, bytes)
                    binding.progressBarUpload.visibility = View.GONE
                    binding.btnUploadSyllabus.isEnabled = true

                    // Reset form
                    selectedPdfUri = null
                    selectedPdfName = ""
                    selectedPdfBytes = null
                    binding.layoutSelectedPdf.visibility = View.GONE
                    binding.btnChoosePdf.visibility = View.VISIBLE
                    binding.actvSelectExam.setText("", false)
                    selectedExamForNewSyllabus = null

                    AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus uploaded and linked to ${exam.examName}")
                    loadExamsAndSyllabuses()
                } catch (e: Exception) {
                    binding.progressBarUpload.visibility = View.GONE
                    binding.btnUploadSyllabus.isEnabled = true
                    AppBulletin.showError(this@ManageSyllabusActivity, "Upload failed: ${e.localizedMessage}")
                }
            }
        }
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
        val dropdownItems = allExams.map { "${it.examName} (${it.categoryOrOther})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, dropdownItems)
        binding.actvSelectExam.setAdapter(adapter)

        binding.actvSelectExam.setOnItemClickListener { _, _, position, _ ->
            selectedExamForNewSyllabus = allExams.getOrNull(position)
            binding.tilSelectExam.error = null
        }
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

    private fun handleNewPdfSelected(uri: Uri) {
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

            selectedPdfUri = uri
            selectedPdfName = displayName
            selectedPdfBytes = bytes

            binding.tvNewPdfName.text = displayName
            binding.layoutSelectedPdf.visibility = View.VISIBLE
            binding.btnChoosePdf.visibility = View.GONE
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to read PDF file: ${e.localizedMessage}")
        }
    }

    private fun handleReplacePdfSelected(exam: Exam, uri: Uri) {
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

            binding.progressBarTop.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    examRepo.uploadSyllabusPdf(exam.id, displayName, bytes)
                    binding.progressBarTop.visibility = View.GONE
                    AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus updated for ${exam.examName}")
                    loadExamsAndSyllabuses()
                } catch (e: Exception) {
                    binding.progressBarTop.visibility = View.GONE
                    AppBulletin.showError(this@ManageSyllabusActivity, "Replace failed: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to read PDF file: ${e.localizedMessage}")
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

        MaterialAlertDialogBuilder(this)
            .setTitle("Reassign Syllabus to Another Exam")
            .setSingleChoiceItems(examLabels, 0) { _, which ->
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

    private fun confirmDeleteSyllabus(exam: Exam) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Syllabus?")
            .setMessage("Are you sure you want to permanently delete the syllabus for \"${exam.examName}\"? Students will no longer be able to view or download it.")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBarTop.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        examRepo.removeSyllabusPdf(exam.id, exam.syllabusUrl)
                        binding.progressBarTop.visibility = View.GONE
                        AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus removed")
                        loadExamsAndSyllabuses()
                    } catch (e: Exception) {
                        binding.progressBarTop.visibility = View.GONE
                        AppBulletin.showError(this@ManageSyllabusActivity, "Delete failed: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class AdminSyllabusAdapter(
        private val onReplace: (Exam) -> Unit,
        private val onReassign: (Exam) -> Unit,
        private val onDelete: (Exam) -> Unit
    ) : RecyclerView.Adapter<AdminSyllabusAdapter.VH>() {

        private var items: List<Exam> = emptyList()

        fun submit(newItems: List<Exam>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemAdminSyllabusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class VH(private val b: ItemAdminSyllabusBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(exam: Exam) {
                b.tvExamName.text = exam.examName
                b.tvExamCategory.text = "Category: ${exam.categoryOrOther}"
                b.tvFileName.text = exam.syllabusFileName.ifBlank { "syllabus.pdf" }

                b.tvUploadDate.text = if (exam.syllabusUploadedAt > 0) {
                    "Uploaded: ${dateFormat.format(Date(exam.syllabusUploadedAt))}"
                } else {
                    "Active Syllabus"
                }

                b.btnReplacePdf.setOnClickListener { onReplace(exam) }
                b.btnChangeExam.setOnClickListener { onReassign(exam) }
                b.btnDeletePdf.setOnClickListener { onDelete(exam) }
            }
        }
    }
}
