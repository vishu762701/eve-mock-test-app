package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import android.content.DialogInterface

import com.eve.app.ui.common.EveBaseActivity

import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
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
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.designsystem.TelegramRadialProgressView
import com.google.android.material.button.MaterialButton

class EditExamActivity : EveBaseActivity() {

    private lateinit var binding: ActivityEditExamBinding
    private val examRepo = ExamRepository()
    private val auditLogRepo = AuditLogRepository()

    private var examId: String = ""
    private var examName: String = ""
    private var examCategory: String = ""
    private var timeLimit: Int = 30

    // Origin metadata for this existing editor; the Worker/gate still own permissions.
    private var editorOwnerUid = ""
    private var editorOwnerEmail = ""
    private val editorIdentityCurrent: Boolean
        get() {
            val live = FirebaseAuth.getInstance().currentUser
            return !isFinishing && !isDestroyed && AdminIdentityGuard.allowed(true,
                editorOwnerUid, editorOwnerEmail, live?.uid, live?.email)
        }
    private var metadataSaveInFlight = false
    private var metadataViewStateRestored = true
    private var restoreMetadataDraft = false
    private var restoredMetadataCategory: String? = null
    private var currentExam: Exam? = null
    private var currentQuestions: MutableList<Question> = mutableListOf()

    private val adminRepo = com.eve.app.data.repository.AdminRepository()
    private var pendingQuestionDeleteId: String? = null
    private var deletingQuestion = false
    private var questionDeleteEpoch = 0L
    private var questionReadGeneration = 0L
    private var reloadQuestionsAfterStop = false
    private var reauthorizeOnResume = false
    private val questionDeleteBusy: Boolean get() = pendingQuestionDeleteId != null || deletingQuestion

    private var canManageQuestions = false
    private var canManageContent = false
    private var questionDialog: TelegramNativeAlertDialog? = null
    private data class ParsedImport(val uri: Uri, val fileName: String, val result: QuestionImportHelper.ImportResult)
    private var awaitingPickerResult = false
    private var pendingImportUri: Uri? = null
    private var parsedImport: ParsedImport? = null
    private var importReadJob: Job? = null
    private var cancelImportRead: (() -> Unit)? = null
    private var importReadGeneration = 0L
    private var closingEditor = false
    private var importingQuestions = false
    private var interruptedImportNotice = false
    private val questionWindowBusy: Boolean
        get() = questionDeleteBusy || awaitingPickerResult || questionDialog?.isShowing == true || importReadJob?.isActive == true || importingQuestions

    private val questionActionsAllowed: Boolean
        get() = canManageQuestions && editorIdentityCurrent

    private val questionAdapter = QuestionManageAdapter(
        onEdit = { question -> showQuestionDetails(question) },
        onDelete = { question -> confirmDeleteQuestion(question) },
        canManage = { questionActionsAllowed && !questionDeleteBusy }
    )

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (!awaitingPickerResult) return@registerForActivityResult
        awaitingPickerResult = false
        updateImportControls()
        if (uri != null && editorIdentityCurrent && !importingQuestions) {
            if (pendingImportUri != uri) {
                importReadGeneration++
                cancelImportRead?.invoke()
                importReadJob?.cancel()
                importReadJob = null
                parsedImport = null
                pendingImportUri = uri
            }
        }
        consumePendingImport()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val origin = FirebaseAuth.getInstance().currentUser
        editorOwnerUid = origin?.uid.orEmpty()
        editorOwnerEmail = origin?.email.orEmpty()
        binding = ActivityEditExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        metadataViewStateRestored = savedInstanceState == null
        restoreMetadataDraft = savedInstanceState?.getBoolean("edit_exam_metadata_ready") == true
        restoredMetadataCategory = savedInstanceState?.getString("edit_exam_metadata_category")
        setMetadataEditingEnabled(false)
        awaitingPickerResult = savedInstanceState?.getBoolean("edit_exam_picker_pending") == true
        pendingImportUri = savedInstanceState?.getString("edit_exam_pending_import")?.let(Uri::parse)
        interruptedImportNotice = savedInstanceState?.getBoolean("edit_exam_import_in_flight") == true

