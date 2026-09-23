package com.eve.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.eve.app.data.model.DailyQuestion
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityDailyAdminBinding
import com.eve.app.util.BulkImportHelper
import com.eve.app.util.DateUtil
import com.eve.app.util.QuestionBulkParser
import com.eve.app.util.SpreadsheetReader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DailyAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyAdminBinding
    private val viewModel: DailyAdminViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private var editing: DailyQuestion? = null

    private val bulkPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importBulk(uri)
    }

    private val adapter = DailyQuestionAdapter(
        onEdit = { startEdit(it) },
        onDelete = { confirmDelete(it) }
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
        binding = ActivityDailyAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDate.setText(DateUtil.todayIso())
        binding.spCorrect.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("A", "B", "C", "D")
        )
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLoadDate.setOnClickListener { loadSelectedDate() }
        binding.btnShareTemplate.setOnClickListener {
            BulkImportHelper.shareTemplate(this, BulkImportHelper.DAILY_TEMPLATE)
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
        binding.btnUpload.setOnClickListener { submit() }
        binding.btnCancelEdit.setOnClickListener { cancelEdit() }
        binding.btnToggleHindi.setOnClickListener {
            if (binding.groupHindi.visibility == View.VISIBLE) {
                binding.groupHindi.visibility = View.GONE
                binding.btnToggleHindi.text = "+ Hindi translation add karo (optional)"
            } else {
                binding.groupHindi.visibility = View.VISIBLE
                binding.btnToggleHindi.text = "− Hindi translation hide karo"
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.questions.collect { list ->
                        adapter.submit(list)
                        binding.tvNoQuestions.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvListTitle.text = "Is date ke questions (${list.size})"
                    }
                }
                launch {
                    viewModel.busy.collect {
                        binding.progress.visibility = if (it) View.VISIBLE else View.GONE
                        binding.btnUpload.isEnabled = !it
                        binding.btnBulkUpload.isEnabled = !it
                    }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@DailyAdminActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.consumeMessage()
                        }
                    }
                }
            }
        }
    }

    private fun importBulk(uri: Uri) {
        val fallback = binding.etDate.text.toString().trim().ifBlank { DateUtil.todayIso() }
        try {
            val name = BulkImportHelper.displayName(this, uri)
            val sheet = SpreadsheetReader.read(contentResolver, uri, name)
            val parsed = QuestionBulkParser.parseDailyQuestions(sheet, fallback)
            if (parsed.questions.isEmpty()) {
                val detail = parsed.errors.take(5).joinToString("\n") { "Row ${it.rowNumber}: ${it.reason}" }
                Toast.makeText(this, "Koi valid question nahi mila.\n$detail", Toast.LENGTH_LONG).show()
                return
            }
            val dates = parsed.questions.map { it.date }.distinct().joinToString(", ")
            val errorPreview = if (parsed.errors.isEmpty()) "Koi row skip nahi hui."
            else parsed.errors.take(8).joinToString("\n") { "Row ${it.rowNumber}: ${it.reason}" }
            AlertDialog.Builder(this)
                .setTitle("Bulk Daily GK upload?")
                .setMessage("${parsed.questions.size} questions\nDates: $dates\nSkip: ${parsed.errors.size}\n\n$errorPreview")
                .setPositiveButton("Upload") { _, _ ->
                    viewModel.addAll(parsed.questions) { }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "File padh nahi paye", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSelectedDate() {
        val date = binding.etDate.text.toString().trim()
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            Toast.makeText(this, "Date yyyy-MM-dd format me daalo", Toast.LENGTH_SHORT).show()
            return
        }
        cancelEdit()
        viewModel.load(date)
    }

    private fun startEdit(q: DailyQuestion) {
        editing = q
        binding.tvFormTitle.text = "Question edit karo"
        binding.etDate.setText(q.date)
        binding.etQuestion.setText(q.questionText)
        binding.etTopic.setText(q.topic)
        binding.etOptionA.setText(q.optionA)
        binding.etOptionB.setText(q.optionB)
        binding.etOptionC.setText(q.optionC)
        binding.etOptionD.setText(q.optionD)
        val idx = listOf("A", "B", "C", "D").indexOf(q.correctAnswer)
        if (idx >= 0) binding.spCorrect.setSelection(idx)
        binding.etExplanation.setText(q.explanation)
        binding.etQuestionHi.setText(q.questionTextHi)
        binding.etOptionAHi.setText(q.optionAHi)
        binding.etOptionBHi.setText(q.optionBHi)
        binding.etOptionCHi.setText(q.optionCHi)
        binding.etOptionDHi.setText(q.optionDHi)
        binding.etExplanationHi.setText(q.explanationHi)
        binding.btnUpload.text = "Update Question"
        binding.btnCancelEdit.visibility = View.VISIBLE
    }

    private fun cancelEdit() {
        editing = null
        binding.tvFormTitle.text = "Naya question"
        binding.etQuestion.text?.clear()
        binding.etTopic.text?.clear()
        binding.etOptionA.text?.clear()
        binding.etOptionB.text?.clear()
        binding.etOptionC.text?.clear()
        binding.etOptionD.text?.clear()
        binding.spCorrect.setSelection(0)
        binding.etExplanation.text?.clear()
        binding.etQuestionHi.text?.clear()
        binding.etOptionAHi.text?.clear()
        binding.etOptionBHi.text?.clear()
        binding.etOptionCHi.text?.clear()
        binding.etOptionDHi.text?.clear()
        binding.etExplanationHi.text?.clear()
        binding.btnUpload.text = "Upload Daily GK question"
        binding.btnCancelEdit.visibility = View.GONE
    }

    private fun confirmDelete(q: DailyQuestion) {
        AlertDialog.Builder(this)
            .setTitle("Question delete karein?")
            .setMessage(q.questionText)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(q)
                if (editing?.id == q.id) cancelEdit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submit() {
        val date = binding.etDate.text.toString().trim()
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            Toast.makeText(this, "Date yyyy-MM-dd format me daalo", Toast.LENGTH_SHORT).show()
            return
        }
        val qText = binding.etQuestion.text.toString().trim()
        val a = binding.etOptionA.text.toString().trim()
        val b = binding.etOptionB.text.toString().trim()
        val c = binding.etOptionC.text.toString().trim()
        val d = binding.etOptionD.text.toString().trim()
        if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            Toast.makeText(this, "Saari fields bharo", Toast.LENGTH_SHORT).show()
            return
        }
        val draft = DailyQuestion(
            id = editing?.id.orEmpty(),
            date = date,
            questionText = qText,
            topic = binding.etTopic.text.toString().trim(),
            optionA = a,
            optionB = b,
            optionC = c,
            optionD = d,
            correctAnswer = binding.spCorrect.selectedItem as String,
            explanation = binding.etExplanation.text.toString().trim(),
            questionTextHi = binding.etQuestionHi.text.toString().trim(),
            optionAHi = binding.etOptionAHi.text.toString().trim(),
            optionBHi = binding.etOptionBHi.text.toString().trim(),
            optionCHi = binding.etOptionCHi.text.toString().trim(),
            optionDHi = binding.etOptionDHi.text.toString().trim(),
            explanationHi = binding.etExplanationHi.text.toString().trim()
        )
        val current = editing
        if (current != null) {
            viewModel.update(draft.copy(id = current.id)) { cancelEdit() }
        } else {
            viewModel.add(draft) { cancelEdit() }
        }
    }
}
