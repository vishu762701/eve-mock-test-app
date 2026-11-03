package com.eve.app.ui.admin

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.content.ActivityNotFoundException
import java.io.FilterInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
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
import com.eve.app.data.repository.AdminRepository
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.databinding.ActivityManageQuestionsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminIdentityGuard
import com.google.firebase.auth.FirebaseAuth
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.ExamImageHelper
import com.eve.app.util.QuestionImportHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import androidx.core.view.doOnLayout

class ManageQuestionsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageQuestionsBinding
    private val viewModel: AdminViewModel by viewModels()
    private val examRepo = ExamRepository()

    private var exams: List<Exam> = emptyList()
    private var selectedExamForImageUpdate: Exam? = null
    private var imageReadJob: Job? = null
    private var imageRequestGeneration = 0L
    private var pendingBulkOwner: QuestionBulkImportOrigin? = null
    private var pendingBulkUri: Uri? = null
    private var bulkReadJob: Job? = null
    private var cancelBulkRead: (() -> Unit)? = null
    private var bulkReadGeneration = 0L
    private var bulkPreviewDialog: TelegramNativeAlertDialog? = null
    private var bulkWriteInFlight = false
    private var adminScreenAuthorized = false
    private var restoredSelectedExam: String? = null
    private var interruptedBulkWrite = false
    private val bulkImportBusy: Boolean
        get() = pendingBulkOwner != null || bulkReadJob?.isActive == true ||
            bulkPreviewDialog?.isShowing == true || bulkWriteInFlight
    private var questionDialog: TelegramNativeAlertDialog? = null
    private var renameExamDialog: TelegramNativeAlertDialog? = null
    private val questionOwnerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val questionOwnerEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
    private val questionIdentityCurrent: Boolean
        get() {
            val live = FirebaseAuth.getInstance().currentUser
            return !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(true,
                questionOwnerUid, questionOwnerEmail, live?.uid, live?.email)
        }

    private fun ownsImageExam(exam: Exam): Boolean =
        adminScreenAuthorized && questionIdentityCurrent &&
            selectedExamForImageUpdate?.id == exam.id &&
            viewModel.questionExamId == exam.id &&
            exams.any { it.id == exam.id }

    private val pickEditExamImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val exam = selectedExamForImageUpdate ?: return@registerForActivityResult
        if (uri == null || !ownsImageExam(exam) || viewModel.busy.value) {
            selectedExamForImageUpdate = null
            return@registerForActivityResult
        }
        val generation = ++imageRequestGeneration
        imageReadJob?.cancel()
        imageReadJob = lifecycleScope.launch {
            try {
                val encoded = withContext(Dispatchers.IO) {
                    ExamImageHelper.uriToBase64(applicationContext, uri)
                }
                if (generation != imageRequestGeneration || !ownsImageExam(exam)) return@launch
                if (encoded == null) {
                    AppBulletin.showError(this@ManageQuestionsActivity, "Failed to load image")
                    return@launch
                }
                val adminRepo = AdminRepository()
                val grant = AdminPermissionSnapshot.load(
                    questionOwnerEmail, "manage_exams",
                    membership = { adminRepo.isAdminFresh(questionOwnerUid, questionOwnerEmail) },
                    permissions = { adminRepo.getAdminMe() }
                )
                if (generation != imageRequestGeneration || !ownsImageExam(exam)) return@launch
                if (grant == null) {
                    AppBulletin.showError(this@ManageQuestionsActivity, "Exam image permission is unavailable")
                    return@launch
                }
                // AdminViewModel and the Worker own the confirmed mutation.
                viewModel.updateExamImage(exam.id, encoded) {}
                selectedExamForImageUpdate = null
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (generation == imageRequestGeneration && ownsImageExam(exam)) {
                    AppBulletin.showError(this@ManageQuestionsActivity, "Failed to read exam image")
                }
            } finally {
                if (generation == imageRequestGeneration) {
                    imageReadJob = null
                    if (selectedExamForImageUpdate?.id == exam.id) selectedExamForImageUpdate = null
                }
            }
        }
    }

    private val pickBulkQuestionsLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val owner = pendingBulkOwner ?: return@registerForActivityResult
        val live = FirebaseAuth.getInstance().currentUser
        if (uri == null || !questionIdentityCurrent ||
            !owner.isCurrent(owner.examId, live?.uid, live?.email)) {
            clearPendingBulkImport()
            return@registerForActivityResult
        }
        pendingBulkUri = uri
        consumePendingBulkImport()
    }

    private val questionAdapter = QuestionManageAdapter(
        onEdit = { q -> showQuestionDetails(q) },
        onDelete = { q -> confirmDeleteQuestion(q) },
        canManage = { questionIdentityCurrent }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        restoredSelectedExam = savedInstanceState?.getString("questions:selected-exam")
        val restoredOwner = QuestionBulkImportOrigin(
            savedInstanceState?.getString("bulk:exam").orEmpty(),
            savedInstanceState?.getString("bulk:uid").orEmpty(),
            savedInstanceState?.getString("bulk:email").orEmpty()
        )
        if (restoredOwner.isCurrent(restoredOwner.examId, questionOwnerUid, questionOwnerEmail)) {
            pendingBulkOwner = restoredOwner
            pendingBulkUri = savedInstanceState?.getString("bulk:uri")?.let(Uri::parse)
        }
        interruptedBulkWrite = savedInstanceState?.getBoolean("bulk:write-in-flight") == true

        binding.telegramActionBar.setTitle("Manage Questions")
        binding.telegramActionBar.onBackPressed = { finish() }

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_questions") { permissions ->
            val canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
            for (control in listOf(binding.btnEditExamImage, binding.btnRenameExam, binding.btnDeleteExam)) {
                control.visibility = if (canManageExams) View.VISIBLE else View.GONE
            }
            adminScreenAuthorized = true
            setupUi()
            setupObservers()
            viewModel.loadExams()
            if (interruptedBulkWrite) {
                interruptedBulkWrite = false
                AppBulletin.showError(this, "Import result is unknown after interruption. Refresh questions before retrying.")
            }
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
            val exam = exams.getOrNull(binding.spExam.selectedItemPosition)
            if (exam == null || !questionIdentityCurrent || viewModel.busy.value ||
                viewModel.questionExamId != exam.id || bulkImportBusy) {
                if (exam == null) AppBulletin.showError(this, "Please select an exam first")
                return@setOnClickListener
            }
            pendingBulkOwner = QuestionBulkImportOrigin(exam.id, questionOwnerUid, questionOwnerEmail)
            pendingBulkUri = null
            updateBulkImportControls()
            try { pickBulkQuestionsLauncher.launch("*/*") }
            catch (_: ActivityNotFoundException) {
                clearPendingBulkImport()
                AppBulletin.showError(this, getString(R.string.exam_import_picker_unavailable))
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exams.collect { examList ->
                        exams = examList
                        // AdminViewModel starts with an empty list before the first network answer.
                        // Restore by stable ID when a nonempty authorized result arrives, not by position.
                        if (examList.isNotEmpty()) {
                            val restoredId = pendingBulkOwner?.examId ?: restoredSelectedExam
                            if (restoredId != null && examList.any { it.id == restoredId } &&
                                viewModel.questionExamId != restoredId) {
                                viewModel.loadQuestions(restoredId)
                            }
                            restoredSelectedExam = null
                        }
                        bindQuestionExamChoices(binding.spExam, examList, viewModel) { examId ->
                            if (pendingBulkOwner?.examId?.let { it != examId } == true) clearPendingBulkImport()
                        }
                        if (examList.isEmpty()) updateQuestionsUi(emptyList())
                        consumePendingBulkImport()
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
        if (isFinishing || isDestroyed || questionDialog?.isShowing == true) return
        val dialog = createQuestionEditorDialog(this, selectedExam, isBusy = { viewModel.busy.value }, submit = viewModel::addQuestion)
        questionDialog = dialog
        dialog.setOnDismissListener { if (questionDialog === dialog) questionDialog = null }
        dialog.show()
    }

    private fun showEditExamImageDialog() {
        if (!questionIdentityCurrent || !adminScreenAuthorized ||
            viewModel.busy.value || imageReadJob?.isActive == true) return
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
        fun targetCurrent() = questionIdentityCurrent && !viewModel.busy.value &&
            q.id.isNotBlank() && viewModel.questionExamId == q.examId &&
            viewModel.questions.value.any { it.id == q.id && it.examId == q.examId }
        if (!targetCurrent() || questionDialog?.isShowing == true) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(getString(R.string.question_delete_title))
            .setMessage(getString(R.string.question_delete_confirmation))
            .setPositiveButton(getString(R.string.question_delete_action)) { _, _ ->
                if (!targetCurrent()) return@setPositiveButton
                viewModel.deleteQuestion(q, onFailure = { message ->
                    if (questionIdentityCurrent) AppBulletin.showError(this, message)
                }) {
                    if (questionIdentityCurrent) AppBulletin.showSuccess(this, getString(R.string.question_deleted))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).create()
        questionDialog = dialog
        dialog.setOnDismissListener { if (questionDialog === dialog) questionDialog = null }
        dialog.show()
    }

    private fun isCurrentBulkTarget(origin: QuestionBulkImportOrigin): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return adminScreenAuthorized && questionIdentityCurrent &&
            origin.isCurrent(viewModel.questionExamId, user?.uid, user?.email) &&
            exams.any { it.id == origin.examId } &&
            exams.getOrNull(binding.spExam.selectedItemPosition)?.id == origin.examId
    }

    private fun updateBulkImportControls() {
        if (!::binding.isInitialized) return
        binding.btnBulkUploadSelectedExam.isEnabled =
            adminScreenAuthorized && questionIdentityCurrent && !bulkImportBusy && !viewModel.busy.value
    }

    private fun clearPendingBulkImport() {
        ++bulkReadGeneration
        cancelBulkRead?.invoke()
        cancelBulkRead = null
        bulkReadJob?.cancel()
        bulkReadJob = null
        pendingBulkOwner = null
        pendingBulkUri = null
        val dialog = bulkPreviewDialog
        bulkPreviewDialog = null
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        updateBulkImportControls()
    }

    private fun consumePendingBulkImport() {
        if (!adminScreenAuthorized || !questionIdentityCurrent || exams.isEmpty() ||
            bulkWriteInFlight || bulkReadJob != null || bulkPreviewDialog != null) return
        val owner = pendingBulkOwner ?: return
        val uri = pendingBulkUri ?: return
        if (!isCurrentBulkTarget(owner)) {
            clearPendingBulkImport()
            AppBulletin.showError(this, "Selected exam changed. Please choose the file again.")
            return
        }
        val exam = exams.first { it.id == owner.examId }
        val token = ++bulkReadGeneration
        val signal = CancellationSignal()
        val descriptor = AtomicReference<android.content.res.AssetFileDescriptor?>()
        cancelBulkRead = {
            signal.cancel()
            runCatching { descriptor.getAndSet(null)?.close() }
        }
        bulkReadJob = lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val ioContext = kotlin.coroutines.coroutineContext
                    ioContext.ensureActive()
                    val fileName = getFileName(uri, signal) ?: "questions.csv"
                    val asset = contentResolver.openAssetFileDescriptor(uri, "r", signal)
                        ?: throw IOException("Selected document unavailable")
                    val parsed = asset.use { opened ->
                        descriptor.set(opened)
                        try {
                            opened.createInputStream().use { stream ->
                                val cancellable = object : FilterInputStream(stream) {
                                    override fun read(): Int {
                                        ioContext.ensureActive()
                                        return super.read()
                                    }
                                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                                        ioContext.ensureActive()
                                        return super.read(b, off, len)
                                    }
                                }
                                QuestionImportHelper.parseQuestions(cancellable, fileName, owner.examId)
                            }
                        } finally { descriptor.compareAndSet(opened, null) }
                    }
                    fileName to parsed
                }
                if (token == bulkReadGeneration && pendingBulkUri == uri &&
                    isCurrentBulkTarget(owner) &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    showBulkImportPreviewDialog(result.first, exam, result.second)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                if (token == bulkReadGeneration && questionIdentityCurrent) {
                    clearPendingBulkImport()
                    AppBulletin.showError(this@ManageQuestionsActivity, getString(R.string.exam_import_read_failed))
                }
            } finally {
                if (token == bulkReadGeneration) {
                    bulkReadJob = null
                    cancelBulkRead = null
                    updateBulkImportControls()
                }
            }
        }
        updateBulkImportControls()
    }

    override fun onStart() {
        super.onStart()
        if (adminScreenAuthorized) consumePendingBulkImport()
    }

    private fun showBulkImportPreviewDialog(
        fileName: String,
        selectedExam: Exam,
        result: QuestionImportHelper.ImportResult
    ) {
        val owner = pendingBulkOwner ?: return
        if (owner.examId != selectedExam.id || !isCurrentBulkTarget(owner) ||
            bulkPreviewDialog != null || questionDialog?.isShowing == true) return
        val validCount = result.validQuestions.size
        val errorCount = result.errors.size

        val message = buildString {
            append("Exam: ${selectedExam.examName}\n")
            append("File: $fileName\n\n")
            append("✅ $validCount questions ready to import.\n")
            if (errorCount > 0) {
                append("⚠️ $errorCount rows skipped due to errors:\n\n")
                result.errors.take(8).forEach { err ->
                    val reason = result.failure?.let { getString(it.messageRes) } ?: err.reason
                    append("• Row ${err.rowNumber}: $reason\n")
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
                if (!bulkWriteInFlight && isCurrentBulkTarget(owner)) {
                    importQuestionsForSelectedExam(selectedExam.id, result.validQuestions)
                }
            }
            builder.setNegativeButton("Cancel", null)
        } else {
            builder.setPositiveButton("OK", null)
        }

        val dialog = builder.create()
        bulkPreviewDialog = dialog
        dialog.setOnDismissListener {
            if (bulkPreviewDialog === dialog) {
                bulkPreviewDialog = null
                if (!bulkWriteInFlight) clearPendingBulkImport()
            }
        }
        dialog.show()
        updateBulkImportControls()
    }

    private fun importQuestionsForSelectedExam(examId: String, questions: List<Question>) {
        val owner = pendingBulkOwner ?: return
        if (bulkWriteInFlight || !isCurrentBulkTarget(owner) || viewModel.busy.value ||
            owner.examId != examId || questions.isEmpty() ||
            questions.any { it.examId != examId }) return
        bulkWriteInFlight = true
        pendingBulkOwner = null
        pendingBulkUri = null
        updateBulkImportControls()
        lifecycleScope.launch {
            try {
                val adminRepo = AdminRepository()
                val rights = AdminPermissionSnapshot.load(
                    questionOwnerEmail, "manage_questions",
                    membership = { adminRepo.isAdminFresh(questionOwnerUid, questionOwnerEmail) },
                    permissions = { adminRepo.getAdminMe() }
                )
                if (rights == null) throw SecurityException("Permission no longer allows question imports")
                if (!questionIdentityCurrent || viewModel.questionExamId != examId) return@launch
                val added = examRepo.addQuestions(questions)
                if (questionIdentityCurrent) {
                    AppBulletin.showSuccess(this@ManageQuestionsActivity, "Successfully imported " + added + " questions!")
                    viewModel.refreshQuestions(examId)
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                if (questionIdentityCurrent) {
                    AppBulletin.showError(this@ManageQuestionsActivity,
                        "Import was not confirmed: " + (e.message ?: "Unknown error"))
                }
            } finally {
                bulkWriteInFlight = false
                updateBulkImportControls()
            }
        }
    }

    private fun getFileName(uri: Uri, signal: CancellationSignal): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null, signal)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("questions:selected-exam", viewModel.questionExamId)
        pendingBulkOwner?.let {
            outState.putString("bulk:exam", it.examId)
            outState.putString("bulk:uid", it.uid)
            outState.putString("bulk:email", it.email)
        }
        pendingBulkUri?.let { outState.putString("bulk:uri", it.toString()) }
        outState.putBoolean("bulk:write-in-flight", bulkWriteInFlight)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        ++imageRequestGeneration
        imageReadJob?.cancel()
        imageReadJob = null
        clearPendingBulkImport()
        renameExamDialog?.dismiss()
        renameExamDialog = null
        questionDialog?.dismiss()
        questionDialog = null
        super.onDestroy()
    }

}

