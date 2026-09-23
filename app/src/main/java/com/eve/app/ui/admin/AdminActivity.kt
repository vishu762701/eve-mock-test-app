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
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityAdminBinding
import com.eve.app.util.BulkImportHelper
import com.eve.app.util.Constants
import com.eve.app.util.QuestionBulkParser
import com.eve.app.util.SpreadsheetReader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()

    private var exams: List<Exam> = emptyList()

    private val bulkPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importBulk(uri)
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

        binding.btnAddExam.setOnClickListener { addExam() }
        binding.btnDeleteExam.setOnClickListener { confirmDeleteExam() }
        binding.btnAddAdmin.setOnClickListener { addAdmin() }

        binding.btnAnalyticsAdmin.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }
        binding.btnDailyGkAdmin.setOnClickListener {
            startActivity(Intent(this, DailyAdminActivity::class.java))
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

        binding.btnShareTemplate.setOnClickListener {
            BulkImportHelper.shareTemplate(this, BulkImportHelper.EXAM_TEMPLATE)
        }
        binding.btnBulkUpload.setOnClickListener {
            bulkPicker.launch(
                arrayOf(
                    "text/*",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream"
                )
            )
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
                        binding.btnDeleteExam.isEnabled = !busy
                        binding.btnBulkUpload.isEnabled = !busy
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
        if (name.isEmpty() || minutes == null || minutes <= 0) {
            Toast.makeText(this, "Please enter exam name and valid duration in minutes", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isEmpty()) {
            Toast.makeText(this, "Please select or type a category", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addExam(name, minutes, category) {
            binding.etExamName.text?.clear()
            binding.etExamMinutes.text?.clear()
            binding.spCategory.setSelection(0)
            binding.etOtherCategory.text?.clear()
            binding.etOtherCategory.visibility = View.GONE
        }
    }

    private fun confirmDeleteExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition) ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Exam?")
            .setMessage("Exam '${exam.examName}' and all its questions will be permanently deleted.")
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

    private fun importBulk(uri: Uri) {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition)
        if (exam == null) {
            Toast.makeText(this, "Please select or create an exam first", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val name = BulkImportHelper.displayName(this, uri)
            val sheet = SpreadsheetReader.read(contentResolver, uri, name)
            val parsed = QuestionBulkParser.parseExamQuestions(sheet, exam.id)
            if (parsed.questions.isEmpty()) {
                val detail = parsed.errors.take(5).joinToString("\n") { "Row ${it.rowNumber}: ${it.reason}" }
                Toast.makeText(this, "No valid questions found.\n$detail", Toast.LENGTH_LONG).show()
                return
            }
            val errorPreview = if (parsed.errors.isEmpty()) "No rows were skipped."
            else parsed.errors.take(8).joinToString("\n") { "Row ${it.rowNumber}: ${it.reason}" } +
                if (parsed.errors.size > 8) "\n… +${parsed.errors.size - 8} more" else ""
            AlertDialog.Builder(this)
                .setTitle("Bulk Upload Questions")
                .setMessage(
                    "Uploading ${parsed.questions.size} questions to '${exam.examName}'.\n" +
                        "Skipped: ${parsed.errors.size}\n\n$errorPreview"
                )
                .setPositiveButton("Upload") { _, _ ->
                    viewModel.addQuestions(parsed.questions) { }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Failed to read spreadsheet", Toast.LENGTH_LONG).show()
        }
    }
}
