package com.eve.app.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val bannerRepo = HomeBannerRepository()

    private var exams: List<Exam> = emptyList()
    private var newExamImageBase64: String = ""
    private var selectedExamForImageUpdate: Exam? = null
    private var onBannerImageSelected: ((Uri) -> Unit)? = null

    private val pickBannerImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onBannerImageSelected?.invoke(uri)
        }
    }

    private val pickNewExamImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val base64 = ExamImageHelper.uriToBase64(this, uri)
            if (base64 != null) {
                newExamImageBase64 = base64
                ExamImageHelper.loadExamImage(binding.ivNewExamImagePreview, base64)
                binding.btnRemoveExamImage.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
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
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
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

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setupUi()
        }
    }

    private fun setupUi() {
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Constants.CATEGORIES
        )
        binding.spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.etOtherCategory.visibility =
                    if (Constants.CATEGORIES.getOrNull(position) == Constants.CATEGORY_OTHER) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        binding.rvAdmins.layoutManager = LinearLayoutManager(this)
        binding.rvAdmins.adapter = adminEmailAdapter

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val exam = exams.getOrNull(position)
                viewModel.loadQuestions(exam?.id ?: "")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnChooseExamImage.setOnClickListener { pickNewExamImageLauncher.launch("image/*") }
        binding.btnRemoveExamImage.setOnClickListener {
            newExamImageBase64 = ""
            binding.ivNewExamImagePreview.setImageResource(R.drawable.ic_exam_placeholder)
            binding.btnRemoveExamImage.visibility = View.GONE
        }
        binding.btnAddExam.setOnClickListener { addExam() }
        binding.btnEditExamImage.setOnClickListener { showEditExamImageDialog() }
        binding.btnRenameExam.setOnClickListener { promptRenameExam() }
        binding.btnDeleteExam.setOnClickListener { confirmDeleteExam() }
        binding.btnAddAdmin.setOnClickListener { addAdmin() }

        binding.btnAnalyticsAdmin.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }
        binding.btnSendNotification.setOnClickListener {
            startActivity(Intent(this, SendNotificationActivity::class.java))
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
        binding.btnFeedbackReplies.setOnClickListener {
            showFeedbackRepliesDialog()
        }
        binding.btnCreateFeedbackPost.setOnClickListener {
            showCreateFeedbackPostDialog()
        }
        binding.btnRefreshStats.setOnClickListener { viewModel.loadUserStats() }

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
                    }
                }
                launch {
                    viewModel.questions.collect { list ->
                        questionAdapter.submit(list)
                        binding.tvNoQuestions.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
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
                        binding.btnAddExam.isEnabled = !busy
                        binding.btnRenameExam.isEnabled = !busy
                        binding.btnDeleteExam.isEnabled = !busy
                    }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@AdminActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.consumeMessage()
                        }
                    }
                }
            }
        }
    }

    private fun addExam() {
        val name = binding.etExamName.text.toString().trim()
        val minutes = binding.etExamMinutes.text.toString().trim().toIntOrNull()
        val selectedCategory = binding.spCategory.selectedItem as? String ?: Constants.CATEGORY_OTHER
        val category = if (selectedCategory == Constants.CATEGORY_OTHER) {
            binding.etOtherCategory.text.toString().trim()
        } else {
            selectedCategory
        }
        if (name.length < 2 || minutes == null || minutes <= 0) {
            Toast.makeText(this, "Please enter exam name (min 2 characters) and valid duration in minutes", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isEmpty()) {
            Toast.makeText(this, "Please select or type a category", Toast.LENGTH_SHORT).show()
            return
        }
        // Duplicate check within same category
        if (exams.any { it.categoryOrOther.equals(category, ignoreCase = true) && it.examName.trim().equals(name, ignoreCase = true) }) {
            Toast.makeText(this, "An exam named '$name' already exists in category '$category'", Toast.LENGTH_LONG).show()
            return
        }
        viewModel.addExam(name, minutes, category, imageUrl = newExamImageBase64) {
            binding.etExamName.text?.clear()
            binding.etExamMinutes.text?.clear()
            binding.spCategory.setSelection(0)
            binding.etOtherCategory.text?.clear()
            binding.etOtherCategory.visibility = View.GONE
            newExamImageBase64 = ""
            binding.ivNewExamImagePreview.setImageResource(R.drawable.ic_exam_placeholder)
            binding.btnRemoveExamImage.visibility = View.GONE
        }
    }

    private fun showEditExamImageDialog() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (exam == null) {
            Toast.makeText(this, "Please select an exam first", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Please select an exam to rename", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Exam name must be at least 2 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (exams.any { it.id != exam.id && it.categoryOrOther.equals(exam.categoryOrOther, ignoreCase = true) && it.examName.trim().equals(newName, ignoreCase = true) }) {
                    Toast.makeText(this, "An exam named '$newName' already exists in category '${exam.categoryOrOther}'", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                viewModel.renameExam(exam.id, newName) { }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition) ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Exam?")
            .setMessage("Exam '${exam.examName}' and all its tests/questions will be permanently deleted.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExam(exam.id) { }
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
        AlertDialog.Builder(this)
            .setTitle("Delete Question?")
            .setMessage(q.questionText)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuestion(q)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addAdmin() {
        val email = binding.etAdminEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addAdmin(email) {
            binding.etAdminEmail.text?.clear()
        }
    }

    private fun confirmRemoveAdmin(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Admin?")
            .setMessage("$email will no longer have admin privileges.")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeAdmin(email) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateFeedbackPostDialog() {
        val dialogBinding = DialogCreateFeedbackPostBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelPost.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnManageExistingPosts.setOnClickListener {
            showManageFeedbackPostsDialog()
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
                    Toast.makeText(this@AdminActivity, "Feedback post published successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.onFailure { err ->
                    Toast.makeText(this@AdminActivity, "Failed to publish: ${err.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@AdminActivity, "Error reordering: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMoveDown = { banner ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.reorderBanner(banner.id, moveUp = false)
                        refreshBannerList(dialogBinding)
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminActivity, "Error reordering: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@AdminActivity, "Banner published successfully!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@AdminActivity, "Failed to load banners: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteBanner(banner: HomeBanner, onDeleted: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Home Banner?")
            .setMessage("This will remove this banner from the student Home screen carousel immediately.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.deleteBanner(banner.id)
                        Toast.makeText(this@AdminActivity, "Banner deleted", Toast.LENGTH_SHORT).show()
                        onDeleted()
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminActivity, "Failed to delete: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFeedbackRepliesDialog() {
        val feedbackRepo = FeedbackRepository()
        lifecycleScope.launch {
            val posts = feedbackRepo.getFeedbackPosts()
            if (posts.isEmpty()) {
                Toast.makeText(this@AdminActivity, "No feedback posts found.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@AdminActivity, "No feedback posts published yet.", Toast.LENGTH_SHORT).show()
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
        val options = arrayOf("👁️ View Replies", "🗑️ Delete Post")
        MaterialAlertDialogBuilder(this)
            .setTitle(post.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPostRepliesDialog(post)
                    1 -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Delete Post?")
                            .setMessage("Delete '${post.title}' from Home screen? All its student replies will also be permanently deleted.")
                            .setPositiveButton("Delete") { _, _ ->
                                lifecycleScope.launch {
                                    feedbackRepo.deleteFeedbackPost(post.id)
                                    Toast.makeText(this@AdminActivity, "Post and replies deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Back", null)
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
                        Toast.makeText(this@AdminActivity, "Marked as read", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@AdminActivity, "Reply deleted", Toast.LENGTH_SHORT).show()
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
                    lifecycleScope.launch {
                        feedbackRepo.deleteFeedbackPost(post.id)
                        dialog.dismiss()
                        Toast.makeText(this@AdminActivity, "Post and replies deleted", Toast.LENGTH_SHORT).show()
                    }
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
}
