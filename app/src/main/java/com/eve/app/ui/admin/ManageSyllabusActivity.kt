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
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.data.repository.AdminRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import java.io.IOException
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
    private val adminRepo = AdminRepository()
    private var accessUid = ""
    private var accessEmail = ""
    private var screenStopSeen = false
    private var reauthorizeOnResume = false
    private var examListGeneration = 0L
    private var pendingNewPdfOwner: String? = null
    private var pendingReplaceOwner: String? = null
    private var reassignDialog: TelegramNativeAlertDialog? = null

    private fun currentOwner(): Boolean {
        val live = FirebaseAuth.getInstance().currentUser
        return adminAccessGranted && !isFinishing && !isDestroyed &&
            AdminIdentityGuard.allowed(true, accessUid, accessEmail, live?.uid, live?.email)
    }

    private fun activeOwner(): Boolean =
        currentOwner() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private suspend fun requireSyllabusRight() {
        requireCurrentSyllabusRight(
            expectedEmail = accessEmail,
            ownerCurrent = { activeOwner() },
            membership = { adminRepo.isAdminFresh(accessUid, accessEmail) },
            permissions = { adminRepo.getAdminMe() }
        )
    }

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
    private var reassignGeneration = 0L
    private var reassignInFlight = false

    private var targetExamForReplace: Exam? = null
    private val replacePdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val original = targetExamForReplace
        val owner = pendingReplaceOwner
        targetExamForReplace = null
        pendingReplaceOwner = null
        if (uri != null && owner == accessUid && activeOwner() && original != null &&
            allExams.any { it.id == original.id }) {
            handleReplacePdfSelected(original, uri)
        }
    }

    private val newPdfPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val owner = pendingNewPdfOwner
        pendingNewPdfOwner = null
        if (uri != null && owner == accessUid && activeOwner()) {
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

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_syllabus") {
            val principal = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            accessUid = principal.uid
            accessEmail = principal.email.orEmpty()
            adminAccessGranted = true
            if (!currentOwner()) return@requireAdmin
            setupRecyclerView()
            setupAddSyllabusForm()
            setupSearch()
            loadExamsAndSyllabuses()
        }
    }

    override fun onStop() {
        reassignDialog?.dismiss()
        reassignDialog = null
        screenStopSeen = true
        reauthorizeOnResume = true
        ++examListGeneration
        ++newPdfGeneration
        ++reassignGeneration
        newPdfReadJob?.cancel()
        newPdfReadJob = null
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (adminAccessGranted && !currentOwner()) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != accessUid || live.email != accessEmail) {
                finish()
                return
            }
            AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_syllabus"
            ) {
                if (!currentOwner()) {
                    finish()
                    return@requireAdmin
                }
                accessUid = live.uid
                accessEmail = live.email.orEmpty()
                if (screenStopSeen) {
                    screenStopSeen = false
                    // Never resend a possibly acknowledged mutation. Refresh the actual server state.
                    if (!uploadInFlight) {
                        binding.progressBarUpload.visibility = View.GONE
                        binding.btnChoosePdf.isEnabled = true
                        binding.btnClearSelectedPdf.isEnabled = true
                        binding.btnUploadSyllabus.isEnabled = true
                        uploadSucceeded = false
                        uploadFailed = false
                        updateUploadStepper()
                    }
                    loadExamsAndSyllabuses()
                }
            }
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
                if (activeOwner() && !uploadInFlight && replacementJob?.isActive != true &&
                    exam.id.isNotBlank() && allExams.any { it.id == exam.id }) {
                    targetExamForReplace = exam
                    pendingReplaceOwner = accessUid
                    try {
                        replacePdfPicker.launch(arrayOf("application/pdf"))
                    } catch (_: android.content.ActivityNotFoundException) {
                        targetExamForReplace = null
                        pendingReplaceOwner = null
                        AppBulletin.showError(this, "PDF picker is unavailable")
                    }
                }
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
            if (!activeOwner() || uploadInFlight || newPdfReadJob?.isActive == true) return@setOnClickListener
            pendingNewPdfOwner = accessUid
            try {
                newPdfPicker.launch(arrayOf("application/pdf"))
            } catch (_: android.content.ActivityNotFoundException) {
                pendingNewPdfOwner = null
                AppBulletin.showError(this, "PDF picker is unavailable")
            }
        }

        binding.btnClearSelectedPdf.setOnClickListener {
            if (!activeOwner() || uploadInFlight) return@setOnClickListener
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
            if (!activeOwner() || uploadInFlight || replacementJob?.isActive == true) return@setOnClickListener
            val exam = selectedExamForNewSyllabus
            if (exam == null || exam.id.isBlank() || allExams.none { it.id == exam.id }) {
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
                    requireSyllabusRight()
                    if (!activeOwner() || selectedExamForNewSyllabus?.id != exam.id ||
                        allExams.none { it.id == exam.id }) return@launch
                    val url = examRepo.uploadSyllabusPdf(exam.id, fileName, bytes)
                    if (!activeOwner()) return@launch
                    if (url.isBlank()) throw IllegalStateException("Server did not confirm syllabus URL")
                    uploadSucceeded = true
                    updateUploadStepper()
                    binding.progressBarUpload.visibility = View.GONE
                    binding.btnUploadSyllabus.isEnabled = true
                    binding.fileUploadView.setUploadSuccess {
                        if (activeOwner()) {
                            resetSyllabusUploadForm()
                            AppBulletin.showSuccess(this@ManageSyllabusActivity, "Syllabus uploaded and linked to ${exam.examName}")
                            loadExamsAndSyllabuses()
                        }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (e: Exception) {
                    if (activeOwner()) {
                        uploadFailed = true
                        binding.progressBarUpload.visibility = View.GONE
                        binding.btnUploadSyllabus.isEnabled = true
                        binding.fileUploadView.setUploadFailed()
                        AppBulletin.showError(this@ManageSyllabusActivity, "Upload failed: ${e.localizedMessage}")
                    }
                } finally {
                    uploadInFlight = false
                    if (activeOwner()) {
                        binding.btnChoosePdf.isEnabled = true
                        binding.btnClearSelectedPdf.isEnabled = true
                        updateUploadStepper()
                    }
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
        if (!currentOwner()) return
        val request = ++examListGeneration
        binding.progressBarTop.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                requireSyllabusRight()
                if (!activeOwner() || request != examListGeneration) return@launch
                val fetched = examRepo.getExams()
                if (!activeOwner() || request != examListGeneration) return@launch
                allExams = fetched
                val selectedId = selectedExamForNewSyllabus?.id
                if (selectedId != null && fetched.none { it.id == selectedId }) {
                    selectedExamForNewSyllabus = null
                }
                updateExamDropdown()
                filterSyllabuses(binding.etSearch.text?.toString().orEmpty())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (activeOwner() && request == examListGeneration) {
                    AppBulletin.showError(this@ManageSyllabusActivity,
                        "Failed to load exams: ${e.localizedMessage}")
                }
            } finally {
                if (activeOwner() && request == examListGeneration) {
                    binding.progressBarTop.visibility = View.GONE
                }
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
        if (!activeOwner() || uploadInFlight) return
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
                val (name, bytes) = withContext(Dispatchers.IO) {
                    safePdfDisplayName(uri) to readPdfWithinLimit(uri)
                }
                if (!activeOwner() || generation != newPdfGeneration) return@launch
                selectedPdfUri = uri
                selectedPdfName = name
                selectedPdfBytes = bytes
                binding.fileUploadView.bindSelectedFile(name, bytes.size.toLong())
                updateUploadStepper()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (tooLarge: PdfSelectionReader.FileTooLargeException) {
                if (activeOwner() && generation == newPdfGeneration) {
                    AppBulletin.showError(this@ManageSyllabusActivity, "PDF exceeds 20MB limit.")
                }
            } catch (error: Exception) {
                if (activeOwner() && generation == newPdfGeneration) {
                    AppBulletin.showError(this@ManageSyllabusActivity,
                        "Failed to read PDF file: ${error.localizedMessage}")
                }
            } finally {
                if (generation == newPdfGeneration && activeOwner()) {
                    binding.btnChoosePdf.isEnabled = true
                    binding.btnUploadSyllabus.isEnabled = true
                    updateUploadStepper()
                }
            }
        }
    }

    private fun handleReplacePdfSelected(exam: Exam, uri: Uri) {
        if (!activeOwner() || uploadInFlight || replacementJob?.isActive == true ||
            exam.id.isBlank() || allExams.none { it.id == exam.id }) return
        val generation = ++replacementGeneration
        binding.progressBarTop.visibility = View.VISIBLE

        replacementJob = lifecycleScope.launch {
            try {
                val (fileName, bytes) = withContext(Dispatchers.IO) {
                    safePdfDisplayName(uri) to readPdfWithinLimit(uri)
                }
                if (!activeOwner() || generation != replacementGeneration ||
                    allExams.none { it.id == exam.id }) return@launch
                requireSyllabusRight()
                if (!activeOwner() || generation != replacementGeneration ||
                    allExams.none { it.id == exam.id }) return@launch
                val url = examRepo.uploadSyllabusPdf(exam.id, fileName, bytes)
                if (!activeOwner() || generation != replacementGeneration) return@launch
                if (url.isBlank()) throw IllegalStateException("Server did not confirm syllabus URL")
                AppBulletin.showSuccess(this@ManageSyllabusActivity,
                    "Syllabus updated for ${exam.examName}")
                loadExamsAndSyllabuses()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (tooLarge: PdfSelectionReader.FileTooLargeException) {
                if (activeOwner() && generation == replacementGeneration) {
                    AppBulletin.showError(this@ManageSyllabusActivity, "PDF exceeds 20MB limit.")
                }
            } catch (error: Exception) {
                if (activeOwner() && generation == replacementGeneration) {
                    AppBulletin.showError(this@ManageSyllabusActivity,
                        "Replace failed: ${error.localizedMessage}")
                }
            } finally {
                if (activeOwner() && generation == replacementGeneration) {
                    binding.progressBarTop.visibility = View.GONE
                }
            }
        }
    }


    private fun showReassignDialog(currentExam: Exam) {
        if (!activeOwner() || reassignInFlight || currentExam.id.isBlank() ||
            currentExam.syllabusUrl.isBlank() ||
            allExams.none { it.id == currentExam.id && it.syllabusUrl == currentExam.syllabusUrl }) return
        // Prevent orphaning the target's previous PDF with an unconfirmed overwrite.
        val targetExams = allExams.filter {
            it.id != currentExam.id && it.syllabusUrl.isBlank() && it.syllabusFileName.isBlank()
        }
        if (targetExams.isEmpty()) {
            AppBulletin.showError(this, "No other exam without a syllabus is available.")
            return
        }
        val ownerUid = accessUid
        val examLabels = targetExams.map { it.examName + " (" + it.categoryOrOther + ")" }.toTypedArray()
        var selectedIndex = 0
        reassignDialog?.dismiss()
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Reassign Syllabus to Another Exam")
            .setSingleChoiceItems(examLabels, 0, dismissOnSelection = false) { _, index ->
                selectedIndex = index
            }
            .setPositiveButton("Reassign") { _, _ ->
                val target = targetExams.getOrNull(selectedIndex) ?: return@setPositiveButton
                if (!activeOwner() || ownerUid != accessUid || reassignInFlight ||
                    allExams.none { it.id == target.id && it.syllabusUrl.isBlank() }) return@setPositiveButton
                val generation = ++reassignGeneration
                fun current() = activeOwner() && accessUid == ownerUid &&
                    generation == reassignGeneration
                reassignInFlight = true
                binding.progressBarTop.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        requireSyllabusRight()
                        if (!current()) return@launch
                        examRepo.reassignSyllabus(currentExam, target)
                        if (!current()) return@launch
                        AppBulletin.showSuccess(this@ManageSyllabusActivity,
                            "Syllabus reassigned to " + target.examName)
                        loadExamsAndSyllabuses()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        if (current()) {
                            AppBulletin.showError(this@ManageSyllabusActivity,
                                "Reassign unconfirmed: " + error.localizedMessage + ". Reload before retrying.")
                            loadExamsAndSyllabuses()
                        }
                    } finally {
                        reassignInFlight = false
                        if (current()) binding.progressBarTop.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        reassignDialog = dialog
        dialog.setOnDismissListener { if (reassignDialog === dialog) reassignDialog = null }
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeleteSyllabus(exam: Exam) {
        if (!activeOwner() || exam.id.isBlank() || allExams.none { it.id == exam.id }) return
        deleteFlow.confirmRemoval("syllabus:${exam.id}", "Syllabus", {
            requireSyllabusRight()
            if (!activeOwner() || allExams.none { it.id == exam.id }) {
                throw CancellationException("Syllabus owner or target changed")
            }
            val notice = examRepo.removeSyllabusPdf(exam.id, exam.syllabusUrl)
            if (!activeOwner()) throw CancellationException("Syllabus owner changed after removal")
            notice
        }) {
            if (activeOwner()) loadExamsAndSyllabuses()
        }
    }

    override fun onDestroy() {
        reassignDialog?.dismiss()
        reassignDialog = null
        newPdfReadJob?.cancel()
        newPdfReadJob = null
        super.onDestroy()
    }

}

/**
 * This is an await-boundary for the existing AdminPermissionSnapshot, not
 * a second authorization owner. Worker requirePermission remains authoritative.
 */
internal suspend fun requireCurrentSyllabusRight(
    expectedEmail: String,
    ownerCurrent: () -> Boolean,
    membership: suspend () -> Boolean,
    permissions: suspend () -> com.eve.app.data.remote.AdminMeData
) {
    if (!ownerCurrent()) throw CancellationException("Syllabus owner changed")
    val right = AdminPermissionSnapshot.load(
        expectedEmail, "manage_syllabus", membership, permissions
    )
    if (!ownerCurrent()) throw CancellationException("Syllabus owner changed")
    if (right == null) throw SecurityException("Syllabus management permission is unavailable")
}
