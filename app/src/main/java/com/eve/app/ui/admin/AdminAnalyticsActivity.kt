package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityAdminAnalyticsBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminAnalyticsActivity : EveBaseActivity() {
    private lateinit var binding: ActivityAdminAnalyticsBinding
    private val viewModel: AdminAnalyticsViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val examRepo = ExamRepository()
    private val examAdapter = AnalyticsExamAdapter()
    private var canManageQuestions = false
    private var canManageExams = false
    private var editorLoadInFlight = false
    private var questionEditor: TelegramNativeAlertDialog? = null
    private val questionActionsAllowed: Boolean
        get() = canManageQuestions && !isFinishing && !isDestroyed
    private val questionAdapter = AnalyticsQuestionAdapter(onEdit = { q -> showEditQuestionDialog(q) }, canEdit = { questionActionsAllowed })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore the visible range before STARTED can issue its first protected read.
        savedInstanceState?.getString("admin_analytics_range")?.let { name ->
            binding.chipGroupTimeRange.check(when (name) {
                AnalyticsTimeRange.LAST_7_DAYS.name -> R.id.chipLast7Days
                AnalyticsTimeRange.LAST_30_DAYS.name -> R.id.chipLast30Days
                else -> R.id.chipAllTime
            })
        }

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "view_analytics_logs") { permissions ->
            canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
            canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
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

        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, _ ->
            viewModel.load(selectedTimeRange())
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = examAdapter
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.load(selectedTimeRange())
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

    private fun selectedTimeRange(): AnalyticsTimeRange = when (binding.chipGroupTimeRange.checkedChipId) {
        R.id.chipLast7Days -> AnalyticsTimeRange.LAST_7_DAYS
        R.id.chipLast30Days -> AnalyticsTimeRange.LAST_30_DAYS
        else -> AnalyticsTimeRange.ALL_TIME
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("admin_analytics_range", selectedTimeRange().name)
    }

    private fun showEditQuestionDialog(qa: QuestionAnalytics) {
        if (!questionActionsAllowed || editorLoadInFlight || questionEditor?.isShowing == true) return
        editorLoadInFlight = true
        lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            try {
                val fullQuestion = resolveQuestionEditorTarget(examRepo.getQuestions(qa.examId),
                    qa.examId, qa.questionId, qa.questionText, legacyId = qa.id)
                if (!questionActionsAllowed) return@launch
                if (fullQuestion == null) {
                    AppBulletin.showError(this@AdminAnalyticsActivity, "Question is unavailable or ambiguous. Reload before editing.")
                    return@launch
                }
                val dialog = createQuestionEditorDialog(this@AdminAnalyticsActivity,
                    com.eve.app.data.model.Exam(id = fullQuestion.examId, examName = qa.examName),
                    isBusy = { !questionActionsAllowed }, title = "Edit Question", initialQuestion = fullQuestion,
                    positiveLabel = "Save Changes", submittingLabel = R.string.question_form_saving,
                    successMessage = "Question updated successfully",
                    neutralLabel = if (canManageExams) "Open in Exam Editor" else null,
                    onNeutral = {
                        if (canManageExams && !isFinishing && !isDestroyed) {
                            startActivity(Intent(this@AdminAnalyticsActivity, EditExamActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, qa.examId)
                                putExtra(Constants.EXTRA_EXAM_NAME, qa.examName)
                            })
                        }
                    }) { updated, onFailure, onDone ->
                        lifecycleScope.launch {
                            binding.progress.visibility = View.VISIBLE
                            var confirmed = false
                            try {
                                examRepo.updateQuestion(updated)
                                confirmed = true
                            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (error: Exception) { onFailure(error.message ?: "Failed to update question") }
                            finally { binding.progress.visibility = View.GONE }
                            if (confirmed && questionActionsAllowed) { onDone(); viewModel.load() }
                        }
                }
                questionEditor = dialog
                dialog.setOnDismissListener { if (questionEditor === dialog) questionEditor = null }
                dialog.show()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                if (!isFinishing && !isDestroyed) AppBulletin.showError(this@AdminAnalyticsActivity,
                    error.message ?: "Failed to load question")
            } finally { editorLoadInFlight = false; binding.progress.visibility = View.GONE }
        }
    }

    override fun onDestroy() {
        questionEditor?.dismiss(); questionEditor = null
        super.onDestroy()
    }
}
