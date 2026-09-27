package com.eve.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FlaggedQuestionRepository
import com.eve.app.databinding.ActivityFlaggedQuestionsBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FlaggedQuestionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlaggedQuestionsBinding
    private val adminRepo = AdminRepository()
    private val flagRepo = FlaggedQuestionRepository()
    private val examRepo = ExamRepository()
    private lateinit var adapter: FlaggedQuestionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityFlaggedQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setupUi()
            loadFlaggedQuestions()
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadFlaggedQuestions() }

        adapter = FlaggedQuestionsAdapter(
            onEdit = { item -> showEditDialog(item) },
            onDismiss = { item -> confirmDismissFlag(item) }
        )

        binding.rvFlaggedQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvFlaggedQuestions.adapter = adapter
    }

    private fun loadFlaggedQuestions() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val list = flagRepo.getPendingFlaggedQuestions()
                binding.progressBar.visibility = View.GONE
                adapter.submit(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to load flags: ${e.message}")
            }
        }
    }

    private fun confirmDismissFlag(item: AggregatedFlaggedQuestion) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Dismiss Flag?")
            .setMessage("Dismiss ${item.flagCount} flag(s) for this question? It will be marked as resolved.")
            .setPositiveButton("Dismiss") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val success = flagRepo.dismissFlags(item.flagIds)
                        binding.progressBar.visibility = View.GONE
                        if (success) {
                            AppBulletin.showSuccess(this@FlaggedQuestionsActivity, "Flags dismissed")
                            loadFlaggedQuestions()
                        } else {
                            AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to dismiss flags")
                        }
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@FlaggedQuestionsActivity, "Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(item: AggregatedFlaggedQuestion) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val fullQuestion = try {
                val questions = examRepo.getQuestions(item.examId)
                questions.find { it.id == item.questionId || it.questionText == item.questionText }
            } catch (_: Exception) {
                null
            }
            binding.progressBar.visibility = View.GONE

            val dialogView = LayoutInflater.from(this@FlaggedQuestionsActivity)
                .inflate(R.layout.dialog_add_question, null)
            val etQuestion = dialogView.findViewById<EditText>(R.id.etQuestionText)
            val etOptA = dialogView.findViewById<EditText>(R.id.etOptionA)
            val etOptB = dialogView.findViewById<EditText>(R.id.etOptionB)
            val etOptC = dialogView.findViewById<EditText>(R.id.etOptionC)
            val etOptD = dialogView.findViewById<EditText>(R.id.etOptionD)
            val rgCorrect = dialogView.findViewById<RadioGroup>(R.id.rgCorrectAnswer)
            val etExplanation = dialogView.findViewById<EditText>(R.id.etExplanation)
            val etTopic = dialogView.findViewById<EditText>(R.id.etTopic)

            etQuestion.setText(fullQuestion?.questionText ?: item.questionText)
            etOptA.setText(fullQuestion?.optionA.orEmpty())
            etOptB.setText(fullQuestion?.optionB.orEmpty())
            etOptC.setText(fullQuestion?.optionC.orEmpty())
            etOptD.setText(fullQuestion?.optionD.orEmpty())
            etExplanation.setText(fullQuestion?.explanation.orEmpty())
            etTopic.setText(fullQuestion?.topic.orEmpty())

            when (fullQuestion?.correctAnswer?.uppercase()) {
                "A" -> rgCorrect.check(R.id.rbA)
                "B" -> rgCorrect.check(R.id.rbB)
                "C" -> rgCorrect.check(R.id.rbC)
                "D" -> rgCorrect.check(R.id.rbD)
                else -> rgCorrect.check(R.id.rbA)
            }

            MaterialAlertDialogBuilder(this@FlaggedQuestionsActivity)
                .setTitle("Review & Edit Question")
                .setView(dialogView)
                .setPositiveButton("Save Changes") { _, _ ->
                    val qText = etQuestion.text?.toString()?.trim().orEmpty()
                    val a = etOptA.text?.toString()?.trim().orEmpty()
                    val b = etOptB.text?.toString()?.trim().orEmpty()
                    val c = etOptC.text?.toString()?.trim().orEmpty()
                    val d = etOptD.text?.toString()?.trim().orEmpty()
                    val exp = etExplanation.text?.toString()?.trim().orEmpty()
                    val topic = etTopic.text?.toString()?.trim().orEmpty()

                    val correct = when (rgCorrect.checkedRadioButtonId) {
                        R.id.rbA -> "A"
                        R.id.rbB -> "B"
                        R.id.rbC -> "C"
                        R.id.rbD -> "D"
                        else -> "A"
                    }

                    if (qText.isBlank()) {
                        AppBulletin.showError(this@FlaggedQuestionsActivity, "Question text cannot be empty")
                        return@setPositiveButton
                    }

                    val updated = (fullQuestion ?: Question(id = item.questionId, examId = item.examId)).copy(
                        questionText = qText,
                        optionA = a,
                        optionB = b,
                        optionC = c,
                        optionD = d,
                        correctAnswer = correct,
                        explanation = exp,
                        topic = topic
                    )

                    lifecycleScope.launch {
                        try {
                            binding.progressBar.visibility = View.VISIBLE
                            examRepo.updateQuestion(updated)
                            flagRepo.dismissFlags(item.flagIds)
                            binding.progressBar.visibility = View.GONE
                            AppBulletin.showSuccess(this@FlaggedQuestionsActivity, "Question updated & flags resolved!")
                            loadFlaggedQuestions()
                        } catch (e: Exception) {
                            binding.progressBar.visibility = View.GONE
                            AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to update: ${e.message}")
                        }
                    }
                }
                .setNeutralButton("Open in Exam Editor") { _, _ ->
                    val intent = Intent(this@FlaggedQuestionsActivity, EditExamActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, item.examId)
                        putExtra(Constants.EXTRA_EXAM_NAME, item.examName)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
