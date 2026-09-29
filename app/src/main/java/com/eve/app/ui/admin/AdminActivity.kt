package com.eve.app.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.FrameLayout
import android.text.InputType
import com.eve.app.data.repository.FloatingLinkRepository
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.FeedbackPostReply
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityAdminBinding
import com.eve.app.data.model.HomeBanner
import com.eve.app.data.repository.HomeBannerRepository
import com.eve.app.databinding.DialogManageBannersBinding
import com.eve.app.databinding.DialogCreateFeedbackPostBinding
import com.eve.app.databinding.DialogPostRepliesBinding
import com.eve.app.util.Constants
import com.eve.app.util.ExamImageHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.provider.OpenableColumns
import com.eve.app.BuildConfig
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.data.repository.ApiUsageRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.util.QuestionImportHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val bannerRepo = HomeBannerRepository()
    private val examRepo = ExamRepository()
    private val auditLogRepo = AuditLogRepository()
    private val apiUsageRepo = ApiUsageRepository()

    private var exams: List<Exam> = emptyList()
    private var selectedExamForImageUpdate: Exam? = null
    private var onBannerImageSelected: ((Uri) -> Unit)? = null

    private val pickBannerImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onBannerImageSelected?.invoke(uri)
        }
    }

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

    private val adminEmailAdapter = AdminEmailAdapter(
        onRemove = { email -> confirmRemoveAdmin(email) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.progressBarAdmin.visibility = View.VISIBLE

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            binding.progressBarAdmin.visibility = View.GONE
            setupUi()
        }
    }

    private fun setupUi() {
        // Tab Navigation
        binding.tabLayoutAdmin.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.scrollTabCreateExam.visibility = View.VISIBLE
                        binding.scrollTabManageQuestions.visibility = View.GONE
                        binding.scrollTabAdmins.visibility = View.GONE
                    }
                    1 -> {
                        binding.scrollTabCreateExam.visibility = View.GONE
                        binding.scrollTabManageQuestions.visibility = View.VISIBLE
                        binding.scrollTabAdmins.visibility = View.GONE
                        updateQuestionsUi(viewModel.questions.value)
                    }
                    2 -> {
                        binding.scrollTabCreateExam.visibility = View.GONE
                        binding.scrollTabManageQuestions.visibility = View.GONE
                        binding.scrollTabAdmins.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        binding.rvAdmins.layoutManager = LinearLayoutManager(this)
        binding.rvAdmins.adapter = adminEmailAdapter

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val exam = exams.getOrNull(position)
                viewModel.loadQuestions(exam?.id ?: "")
                updateQuestionsUi(viewModel.questions.value)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateQuestionsUi(viewModel.questions.value)
            }
        }

        binding.btnEditExamImage.setOnClickListener { showEditExamImageDialog() }
        binding.btnRenameExam.setOnClickListener { promptRenameExam() }
        binding.btnDeleteExam.setOnClickListener { confirmDeleteExam() }
        binding.btnAddAdmin.setOnClickListener { addAdmin() }

        // Section 1: User Management
        binding.cardUserStats.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }
        binding.btnRefreshStats.setOnClickListener { viewModel.loadUserStats() }

        // Section 2: Content Management
        binding.btnManageExistingExams.setOnClickListener {
            startActivity(Intent(this, ManageExistingExamsActivity::class.java))
        }
        binding.btnManageExams.setOnClickListener {
            startActivity(Intent(this, ManageExamsActivity::class.java))
        }
        binding.btnGeneratedTests.setOnClickListener {
            startActivity(Intent(this, GeneratedTestsActivity::class.java))
        }
        binding.btnHomeBanner.setOnClickListener {
            showHomeBannerOptionsDialog()
        }
        binding.btnFloatingLink.setOnClickListener {
            showFloatingLinkDialog()
        }
        binding.btnManageSyllabus.setOnClickListener {
            startActivity(Intent(this, ManageSyllabusActivity::class.java))
        }

        // Section 3: Communication
        binding.btnSendNotification.setOnClickListener {
            startActivity(Intent(this, SendNotificationActivity::class.java))
        }
        binding.btnFlaggedQuestions.setOnClickListener {
            startActivity(Intent(this, FlaggedQuestionsActivity::class.java))
        }
        binding.btnFeedbackReplies.setOnClickListener {
            startActivity(Intent(this, FeedbackMessagesActivity::class.java))
        }
        binding.btnCreateFeedbackPost.setOnClickListener {
            showCreateFeedbackPostDialog()
        }
        binding.btnManageFeedbackPosts.setOnClickListener {
            showManageFeedbackPostsDialog()
        }

        // Section 4: Analytics & Monitoring
        binding.btnAnalyticsAdmin.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }
        binding.btnActivityLog.setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }
        binding.btnApiUsage.setOnClickListener {
            startActivity(Intent(this, ApiUsageActivity::class.java))
        }
        binding.btnExportData.setOnClickListener {
            exportExamData()
        }
        binding.btnAppConfig.setOnClickListener {
            startActivity(Intent(this, AppConfigActivity::class.java))
        }

        // System Maintenance & Version Status
        setupMaintenanceControls()
        loadAppConfigAndMaintenance()

        // Questions in Selected Exam: Manual Single-Question Add
        binding.btnAddQuestionManual.setOnClickListener {
            val selected = exams.getOrNull(binding.spExam.selectedItemPosition)
            if (selected == null) {
                AppBulletin.showError(this, "Please select an exam first to add a question")
                return@setOnClickListener
            }
            showAddQuestionDialog(selected)
        }

        // Questions in Selected Exam: Bulk Upload
        binding.btnBulkUploadSelectedExam.setOnClickListener {
            val selected = exams.getOrNull(binding.spExam.selectedItemPosition)
            if (selected == null) {
                AppBulletin.showError(this, "Please select an exam first to upload questions to")
                return@setOnClickListener
            }
            pickBulkQuestionsLauncher.launch("*/*")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.totalUsers.collect { count ->
                        binding.tvTotalUsers.text = count?.toString() ?: "—"
                    }
                }
                launch {
                    viewModel.onlineUsers.collect { count ->
                        binding.tvOnlineUsers.text = count?.toString() ?: "—"
                    }
                }
                launch {
                    viewModel.exams.collect { list ->
                        exams = list
                        binding.spExam.adapter = ArrayAdapter(
                            this@AdminActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            list.map { it.examName }
                        )
                        val selected = exams.getOrNull(binding.spExam.selectedItemPosition)
                        viewModel.loadQuestions(selected?.id ?: "")
                        updateQuestionsUi(viewModel.questions.value)
                    }
                }
                launch {
                    viewModel.questions.collect { list ->
                        questionAdapter.submit(list)
                        updateQuestionsUi(list)
                    }
                }
                launch {
                    viewModel.admins.collect { list ->
                        adminEmailAdapter.submit(list)
                        binding.tvNoAdmins.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.busy.collect { busy ->
                        binding.btnRenameExam.isEnabled = !busy
                        binding.btnDeleteExam.isEnabled = !busy
                    }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            AppBulletin.show(this@AdminActivity, msg)
                            viewModel.consumeMessage()
                        }
                    }
                }
            }
        }
    }

    private fun updateQuestionsUi(questionsList: List<Question>) {
        val selectedExam = exams.getOrNull(binding.spExam.selectedItemPosition)

        if (selectedExam == null) {
            // Case 1: No exam selected
            binding.layoutEmptyQuestions.visibility = View.VISIBLE
            binding.rvQuestions.visibility = View.GONE
            binding.tvNoQuestions.visibility = View.GONE
            binding.tvEmptyQuestionsTitle.text = "Select an exam above to view its questions"
            binding.tvEmptyQuestionsSubtitle.text = "Choose an exam from the dropdown to manage questions or upload new ones."
            animateEmptyFolderIcon()
        } else if (questionsList.isEmpty()) {
            // Case 2: Exam selected but genuinely 0 questions
            binding.layoutEmptyQuestions.visibility = View.VISIBLE
            binding.rvQuestions.visibility = View.GONE
            binding.tvNoQuestions.visibility = View.GONE
            binding.tvEmptyQuestionsTitle.text = "No questions uploaded yet for ${selectedExam.examName}"
            binding.tvEmptyQuestionsSubtitle.text = "Add questions manually or import them in bulk from CSV / Excel."
            animateEmptyFolderIcon()
        } else {
            // Case 3: Exam selected with questions
            binding.layoutEmptyQuestions.visibility = View.GONE
            binding.rvQuestions.visibility = View.VISIBLE
            binding.tvNoQuestions.visibility = View.GONE
        }
    }

    private fun animateEmptyFolderIcon() {
        binding.ivEmptyStateFolder.alpha = 0f
        binding.ivEmptyStateFolder.scaleX = 0.8f
        binding.ivEmptyStateFolder.scaleY = 0.8f
        binding.ivEmptyStateFolder.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
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

        MaterialAlertDialogBuilder(this)
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
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_EXAM_EDITED,
                        "Added question to ${selectedExam.examName}"
                    )
                    apiUsageRepo.incrementDocumentWrites(1)
                    AppBulletin.showSuccess(this@AdminActivity, "Question added successfully!")
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
        AlertDialog.Builder(this)
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
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
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
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_EXAM_EDITED,
                        "Renamed exam '${exam.examName}' to '$newName'"
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition) ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete ${exam.examName}?")
            .setMessage("Delete ${exam.examName}? This will permanently remove the exam and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExam(exam.id) {
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_EXAM_DELETED,
                        "Deleted exam '${exam.examName}'"
                    )
                    AppBulletin.showSuccess(this@AdminActivity, "Exam '${exam.examName}' deleted")
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
        AlertDialog.Builder(this)
            .setTitle("Question Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDeleteQuestion(q: Question) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Question?")
            .setMessage("Delete this question? This will permanently remove the question and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuestion(q)
                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_EXAM_EDITED,
                    "Deleted question: ${q.questionText.take(40)}"
                )
                AppBulletin.showSuccess(this@AdminActivity, "Question deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addAdmin() {
        val email = binding.etAdminEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            AppBulletin.showError(this, "Please enter a valid email address")
            return
        }
        viewModel.addAdmin(email) {
            auditLogRepo.recordLog(
                AdminAuditLog.ACTION_ADMIN_ADDED,
                "Added new admin email: $email"
            )
            binding.etAdminEmail.text?.clear()
        }
    }

    private fun confirmRemoveAdmin(email: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Admin?")
            .setMessage("Remove $email? This will revoke admin privileges and cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeAdmin(email)
                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_ADMIN_REMOVED,
                    "Revoked admin privileges for: $email"
                )
                AppBulletin.showSuccess(this@AdminActivity, "Admin $email removed")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleBulkUploadForSelectedExam(uri: Uri) {
        val selectedExam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (selectedExam == null) {
            AppBulletin.showError(this, "No exam selected to upload questions")
            return
        }
        val fileName = getFileName(uri) ?: "questions.csv"
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val result = QuestionImportHelper.parseQuestions(stream, fileName, selectedExam.id)
                showBulkImportPreviewDialog(fileName, selectedExam, result)
            }
        } catch (e: Exception) {
            AppBulletin.showError(this, "Failed to read file: ${e.message}")
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

    private fun showBulkImportPreviewDialog(
        fileName: String,
        selectedExam: Exam,
        result: QuestionImportHelper.ImportResult
    ) {
        val validCount = result.validQuestions.size
        val errorCount = result.errors.size

        val message = buildString {
            append("Exam: ${selectedExam.examName}\n")
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
                importQuestionsForSelectedExam(selectedExam.id, result.validQuestions)
            }
            builder.setNegativeButton("Cancel", null)
        } else {
            builder.setPositiveButton("OK", null)
        }

        builder.show()
    }

    private fun importQuestionsForSelectedExam(examId: String, questions: List<Question>) {
        lifecycleScope.launch {
            try {
                val added = examRepo.addQuestions(questions)
                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_EXAM_EDITED,
                    "Imported $added questions to exam"
                )
                apiUsageRepo.incrementDocumentWrites(added)
                AppBulletin.showSuccess(this@AdminActivity, "Successfully imported $added questions!")
                viewModel.loadQuestions(examId)
            } catch (e: Exception) {
                AppBulletin.showError(this@AdminActivity, "Import failed: ${e.message}")
            }
        }
    }

    private fun showCreateFeedbackPostDialog() {
        val dialogBinding = DialogCreateFeedbackPostBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelPost.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnPublishPost.setOnClickListener {
            val title = dialogBinding.etPostTitle.text?.toString()?.trim().orEmpty()
            val message = dialogBinding.etPostMessage.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                dialogBinding.tilPostTitle.error = "Title cannot be empty"
                return@setOnClickListener
            }
            dialogBinding.tilPostTitle.error = null

            if (message.isEmpty()) {
                dialogBinding.tilPostMessage.error = "Message cannot be empty"
                return@setOnClickListener
            }
            dialogBinding.tilPostMessage.error = null

            val user = FirebaseAuth.getInstance().currentUser
            val authorId = user?.uid ?: ""
            val authorEmail = user?.email ?: ""

            dialogBinding.btnPublishPost.isEnabled = false
            lifecycleScope.launch {
                val feedbackRepo = FeedbackRepository()
                val result = feedbackRepo.createFeedbackPost(title, message, authorId, authorEmail)
                dialogBinding.btnPublishPost.isEnabled = true
                result.onSuccess {
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_FEEDBACK_POST_CREATED,
                        "Created feedback post '$title'"
                    )
                    apiUsageRepo.incrementDocumentWrites(1)
                    AppBulletin.showSuccess(this@AdminActivity, "Feedback post published successfully!")
                    dialog.dismiss()
                }.onFailure { err ->
                    AppBulletin.showError(this@AdminActivity, "Failed to publish: ${err.localizedMessage ?: "Unknown error"}")
                }
            }
        }

        dialog.show()
    }

    private fun showHomeBannerOptionsDialog() {
        val dialogBinding = DialogManageBannersBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        var pendingBannerUri: Uri? = null

        val bannerAdapter = AdminBannerAdapter(
            onMoveUp = { banner ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.reorderBanner(banner.id, moveUp = true)
                        refreshBannerList(dialogBinding)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@AdminActivity, "Error reordering: ${e.localizedMessage}")
                    }
                }
            },
            onMoveDown = { banner ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.reorderBanner(banner.id, moveUp = false)
                        refreshBannerList(dialogBinding)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@AdminActivity, "Error reordering: ${e.localizedMessage}")
                    }
                }
            },
            onDelete = { banner ->
                confirmDeleteBanner(banner) {
                    refreshBannerList(dialogBinding)
                }
            }
        )

        dialogBinding.rvAdminBanners.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = bannerAdapter
        }

        dialogBinding.btnPickBannerImage.setOnClickListener {
            onBannerImageSelected = { uri ->
                val sizeBytes = try {
                    contentResolver.openInputStream(uri)?.use { it.available() } ?: 0
                } catch (e: Exception) {
                    0
                }

                if (sizeBytes > 5 * 1024 * 1024) {
                    val sizeMb = String.format(java.util.Locale.US, "%.1f", sizeBytes / (1024f * 1024f))
                    dialogBinding.tvBannerUploadError.text = "Image size exceeds 5MB limit ($sizeMb MB). Please select a smaller image."
                    dialogBinding.tvBannerUploadError.visibility = View.VISIBLE
                    dialogBinding.cardPreviewBanner.visibility = View.GONE
                    dialogBinding.btnSaveBanner.visibility = View.GONE
                    pendingBannerUri = null
                } else {
                    dialogBinding.tvBannerUploadError.visibility = View.GONE
                    dialogBinding.cardPreviewBanner.visibility = View.VISIBLE
                    dialogBinding.ivPreviewBanner.setImageURI(uri)
                    dialogBinding.btnSaveBanner.visibility = View.VISIBLE
                    pendingBannerUri = uri
                }
            }
            pickBannerImageLauncher.launch("image/*")
        }

        dialogBinding.btnSaveBanner.setOnClickListener {
            val uri = pendingBannerUri ?: return@setOnClickListener
            dialogBinding.btnSaveBanner.isEnabled = false
            dialogBinding.btnSaveBanner.text = "Uploading..."
            val email = FirebaseAuth.getInstance().currentUser?.email ?: "admin"
            lifecycleScope.launch {
                val result = bannerRepo.uploadBanner(this@AdminActivity, uri, email)
                if (result.isSuccess) {
                    AppBulletin.showSuccess(this@AdminActivity, "Banner published successfully!")
                    dialogBinding.cardPreviewBanner.visibility = View.GONE
                    dialogBinding.btnSaveBanner.visibility = View.GONE
                    dialogBinding.btnSaveBanner.isEnabled = true
                    dialogBinding.btnSaveBanner.text = "Save & Publish Banner"
                    pendingBannerUri = null
                    refreshBannerList(dialogBinding)
                } else {
                    dialogBinding.btnSaveBanner.isEnabled = true
                    dialogBinding.btnSaveBanner.text = "Save & Publish Banner"
                    dialogBinding.tvBannerUploadError.text = "Upload failed: ${result.exceptionOrNull()?.localizedMessage}"
                    dialogBinding.tvBannerUploadError.visibility = View.VISIBLE
                }
            }
        }

        dialogBinding.btnCloseBannerDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            onBannerImageSelected = null
        }

        refreshBannerList(dialogBinding)
        dialog.show()
    }

    private fun refreshBannerList(dialogBinding: DialogManageBannersBinding) {
        lifecycleScope.launch {
            try {
                val banners = bannerRepo.getBanners()
                (dialogBinding.rvAdminBanners.adapter as? AdminBannerAdapter)?.submitList(banners)
                dialogBinding.tvNoBanners.visibility = if (banners.isEmpty()) View.VISIBLE else View.GONE
                dialogBinding.rvAdminBanners.visibility = if (banners.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                AppBulletin.showError(this@AdminActivity, "Failed to load banners: ${e.localizedMessage}")
            }
        }
    }

    private fun confirmDeleteBanner(banner: HomeBanner, onDeleted: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Home Banner?")
            .setMessage("This will remove this banner from the student Home screen carousel immediately.")
            .setPositiveButton("Delete") { _, _ ->
                AppUndoBar.show(
                    context = this@AdminActivity,
                    message = "Banner deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        AppBulletin.show(this@AdminActivity, "Delete cancelled")
                    },
                    onExecuteDelete = {
                        lifecycleScope.launch {
                            try {
                                bannerRepo.deleteBanner(banner.id)
                                onDeleted()
                            } catch (e: Exception) {
                                AppBulletin.showError(this@AdminActivity, "Failed to delete: ${e.localizedMessage}")
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFloatingLinkDialog() {
        val floatingLinkRepo = FloatingLinkRepository()
        val input = EditText(this).apply {
            hint = "https://t.me/..."
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val container = FrameLayout(this).apply {
            val marginH = (20 * resources.displayMetrics.density).toInt()
            val marginV = (8 * resources.displayMetrics.density).toInt()
            setPadding(marginH, marginV, marginH, marginV)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Floating Link")
            .setMessage("Set the HTTPS community link for the floating paper airplane chip on the Home screen.")
            .setView(container)
            .setPositiveButton("Save", null)
            .setNegativeButton("Clear", null)
            .setNeutralButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            lifecycleScope.launch {
                floatingLinkRepo.getAdminFloatingLink()
                    .onSuccess { currentUrl ->
                        input.setText(currentUrl)
                        input.setSelection(input.text.length)
                    }
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val rawText = input.text.toString().trim()
                if (rawText.isEmpty()) {
                    lifecycleScope.launch {
                        floatingLinkRepo.clearFloatingLink()
                            .onSuccess {
                                auditLogRepo.recordLog(
                                    AdminAuditLog.ACTION_FLOATING_LINK_UPDATED,
                                    "Cleared floating community link"
                                )
                                AppBulletin.showSuccess(this@AdminActivity, "Floating link cleared")
                                dialog.dismiss()
                            }
                            .onFailure { e ->
                                AppBulletin.showError(this@AdminActivity, e.localizedMessage ?: "Failed to clear link")
                            }
                    }
                    return@setOnClickListener
                }

                val finalUrl = if (!rawText.startsWith("http://", ignoreCase = true) &&
                    !rawText.startsWith("https://", ignoreCase = true)
                ) {
                    "https://$rawText"
                } else {
                    rawText
                }

                if (!FloatingLinkRepository.isValidHttpsUrl(finalUrl)) {
                    AppBulletin.showError(this@AdminActivity, "Invalid URL. Must be a valid HTTPS URL up to 500 characters.")
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    floatingLinkRepo.updateFloatingLink(finalUrl)
                        .onSuccess { cleanUrl ->
                            auditLogRepo.recordLog(
                                AdminAuditLog.ACTION_FLOATING_LINK_UPDATED,
                                "Updated floating community link to $cleanUrl"
                            )
                            AppBulletin.showSuccess(this@AdminActivity, "Floating link updated")
                            dialog.dismiss()
                        }
                        .onFailure { e ->
                            AppBulletin.showError(this@AdminActivity, e.localizedMessage ?: "Failed to update link")
                        }
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                lifecycleScope.launch {
                    floatingLinkRepo.clearFloatingLink()
                        .onSuccess {
                            auditLogRepo.recordLog(
                                AdminAuditLog.ACTION_FLOATING_LINK_UPDATED,
                                "Cleared floating community link"
                            )
                            AppBulletin.showSuccess(this@AdminActivity, "Floating link cleared")
                            dialog.dismiss()
                        }
                        .onFailure { e ->
                            AppBulletin.showError(this@AdminActivity, e.localizedMessage ?: "Failed to clear link")
                        }
                }
            }
        }

        dialog.show()
    }

    private fun showFeedbackRepliesDialog() {
        val feedbackRepo = FeedbackRepository()
        lifecycleScope.launch {
            val posts = feedbackRepo.getFeedbackPosts()
            if (posts.isEmpty()) {
                AppBulletin.show(this@AdminActivity, "No feedback posts found.")
                return@launch
            }
            if (posts.size == 1) {
                showPostRepliesDialog(posts[0])
            } else {
                val postOptions = posts.map { post ->
                    "${post.title} (${post.message.take(30)}...)"
                }.toTypedArray()
                MaterialAlertDialogBuilder(this@AdminActivity)
                    .setTitle("Select Feedback Post to View Replies")
                    .setItems(postOptions) { _, which ->
                        showPostRepliesDialog(posts[which])
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showManageFeedbackPostsDialog() {
        val feedbackRepo = FeedbackRepository()
        lifecycleScope.launch {
            val posts = feedbackRepo.getFeedbackPosts()
            if (posts.isEmpty()) {
                AppBulletin.show(this@AdminActivity, "No feedback posts published yet.")
                return@launch
            }
            val titles = posts.map { "${it.title} (${it.message.take(30)}...)" }.toTypedArray()
            MaterialAlertDialogBuilder(this@AdminActivity)
                .setTitle("Feedback Posts")
                .setItems(titles) { _, which ->
                    val selected = posts[which]
                    showPostActionsDialog(selected)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showPostActionsDialog(post: FeedbackPost) {
        val feedbackRepo = FeedbackRepository()
        val options = arrayOf("✏️ Edit Post", "👁️ View Replies", "🗑️ Delete Post")
        MaterialAlertDialogBuilder(this)
            .setTitle(post.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPostDialog(post)
                    1 -> showPostRepliesDialog(post)
                    2 -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Delete Post?")
                            .setMessage("Delete '${post.title}' from Home screen? All its student replies will also be permanently deleted.")
                            .setPositiveButton("Delete") { _, _ ->
                                AppUndoBar.show(
                                    context = this@AdminActivity,
                                    message = "Post '${post.title}' deleted",
                                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                                    onUndo = {
                                        AppBulletin.show(this@AdminActivity, "Delete cancelled")
                                    },
                                    onExecuteDelete = {
                                        lifecycleScope.launch {
                                            feedbackRepo.deleteFeedbackPost(post.id)
                                            auditLogRepo.recordLog(
                                                AdminAuditLog.ACTION_FEEDBACK_POST_DELETED,
                                                "Deleted feedback post '${post.title}'"
                                            )
                                        }
                                    }
                                )
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun showEditPostDialog(post: FeedbackPost) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etTitle = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = "Post Title"
            setText(post.title)
        }
        val tilTitle = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Post Title"
            addView(etTitle)
        }
        val etMessage = com.google.android.material.textfield.TextInputEditText(this).apply {
            hint = "Message / Prompt"
            setText(post.message)
            minLines = 3
        }
        val tilMessage = com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Message / Prompt"
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 24
            }
            layoutParams = params
            addView(etMessage)
        }
        layout.addView(tilTitle)
        layout.addView(tilMessage)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Feedback Post")
            .setView(layout)
            .setPositiveButton("Save Changes") { _, _ ->
                val newTitle = etTitle.text?.toString()?.trim().orEmpty()
                val newMessage = etMessage.text?.toString()?.trim().orEmpty()
                if (newTitle.isBlank() || newMessage.isBlank()) {
                    AppBulletin.showError(this, "Title and message cannot be empty")
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val feedbackRepo = FeedbackRepository()
                    val res = feedbackRepo.updateFeedbackPost(post.id, newTitle, newMessage)
                    res.onSuccess {
                        auditLogRepo.recordLog(
                            AdminAuditLog.ACTION_FEEDBACK_POST_EDITED,
                            "Updated feedback post '$newTitle'"
                        )
                        AppBulletin.showSuccess(this@AdminActivity, "Feedback post updated successfully!")
                    }.onFailure { e ->
                        AppBulletin.showError(this@AdminActivity, "Failed to update: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostRepliesDialog(post: FeedbackPost) {
        val dialogBinding = DialogPostRepliesBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvPostRepliesTitle.text = "Replies: ${post.title}"
        dialogBinding.tvPostRepliesSnippet.text = post.message

        val feedbackRepo = FeedbackRepository()
        var currentReplies: List<FeedbackPostReply> = emptyList()

        lateinit var repliesAdapter: PostRepliesAdapter
        repliesAdapter = PostRepliesAdapter(
            onMarkRead = { reply ->
                lifecycleScope.launch {
                    feedbackRepo.markReplyAsRead(post.id, reply.id).onSuccess {
                        currentReplies = currentReplies.map { if (it.id == reply.id) it.copy(read = true) else it }
                        repliesAdapter.submitList(currentReplies)
                        AppBulletin.showSuccess(this@AdminActivity, "Marked as read")
                    }
                }
            },
            onDelete = { reply ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Reply?")
                    .setMessage("Delete reply from ${reply.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            feedbackRepo.deleteSingleReply(post.id, reply.id).onSuccess {
                                currentReplies = currentReplies.filter { it.id != reply.id }
                                repliesAdapter.submitList(currentReplies)
                                dialogBinding.tvNoReplies.visibility = if (currentReplies.isEmpty()) View.VISIBLE else View.GONE
                                AppBulletin.showSuccess(this@AdminActivity, "Reply deleted")
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        dialogBinding.rvReplies.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvReplies.adapter = repliesAdapter

        dialogBinding.btnDeletePostFromReplies.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Post?")
                .setMessage("Delete '${post.title}' from Home screen? All its student replies will also be permanently deleted.")
                .setPositiveButton("Delete") { _, _ ->
                    dialog.dismiss()
                    AppUndoBar.show(
                        context = this@AdminActivity,
                        message = "Post '${post.title}' deleted",
                        timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                        onUndo = {
                            AppBulletin.show(this@AdminActivity, "Delete cancelled")
                        },
                        onExecuteDelete = {
                            lifecycleScope.launch {
                                feedbackRepo.deleteFeedbackPost(post.id)
                                auditLogRepo.recordLog(
                                    AdminAuditLog.ACTION_FEEDBACK_POST_DELETED,
                                    "Deleted feedback post '${post.title}'"
                                )
                            }
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialogBinding.btnCloseReplies.setOnClickListener { dialog.dismiss() }

        dialogBinding.progressBarReplies.visibility = View.VISIBLE
        lifecycleScope.launch {
            currentReplies = feedbackRepo.getRepliesForPost(post.id)
            dialogBinding.progressBarReplies.visibility = View.GONE
            repliesAdapter.submitList(currentReplies)
            dialogBinding.tvNoReplies.visibility = if (currentReplies.isEmpty()) View.VISIBLE else View.GONE
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadAppConfigAndMaintenance()
    }

    private fun setupMaintenanceControls() {
        binding.switchMaintenanceMode.setOnClickListener {
            val willBeOn = binding.switchMaintenanceMode.isChecked
            if (willBeOn) {
                // Must show confirmation dialog before turning ON
                binding.switchMaintenanceMode.isChecked = false
                MaterialAlertDialogBuilder(this)
                    .setTitle("Enable Maintenance Mode?")
                    .setMessage("Are you sure? Turning ON maintenance mode will immediately block all students from accessing the app. Only enable this during planned outages.")
                    .setPositiveButton("Enable Maintenance") { _, _ ->
                        binding.switchMaintenanceMode.isChecked = true
                        updateMaintenanceStatusBadge(true)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Turning OFF does not require confirmation dialog
                updateMaintenanceStatusBadge(false)
            }
        }

        binding.btnApplyMaintenance.setOnClickListener {
            applyMaintenanceSettings()
        }
    }

    private fun updateMaintenanceStatusBadge(isModeActive: Boolean) {
        if (isModeActive) {
            binding.tvLiveMaintenanceStatus.text = "ACTIVE (Restricted)"
            binding.tvLiveMaintenanceStatus.setTextColor(getColor(R.color.eve_red))
        } else {
            binding.tvLiveMaintenanceStatus.text = "OFF (Live)"
            binding.tvLiveMaintenanceStatus.setTextColor(getColor(R.color.eve_green))
        }
    }

    private fun loadAppConfigAndMaintenance() {
        binding.tvInstalledVersion.text = "v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})"

        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                binding.tvMinSupportedVersion.text = config.minimum_supported_version_code.toString()

                val isSupported = BuildConfig.VERSION_CODE >= config.minimum_supported_version_code
                binding.tvVersionStatusBadge.text = if (isSupported) "Supported" else "Update Required"
                binding.tvVersionStatusBadge.setTextColor(getColor(if (isSupported) R.color.eve_green else R.color.eve_red))

                binding.switchMaintenanceMode.isChecked = config.maintenance_mode
                binding.etMaintenanceMessage.setText(config.maintenance_message)
                updateMaintenanceStatusBadge(config.maintenance_mode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyMaintenanceSettings() {
        val isModeOn = binding.switchMaintenanceMode.isChecked
        val message = binding.etMaintenanceMessage.text?.toString()?.trim().orEmpty()
            .ifBlank { "Scheduled maintenance is currently in progress. Please check back shortly." }

        binding.progressBarMaintenance.visibility = View.VISIBLE
        binding.btnApplyMaintenance.isEnabled = false

        lifecycleScope.launch {
            try {
                // 1. Invoke Callable Cloud Function
                try {
                    val functions = FirebaseFunctions.getInstance()
                    val payload = hashMapOf(
                        "maintenanceMode" to isModeOn,
                        "maintenanceMessage" to message
                    )
                    functions.getHttpsCallable("updateRemoteConfigMaintenance").call(payload).await()
                } catch (fnErr: Exception) {
                    // Fallback to direct Firestore update if callable function is unavailable
                }

                // 2. Ensure Firestore app_config is always updated
                val currentConfig = adminRepo.getAppConfig()
                adminRepo.updateAppConfig(
                    currentConfig.copy(
                        maintenance_mode = isModeOn,
                        maintenance_message = message
                    )
                )

                // 3. Record in Audit Log
                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_MAINTENANCE_TOGGLED,
                    if (isModeOn) "Turned ON maintenance mode: $message" else "Turned OFF maintenance mode"
                )

                binding.progressBarMaintenance.visibility = View.GONE
                binding.btnApplyMaintenance.isEnabled = true
                updateMaintenanceStatusBadge(isModeOn)
                AppBulletin.showSuccess(
                    this@AdminActivity,
                    if (isModeOn) "Maintenance mode published and ACTIVE" else "Maintenance mode turned OFF (Normal operation)"
                )
            } catch (e: Exception) {
                binding.progressBarMaintenance.visibility = View.GONE
                binding.btnApplyMaintenance.isEnabled = true
                AppBulletin.showError(this@AdminActivity, "Failed to apply maintenance settings: ${e.localizedMessage}")
            }
        }
    }

    private fun exportExamData() {
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Exporting Exam Data")
            .setMessage("Fetching all exams and questions from database... Please wait.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val allExams = examRepo.getExams()
                val exportList = mutableListOf<Map<String, Any?>>()

                for (exam in allExams) {
                    val questions = examRepo.getQuestions(exam.id)
                    val examMap = linkedMapOf<String, Any?>(
                        "id" to exam.id,
                        "examName" to exam.examName,
                        "category" to exam.categoryOrOther,
                        "timeLimitMinutes" to exam.timeLimitMinutes,
                        "questionCount" to questions.size,
                        "autoGenerationEnabled" to exam.autoGenerationEnabled,
                        "autoGenTime" to exam.autoGenTime,
                        "generationPrompt" to exam.generationPrompt,
                        "syllabusFileName" to exam.syllabusFileName,
                        "syllabusUrl" to exam.syllabusUrl,
                        "questions" to questions.map { q ->
                            linkedMapOf(
                                "id" to q.id,
                                "examId" to q.examId,
                                "questionText" to q.questionText,
                                "optionA" to q.optionA,
                                "optionB" to q.optionB,
                                "optionC" to q.optionC,
                                "optionD" to q.optionD,
                                "correctAnswer" to q.correctAnswer,
                                "explanation" to q.explanation,
                                "topic" to q.topic,
                                "questionTextHi" to q.questionTextHi,
                                "optionAHi" to q.optionAHi,
                                "optionBHi" to q.optionBHi,
                                "optionCHi" to q.optionCHi,
                                "optionDHi" to q.optionDHi,
                                "explanationHi" to q.explanationHi
                            )
                        }
                    )
                    exportList.add(examMap)
                }

                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val fileName = "eve_backup_$dateStr.json"
                val jsonString = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(exportList)

                // Write to cache directory for reliable FileProvider sharing
                val cacheFile = File(cacheDir, fileName)
                cacheFile.writeText(jsonString)

                // Also try saving to public Downloads or App External Files Dir
                var savedPath = cacheFile.absolutePath
                try {
                    val extDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (extDir != null) {
                        val extFile = File(extDir, fileName)
                        extFile.writeText(jsonString)
                        savedPath = extFile.absolutePath
                    }
                } catch (_: Exception) {}

                progressDialog.dismiss()

                // Record audit log
                auditLogRepo.recordLog(
                    AdminAuditLog.ACTION_EXAM_EDITED,
                    "Exported backup of ${allExams.size} exams and ${exportList.sumOf { (it["questions"] as? List<*>)?.size ?: 0 }} questions to $fileName"
                )

                // Present success dialog with share action
                val contentUri = FileProvider.getUriForFile(this@AdminActivity, "${packageName}.fileprovider", cacheFile)
                MaterialAlertDialogBuilder(this@AdminActivity)
                    .setTitle("Export Complete")
                    .setMessage("Successfully exported ${allExams.size} exams to:\n$savedPath\n\nYou can also share or save this file to Google Drive / Downloads now.")
                    .setPositiveButton("Share / Save As") { _, _ ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_SUBJECT, fileName)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Save or Share Exam Backup"))
                    }
                    .setNegativeButton("Done", null)
                    .show()

            } catch (e: Exception) {
                progressDialog.dismiss()
                AppBulletin.showError(this@AdminActivity, "Export failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }
}