/** The single production spinner binding; identity survives adapter rebuilds and reordering. */
internal fun bindQuestionExamChoices(spinner: android.widget.Spinner, exams: List<Exam>,
                                    viewModel: AdminViewModel,
                                    onSelectionChanged: ((String) -> Unit)? = null) {
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
            if (exam.id != viewModel.questionExamId) {
                onSelectionChanged?.invoke(exam.id)
                viewModel.loadQuestions(exam.id)
            }
        }
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }
}

/** Canonical question form presentation; authorization stays at the Activity/Worker entry points. */
internal fun createQuestionEditorDialog(activity: android.app.Activity, selectedExam: Exam,
                                     isBusy: () -> Boolean = { false },
                                     title: CharSequence = "Add Question to ${selectedExam.examName}",
                                     initialQuestion: Question? = null,
                                     positiveLabel: CharSequence = "Add",
                                     submittingLabel: Int = R.string.question_form_submitting,
                                     successMessage: String = "Question added successfully!",
                                     neutralLabel: CharSequence? = null,
                                     onNeutral: (() -> Unit)? = null,
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

        initialQuestion?.let { question ->
            etQuestion.setText(question.questionText)
            etOptA.setText(question.optionA); etOptB.setText(question.optionB)
            etOptC.setText(question.optionC); etOptD.setText(question.optionD)
            etExplanation.setText(question.explanation); etTopic.setText(question.topic)
            when (question.correctAnswer.uppercase(java.util.Locale.ROOT)) {
                "A" -> rgCorrect.check(R.id.rbA); "B" -> rgCorrect.check(R.id.rbB)
                "C" -> rgCorrect.check(R.id.rbC); "D" -> rgCorrect.check(R.id.rbD)
                else -> rgCorrect.clearCheck()
            }
        }
        dialog = TelegramNativeAlertDialog.Builder(activity)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(positiveLabel) { _, _ ->
                if (submitting || isBusy() || activity.isFinishing || activity.isDestroyed || !dialog.isShowing) return@setPositiveButton
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

                val newQuestion = (initialQuestion ?: Question(id = "", examId = selectedExam.id)).copy(
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
                addButton?.setText(submittingLabel)
                dialog.getActionButton(android.content.DialogInterface.BUTTON_NEGATIVE)?.isEnabled = false
                dialog.getActionButton(android.content.DialogInterface.BUTTON_NEUTRAL)?.isEnabled = false
                dialog.setCancelable(false)
                submit(newQuestion, { message ->
                    submitting = false
                    if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                        addButton?.text = addLabel
                        addButton?.isEnabled = true
                        dialog.getActionButton(android.content.DialogInterface.BUTTON_NEGATIVE)?.isEnabled = true
                        dialog.getActionButton(android.content.DialogInterface.BUTTON_NEUTRAL)?.isEnabled = true
                        dialog.setCancelable(true)
                        showFormError(message)
                    }
                }) {
                    if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                        dialog.dismiss()
                        AppBulletin.showSuccess(activity, successMessage)
                    }
                }
            }
            .setNegativeButton("Cancel") { owner, _ -> if (!submitting) owner.dismiss() }
            .apply {
                if (neutralLabel != null && onNeutral != null) {
                    setNeutralButton(neutralLabel) { owner, _ ->
                        if (!submitting) { owner.dismiss(); onNeutral() }
                    }
                }
            }
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

/** Captured picker request identity; not a second authorization authority. */
internal data class QuestionBulkImportOrigin(val examId: String, val uid: String, val email: String) {
    fun isCurrent(selectedExamId: String?, currentUid: String?, currentEmail: String?): Boolean =
        examId.isNotBlank() && examId == selectedExamId &&
            AdminIdentityGuard.allowed(true, uid, email, currentUid, currentEmail)
}
