package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityAdminAnalyticsBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminAnalyticsActivity : EveBaseActivity() {
    private lateinit var binding: ActivityAdminAnalyticsBinding
    private val viewModel: AdminAnalyticsViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val examRepo = ExamRepository()
    private val examAdapter = AnalyticsExamAdapter()
    private val questionAdapter = AnalyticsQuestionAdapter(onEdit = { q -> showEditQuestionDialog(q) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setup()
        }
    }

    private fun setup() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.load() }
        binding.btnOpenCrashlyticsConsole.setOnClickListener {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://console.firebase.google.com/u/0/project/_/crashlytics")
                )
                startActivity(intent)
            } catch (e: Exception) {
                AppBulletin.showError(this, "Could not open browser: ${e.message}")
            }
        }

        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            val timeRange = when {
                checkedIds.contains(R.id.chipLast7Days) -> AnalyticsTimeRange.LAST_7_DAYS
                checkedIds.contains(R.id.chipLast30Days) -> AnalyticsTimeRange.LAST_30_DAYS
                else -> AnalyticsTimeRange.ALL_TIME
            }
            viewModel.load(timeRange)
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = examAdapter
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.load()
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            UiState.Loading -> {
                                binding.progress.visibility = View.VISIBLE
                                binding.content.visibility = View.GONE
                            }
                            is UiState.Error -> {
                                binding.progress.visibility = View.GONE
                                binding.content.visibility = View.VISIBLE
                                AppBulletin.showError(this@AdminAnalyticsActivity, state.message)
                            }
                            is UiState.Success -> {
                                binding.progress.visibility = View.GONE
                                binding.content.visibility = View.VISIBLE
                                val data = state.data
                                examAdapter.submit(data.exams)
                                // Weak-question detection requires at least 3 attempted answers to avoid
                                // ranking a question from one unlucky student.
                                questionAdapter.submit(data.questions.filter { it.attempts >= 3 }.take(30))
                                val total = data.exams.sumOf { it.attemptCount }
                                val students = data.exams.sumOf { it.uniqueUsers }
                                binding.tvSummary.text = "Total test submissions: $total • Unique students (sum by exam): $students"
                                val top = data.exams.firstOrNull()
                                binding.tvMostAttempted.text = top?.let {
                                    "Most attempted: ${it.examName} — ${it.attemptCount} attempts"
                                } ?: "Most attempted: —"
                                binding.tvEmpty.visibility =
                                    if (data.exams.isEmpty()) View.VISIBLE else View.GONE

                                val barData = data.exams.map { ExamBarData(it.examName, it.attemptCount.toInt()) }
                                binding.barChartView.setData(barData)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEditQuestionDialog(qa: QuestionAnalytics) {
        val examId = qa.examId
        val targetId = qa.questionId.ifBlank { qa.id }

        lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            val fullQuestion = try {
                val list = examRepo.getQuestions(examId)
                list.find { it.id == targetId || it.questionText == qa.questionText }
            } catch (_: Exception) {
                null
            }
            binding.progress.visibility = View.GONE

            val dialogView = LayoutInflater.from(this@AdminAnalyticsActivity)
                .inflate(R.layout.dialog_add_question, null)
            val etQuestion = dialogView.findViewById<EditText>(R.id.etQuestionText)
            val etOptA = dialogView.findViewById<EditText>(R.id.etOptionA)
            val etOptB = dialogView.findViewById<EditText>(R.id.etOptionB)
            val etOptC = dialogView.findViewById<EditText>(R.id.etOptionC)
            val etOptD = dialogView.findViewById<EditText>(R.id.etOptionD)
            val rgCorrect = dialogView.findViewById<RadioGroup>(R.id.rgCorrectAnswer)
            val etExplanation = dialogView.findViewById<EditText>(R.id.etExplanation)
            val etTopic = dialogView.findViewById<EditText>(R.id.etTopic)

            etQuestion.setText(fullQuestion?.questionText ?: qa.questionText)
            etOptA.setText(fullQuestion?.optionA.orEmpty())
            etOptB.setText(fullQuestion?.optionB.orEmpty())
            etOptC.setText(fullQuestion?.optionC.orEmpty())
            etOptD.setText(fullQuestion?.optionD.orEmpty())
            etExplanation.setText(fullQuestion?.explanation.orEmpty())
            etTopic.setText(fullQuestion?.topic ?: qa.topic)

            when (fullQuestion?.correctAnswer?.uppercase()) {
                "A" -> rgCorrect.check(R.id.rbA)
                "B" -> rgCorrect.check(R.id.rbB)
                "C" -> rgCorrect.check(R.id.rbC)
                "D" -> rgCorrect.check(R.id.rbD)
                else -> rgCorrect.check(R.id.rbA)
            }

            MaterialAlertDialogBuilder(this@AdminAnalyticsActivity)
                .setTitle("Edit Question")
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
                        AppBulletin.showError(this@AdminAnalyticsActivity, "Question text cannot be empty")
                        return@setPositiveButton
                    }

                    val updated = (fullQuestion ?: Question(id = targetId, examId = examId)).copy(
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
                            binding.progress.visibility = View.VISIBLE
                            examRepo.updateQuestion(updated)
                            binding.progress.visibility = View.GONE
                            AppBulletin.showSuccess(this@AdminAnalyticsActivity, "Question updated successfully")
                            viewModel.load()
                        } catch (e: Exception) {
                            binding.progress.visibility = View.GONE
                            AppBulletin.showError(this@AdminAnalyticsActivity, "Failed to update question: ${e.message}")
                        }
                    }
                }
                .setNeutralButton("Open in Exam Editor") { _, _ ->
                    val intent = Intent(this@AdminAnalyticsActivity, EditExamActivity::class.java).apply {
                        putExtra(Constants.EXTRA_EXAM_ID, qa.examId)
                        putExtra(Constants.EXTRA_EXAM_NAME, qa.examName)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