        binding.btnBack.setOnClickListener { finish() }
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollEditContent.visibility = View.GONE

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_exams",
            restorationScope = "edit_exam:" + intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty(),
            isRestoring = savedInstanceState != null) { permissions ->
        if (!editorIdentityCurrent) { finish(); return@requireAdmin }
        binding.progressBar.visibility = View.GONE
        binding.scrollEditContent.visibility = View.VISIBLE
        canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
        canManageContent = permissions.isOwner || "manage_content" in permissions.rights
        if (!canManageQuestions) { pendingImportUri = null; parsedImport = null }
        binding.btnAddQuestion.visibility = if (canManageQuestions) View.VISIBLE else View.GONE
        binding.btnBulkUpload.visibility = if (canManageQuestions) View.VISIBLE else View.GONE
        binding.btnPickEmoji.visibility = if (canManageContent) View.VISIBLE else View.GONE

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()
        examCategory = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY).orEmpty()
        timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 30)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        setupCategorySpinner()
        if (!restoreMetadataDraft) setupForm()

        binding.btnSaveExamDetails.setOnClickListener { saveExamDetails() }
        binding.btnPickEmoji.setOnClickListener {
            if (!canManageContent || !editorIdentityCurrent) return@setOnClickListener
            startActivity(android.content.Intent(this, com.eve.app.ui.admin.emoji.EmojiStudioActivity::class.java))
        }
        binding.btnAddQuestion.setOnClickListener { showAddQuestionDialog() }
        binding.btnBulkUpload.setOnClickListener {
            if (!questionActionsAllowed || questionWindowBusy) return@setOnClickListener
            awaitingPickerResult = true
            updateImportControls()
            try { filePicker.launch("*/*") }
            catch (_: android.content.ActivityNotFoundException) {
                awaitingPickerResult = false
                updateImportControls()
                AppBulletin.showError(this, getString(R.string.exam_import_picker_unavailable))
            }
        }

        loadExamData()
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Constants.CATEGORIES.toMutableList()
        )
        binding.spCategory.adapter = adapter
        restoredMetadataCategory?.let { selectMetadataCategory(it) }
    }

    private fun setupForm() {
        binding.etExamName.setText(examName)
        binding.etExamMinutes.setText(timeLimit.toString())

        selectMetadataCategory(examCategory, allowExisting = currentExam != null)
    }

    private fun selectMetadataCategory(category: String, allowExisting: Boolean = true) {
        @Suppress("UNCHECKED_CAST")
        val adapter = binding.spCategory.adapter as? ArrayAdapter<String> ?: return
        var index = (0 until adapter.count).firstOrNull { adapter.getItem(it).equals(category, ignoreCase = true) } ?: -1
        if (index < 0 && allowExisting && category.isNotBlank()) {
            adapter.add(category)
            index = adapter.count - 1
        }
        if (index >= 0) binding.spCategory.setSelection(index)
    }

    private fun setMetadataEditingEnabled(enabled: Boolean) {
        for (view in listOf(binding.etExamName, binding.etExamMinutes, binding.spCategory,
            binding.etCutoffGeneral, binding.etCutoffObc, binding.etCutoffSc,
            binding.etCutoffSt, binding.etCutoffEws, binding.btnSaveExamDetails)) view.isEnabled = enabled
    }

    /** Android owns text/cursor restoration; only the positional Spinner needs a stable identity. */
    private fun publishMetadataForm() {
        val exam = currentExam ?: return
        if (!metadataViewStateRestored || !editorIdentityCurrent) return
        if (!restoreMetadataDraft) {
            setupForm()
            binding.etCutoffGeneral.setText(exam.cutoffs["General"]?.toString().orEmpty())
            binding.etCutoffObc.setText(exam.cutoffs["OBC"]?.toString().orEmpty())
            binding.etCutoffSc.setText(exam.cutoffs["SC"]?.toString().orEmpty())
            binding.etCutoffSt.setText(exam.cutoffs["ST"]?.toString().orEmpty())
            binding.etCutoffEws.setText(exam.cutoffs["EWS"]?.toString().orEmpty())
        } else {
            restoredMetadataCategory?.let { selectMetadataCategory(it) }
        }
        restoredMetadataCategory = null
        setMetadataEditingEnabled(!metadataSaveInFlight)
        updateImportControls()
        consumePendingImport()
    }

    override fun onResume() {
        super.onResume()
        consumePendingImport()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != editorOwnerUid || live.email != editorOwnerEmail) {
                finish()
                return
            }
            if (currentExam == null) {
                binding.progressBar.visibility = View.VISIBLE
                binding.scrollEditContent.visibility = View.GONE
            }
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_exams"
            ) { permissions ->
                if (!editorIdentityCurrent) {
                    finish()
                    return@requireAdmin
                }
                binding.progressBar.visibility = View.GONE
                binding.scrollEditContent.visibility = View.VISIBLE
                editorOwnerUid = live.uid
                editorOwnerEmail = live.email.orEmpty()
                canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
                canManageContent = permissions.isOwner || "manage_content" in permissions.rights
                binding.btnAddQuestion.visibility = if (canManageQuestions) View.VISIBLE else View.GONE
                binding.btnBulkUpload.visibility = if (canManageQuestions) View.VISIBLE else View.GONE
                binding.btnPickEmoji.visibility = if (canManageContent) View.VISIBLE else View.GONE
                if (reloadQuestionsAfterStop && !deletingQuestion && questionActionsAllowed) {
                    reloadQuestionsAfterStop = false
                    loadQuestions()
                }
            }
        } else if (reloadQuestionsAfterStop && !deletingQuestion && questionActionsAllowed) {
            reloadQuestionsAfterStop = false
            loadQuestions()
        }
    }

    override fun onStop() {
        questionDialog?.dismiss()
        questionDialog = null
        ++questionDeleteEpoch
        ++questionReadGeneration
        reloadQuestionsAfterStop = true
        reauthorizeOnResume = true
        AppUndoBar.cancelDeferredForHost(this)
        super.onStop()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        metadataViewStateRestored = true
        publishMetadataForm()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("edit_exam_metadata_ready", currentExam != null || restoreMetadataDraft)
        outState.putBoolean("edit_exam_picker_pending", awaitingPickerResult)
        outState.putString("edit_exam_pending_import", pendingImportUri?.toString())
        outState.putBoolean("edit_exam_import_in_flight", importingQuestions || interruptedImportNotice)
        if (::binding.isInitialized) {
            outState.putString("edit_exam_metadata_category",
                restoredMetadataCategory ?: binding.spCategory.selectedItem?.toString())
        }
    }

    private fun loadExamData() {
        if (examId.isBlank()) return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val exams = examRepo.getExams()
                if (!editorIdentityCurrent) return@launch
                val exam = exams.find { it.id == examId }
                    ?: throw IllegalStateException(getString(R.string.exam_metadata_unavailable))
                currentExam = exam
                examName = exam.examName
                examCategory = exam.categoryOrOther
                timeLimit = exam.timeLimitMinutes
                publishMetadataForm()

                loadQuestions()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                if (editorIdentityCurrent) {
                    binding.progressBar.visibility = View.GONE
                    setMetadataEditingEnabled(false)
                    AppBulletin.showError(this@EditExamActivity, "Failed to load exam: ${e.toUserFriendlyMessage()}")
                }
            }
        }
    }

    private fun loadQuestions() {
        if (!editorIdentityCurrent) return
        val request = ++questionReadGeneration
        lifecycleScope.launch {
            try {
                val questions = examRepo.getQuestions(examId)
                if (!editorIdentityCurrent || request != questionReadGeneration) return@launch
                currentQuestions = questions.toMutableList()
                binding.progressBar.visibility = View.GONE

                publishQuestionRows()
                if (interruptedImportNotice) {
                    interruptedImportNotice = false
                    AppBulletin.show(this@EditExamActivity, getString(R.string.exam_import_interrupted))
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                if (editorIdentityCurrent && request == questionReadGeneration) {
                    binding.progressBar.visibility = View.GONE
                    AppBulletin.showError(this@EditExamActivity, "Failed to load questions: ${e.toUserFriendlyMessage()}")
                }
            }
        }
    }

    private fun saveExamDetails() {
        if (!editorIdentityCurrent || metadataSaveInFlight) return
        val baseExam = currentExam?.takeIf { it.id == examId } ?: run {
            AppBulletin.showError(this, getString(R.string.exam_metadata_unavailable))
            return
        }
        val newName = binding.etExamName.text?.toString()?.trim().orEmpty()
        val newMinutes = binding.etExamMinutes.text?.toString()?.trim()?.toIntOrNull()
        val selectedLabel = binding.spCategory.selectedItem?.toString() ?: Constants.CATEGORY_OTHER
        val selectedCategory = baseExam.categoryOrOther.takeIf { it.equals(selectedLabel, ignoreCase = true) } ?: selectedLabel

        if (newName.length < 2) {
            AppBulletin.showError(this, "Exam name must be at least 2 characters")
            return
        }
        if (newMinutes == null || newMinutes !in 1..600) {
            AppBulletin.showError(this, getString(R.string.exam_minutes_range))
            return
        }

        val cutoffsMap = baseExam.cutoffs.toMutableMap()
        for ((category, field) in listOf("General" to binding.etCutoffGeneral, "OBC" to binding.etCutoffObc,
            "SC" to binding.etCutoffSc, "ST" to binding.etCutoffSt, "EWS" to binding.etCutoffEws)) {
            val raw = field.text?.toString()?.trim().orEmpty()
            if (raw.isEmpty()) { cutoffsMap.remove(category); continue }
            val cutoff = raw.toDoubleOrNull()
            if (cutoff == null || !cutoff.isFinite() || cutoff < 0) {
                AppBulletin.showError(this, getString(R.string.exam_cutoff_invalid, category))
                return
            }
            cutoffsMap[category] = cutoff
        }

        val updatedExam = baseExam.copy(
            id = examId,
            examName = newName,
            timeLimitMinutes = newMinutes,
            category = selectedCategory,
            cutoffs = cutoffsMap
        )

        metadataSaveInFlight = true
        setMetadataEditingEnabled(false)
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                examRepo.updateExam(updatedExam)
                if (editorIdentityCurrent) {
                    currentExam = updatedExam
                    examName = newName
                    examCategory = selectedCategory
                    timeLimit = newMinutes
                    AppBulletin.showSuccess(this@EditExamActivity, "Exam details updated successfully")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                if (editorIdentityCurrent) AppBulletin.showError(this@EditExamActivity,
                    if (e is com.eve.app.data.repository.ExamUpdateNotVerifiedException) getString(R.string.exam_metadata_unconfirmed)
                    else "Failed to update exam: ${e.toUserFriendlyMessage()}")
            } finally {
                metadataSaveInFlight = false
                if (editorIdentityCurrent) {
                    binding.progressBar.visibility = View.GONE
                    setMetadataEditingEnabled(currentExam != null && metadataViewStateRestored)
                }
            }
        }
    }

    private fun updateImportControls() {
        if (!::binding.isInitialized || !editorIdentityCurrent) return
        binding.btnBulkUpload.isEnabled = questionActionsAllowed && !questionWindowBusy
        binding.btnAddQuestion.isEnabled = questionActionsAllowed && !questionWindowBusy
    }

    /** One existing Activity owns its registered result, read job and current native modal. */
    private fun consumePendingImport() {
        if (closingEditor || !questionActionsAllowed || currentExam?.id != examId ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || questionWindowBusy) return
        val uri = pendingImportUri ?: return
        val ready = parsedImport
        if (ready?.uri == uri) { showImportPreviewDialog(ready.fileName, ready.result); return }
        val request = ++importReadGeneration
        val requestedExam = examId
        val signal = CancellationSignal()
        val descriptor = java.util.concurrent.atomic.AtomicReference<android.content.res.AssetFileDescriptor?>()
        cancelImportRead = {
            signal.cancel()
            runCatching { descriptor.getAndSet(null)?.close() }
        }
        importReadJob = lifecycleScope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    val ioContext = kotlin.coroutines.coroutineContext
                    ioContext.ensureActive()
                    val fileName = getFileName(uri, signal) ?: "upload.csv"
                    val asset = contentResolver.openAssetFileDescriptor(uri, "r", signal)
                        ?: throw java.io.IOException("Selected document is unavailable")
                    asset.use { opened ->
                        descriptor.set(opened)
                        try {
                            ioContext.ensureActive()
                            opened.createInputStream().use { stream ->
                                val cancellable = object : java.io.FilterInputStream(stream) {
                                    override fun read(): Int { ioContext.ensureActive(); return super.read() }
                                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                                        ioContext.ensureActive(); return super.read(b, off, len)
                                    }
                                }
                                ParsedImport(uri, fileName, QuestionImportHelper.parseQuestions(cancellable, fileName, requestedExam))
                            }
                        } finally { descriptor.compareAndSet(opened, null) }
                    }
                }
                if (request != importReadGeneration || !questionActionsAllowed || requestedExam != examId || pendingImportUri != uri) return@launch
                parsedImport = parsed
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) {
                if (request == importReadGeneration && questionActionsAllowed) {
                    pendingImportUri = null
                    parsedImport = null
                    AppBulletin.showError(this@EditExamActivity, getString(R.string.exam_import_read_failed))
                }
            } finally {
                if (request == importReadGeneration) {
                    importReadJob = null
                    cancelImportRead = null
                    updateImportControls()
                    // A completed read is cached while stopped; Resume can show it without rereading.
                    val readyNow = parsedImport
                    if (!closingEditor && questionActionsAllowed && pendingImportUri == readyNow?.uri &&
                        readyNow != null && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && questionDialog?.isShowing != true) {
                        showImportPreviewDialog(readyNow.fileName, readyNow.result)
                    }
                }
            }
        }
        updateImportControls()
    }

    private fun getFileName(uri: Uri, signal: CancellationSignal): String? {
        if (uri.scheme == "content") {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null, signal)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun showOwnedQuestionDialog(dialog: TelegramNativeAlertDialog, importUri: Uri? = null) {
        if (closingEditor || !questionActionsAllowed || questionDialog?.isShowing == true) return
        questionDialog = dialog
        dialog.setOnDismissListener {
            if (questionDialog === dialog) {
                questionDialog = null
                if (importUri != null && pendingImportUri == importUri) {
                    pendingImportUri = null
                    parsedImport = null
                }
                updateImportControls()
                consumePendingImport()
            }
        }
        dialog.show()
        updateImportControls()
    }

    private fun showImportPreviewDialog(
        fileName: String,
        result: QuestionImportHelper.ImportResult
    ) {
        if (!questionActionsAllowed || questionDialog?.isShowing == true) return
        val validCount = result.validQuestions.size
        val errorCount = result.errors.size

        val message = buildString {
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
                if (questionDialog?.isShowing != true) return@setPositiveButton
                importQuestions(result.validQuestions)
            }
            builder.setNegativeButton("Cancel", null)
        } else {
            builder.setPositiveButton("OK", null)
        }

        val dialog = builder.create()
        showOwnedQuestionDialog(dialog, importUri = pendingImportUri)
    }

    private fun importQuestions(questions: List<Question>) {
        if (!questionActionsAllowed || importingQuestions || questions.any { it.examId != examId }) return
        importingQuestions = true
        updateImportControls()
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val added = examRepo.addQuestions(questions)
                if (!questionActionsAllowed) return@launch
                binding.progressBar.visibility = View.GONE
                AppBulletin.showSuccess(this@EditExamActivity, "Successfully imported $added questions!")
                loadQuestions()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                if (questionActionsAllowed) {
                    binding.progressBar.visibility = View.GONE
                    AppBulletin.showError(this@EditExamActivity, "Import failed: ${e.toUserFriendlyMessage()}")
                }
            } finally {
                importingQuestions = false
                updateImportControls()
            }
        }
    }

    private fun showAddQuestionDialog() {
        if (!questionActionsAllowed || questionWindowBusy) return
        val exam = currentExam
        if (exam == null) {
            AppBulletin.showError(this, "Exam details are unavailable. Reload before adding a question.")
            return
        }
        val dialog = createQuestionEditorDialog(this, exam, title = "Add New Question",
            submit = ::submitAddedQuestion)
        showOwnedQuestionDialog(dialog)
    }

    private fun submitAddedQuestion(question: Question, onFailure: (String) -> Unit, onDone: () -> Unit) {
        if (!questionActionsAllowed) return
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            var confirmed = false
            try {
                examRepo.addQuestion(question)
                confirmed = true
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                onFailure("Failed to add question: ${e.toUserFriendlyMessage()}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
            if (confirmed && questionActionsAllowed) {
                onDone()
                loadQuestions()
            }
        }
    }

    override fun onDestroy() {
        ++questionDeleteEpoch
        AppUndoBar.cancelDeferredForHost(this)
        closingEditor = true
        importReadGeneration++
        cancelImportRead?.invoke()
        cancelImportRead = null
        importReadJob?.cancel()
        importReadJob = null
        questionDialog?.dismiss()
        questionDialog = null
        parsedImport = null
        super.onDestroy()
    }

    private fun showQuestionDetails(q: Question) {
        if (!questionActionsAllowed || questionWindowBusy) return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_question_details, null)
        val etQuestionText = dialogView.findViewById<EditText>(R.id.etQuestionText)
        val etOptionA = dialogView.findViewById<EditText>(R.id.etOptionA)
        val etOptionB = dialogView.findViewById<EditText>(R.id.etOptionB)
        val etOptionC = dialogView.findViewById<EditText>(R.id.etOptionC)
        val etOptionD = dialogView.findViewById<EditText>(R.id.etOptionD)
        val rgCorrectAnswer = dialogView.findViewById<RadioGroup>(R.id.rgCorrectAnswer)
        val btnReformatAi = dialogView.findViewById<MaterialButton>(R.id.btnReformatAi)
        val pbReformat = dialogView.findViewById<TelegramRadialProgressView>(R.id.pbReformat)
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
            if (!questionActionsAllowed) return@setOnClickListener
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
                    val payload = mapOf(
                        "questionText" to qText,
                        "optionA" to optA,
                        "optionB" to optB,
                        "optionC" to optC,
                        "optionD" to optD,
                        "correctAnswer" to correctOpt,
                        "explanation" to currentExp
                    )
                    val result = com.eve.app.data.remote.ApiClient.apiService.reformatExplanation(payload)
                    if (!questionActionsAllowed) return@launch
                    val reformatted = result.data?.get("reformattedExplanation")
                    if (result.success && !reformatted.isNullOrBlank()) {
                        etExplanation.setText(reformatted)
                        AppBulletin.showSuccess(this@EditExamActivity, "Restructured to Key Points format!")
                    } else {
                        val fallback = formatKeyPointsLocally(correctOpt, currentExp)
                        etExplanation.setText(fallback)
                        AppBulletin.show(this@EditExamActivity, "AI unavailable; formatted locally (not by AI).")
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (e: Exception) {
                    if (questionActionsAllowed) {
                        val fallback = formatKeyPointsLocally(correctOpt, currentExp)
                        etExplanation.setText(fallback)
                        AppBulletin.show(this@EditExamActivity, "AI unavailable; formatted locally (not by AI).")
                    }
                } finally {
                    if (questionActionsAllowed) {
                        pbReformat.visibility = View.GONE
                        btnReformatAi.isEnabled = true
                    }
                }
            }
        }

        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Edit Question")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (!questionActionsAllowed) return@setPositiveButton
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
                        if (!questionActionsAllowed) return@launch
                        val idx = currentQuestions.indexOfFirst { it.id == q.id }
                        if (idx >= 0) {
                            currentQuestions[idx] = updatedQ
                            questionAdapter.submit(currentQuestions.toList())
                        }
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showSuccess(this@EditExamActivity, "Question updated successfully")
                    } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (e: Exception) {
                        if (questionActionsAllowed) {
                            binding.progressBar.visibility = View.GONE
                            AppBulletin.showError(this@EditExamActivity, "Failed to update question: ${e.toUserFriendlyMessage()}")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        showOwnedQuestionDialog(dialog)
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

    private fun publishQuestionRows() {
        val visible = currentQuestions.filter { it.id != pendingQuestionDeleteId }
        binding.tvQuestionsHeader.text = "Questions (${visible.size})"
        questionAdapter.submit(visible)
        binding.tvNoQuestions.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        binding.rvQuestions.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        updateImportControls()
    }

    private fun deleteRequestCurrent(epoch: Long): Boolean = questionActionsAllowed &&
        epoch == questionDeleteEpoch && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun commitPendingQuestionDelete(id: String, epoch: Long) {
        if (pendingQuestionDeleteId != id || deletingQuestion) return
        if (!deleteRequestCurrent(epoch)) { pendingQuestionDeleteId = null; return }
        deletingQuestion = true
        lifecycleScope.launch {
            var revalidate = false
            try {
                val permissions = com.eve.app.util.AdminPermissionSnapshot.load(
                    editorOwnerEmail, "manage_questions",
                    membership = { adminRepo.isAdminFresh(editorOwnerUid, editorOwnerEmail) },
                    permissions = {
                        if (!deleteRequestCurrent(epoch)) throw kotlinx.coroutines.CancellationException("Question deletion owner changed")
                        adminRepo.getAdminMe()
                    }
                )
                if (!deleteRequestCurrent(epoch)) throw kotlinx.coroutines.CancellationException("Question deletion owner changed")
                if (permissions == null || !com.eve.app.util.AdminPermissionSnapshot.allows(permissions, editorOwnerEmail, "manage_exams")) {
                    throw SecurityException("Question deletion permission is no longer available")
                }
                examRepo.deleteQuestion(id)
                if (!deleteRequestCurrent(epoch)) return@launch
                currentQuestions.removeAll { it.id == id }
                AppBulletin.showSuccess(this@EditExamActivity, getString(R.string.question_deleted))
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                revalidate = true
                if (deleteRequestCurrent(epoch)) AppBulletin.showError(this@EditExamActivity,
                    getString(R.string.question_delete_failed, e.toUserFriendlyMessage()))
            } finally {
                pendingQuestionDeleteId = null
                deletingQuestion = false
                if (questionActionsAllowed && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    publishQuestionRows()
                    if (revalidate || reloadQuestionsAfterStop || epoch != questionDeleteEpoch) {
                        reloadQuestionsAfterStop = false
                        loadQuestions()
                    }
                }
            }
        }
    }

    private fun confirmDeleteQuestion(q: Question) {
        if (!questionActionsAllowed || questionWindowBusy || q.examId != examId || currentQuestions.none { it.id == q.id }) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(getString(R.string.question_delete_title))
            .setMessage(getString(R.string.question_delete_with_undo_confirmation) + "\n\n\"${q.questionText}\"")
            .setPositiveButton(getString(R.string.question_delete_action)) { _, _ ->
                if (!questionActionsAllowed || questionDeleteBusy ||
                    !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || currentQuestions.none { it.id == q.id }) return@setPositiveButton
                val epoch = questionDeleteEpoch
                pendingQuestionDeleteId = q.id
                publishQuestionRows()
                AppUndoBar.showDeferred(
                    context = this@EditExamActivity,
                    message = getString(R.string.question_delete_pending),
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        if (pendingQuestionDeleteId == q.id && !deletingQuestion) {
                            pendingQuestionDeleteId = null
                            if (editorIdentityCurrent) publishQuestionRows()
                        }
                    },
                    onExecuteDelete = { commitPendingQuestionDelete(q.id, epoch) }
                )
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).create()
        showOwnedQuestionDialog(dialog)
    }
}
