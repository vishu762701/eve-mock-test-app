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
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminPermissionSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var editorLoadJob: Job? = null
    private var editorSaveJob: Job? = null
    private var editorEpoch = 0L
    private var screenAuthorized = false
    private var screenInitialized = false
    private var reauthorizeOnResume = false
    private var openingUid = ""
    private var openingEmail = ""
    private var questionEditor: TelegramNativeAlertDialog? = null

    private fun currentIdentity(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser
        return AdminIdentityGuard.allowed(true, openingUid, openingEmail, current?.uid, current?.email) &&
            !isFinishing && !isDestroyed
    }

    private fun currentAccess(): Boolean =
        screenAuthorized && currentIdentity() &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private val questionActionsAllowed: Boolean
        get() = currentAccess() && canManageQuestions

    private fun authorizeAnalytics() {
        screenAuthorized = false
        canManageQuestions = false
        canManageExams = false
        binding.progress.visibility = View.VISIBLE
        binding.content.visibility = View.GONE
        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true,
            requiredRight = "view_analytics_logs") { permissions ->
            if (!currentIdentity()) {
                binding.root.visibility = View.INVISIBLE
                finish()
                return@requireAdmin
            }
            screenAuthorized = true
            canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
            canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
            if (!screenInitialized) {
                setup()
                screenInitialized = true
            }
            viewModel.load(selectedTimeRange())
        }
    }

    override fun onStop() {
        screenAuthorized = false
        canManageQuestions = false
        canManageExams = false
        reauthorizeOnResume = true
        editorEpoch++
        editorLoadJob?.cancel()
        editorSaveJob?.cancel()
        editorLoadJob = null
        editorSaveJob = null
        editorLoadInFlight = false
        questionEditor?.dismiss()
        questionEditor = null
        if (::binding.isInitialized) binding.root.visibility = View.INVISIBLE
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            authorizeAnalytics()
        }
    }
    private val questionAdapter = AnalyticsQuestionAdapter(onEdit = { q -> showEditQuestionDialog(q) }, canEdit = { questionActionsAllowed })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startingUser = FirebaseAuth.getInstance().currentUser
        if (startingUser == null || startingUser.email.isNullOrBlank()) {
            finish()
            return
        }
        openingUid = startingUser.uid
        openingEmail = startingUser.email.orEmpty()
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        // Restore the visible range before STARTED can issue its first protected read.
        savedInstanceState?.getString("admin_analytics_range")?.let { name ->
            binding.chipGroupTimeRange.check(when (name) {
                AnalyticsTimeRange.LAST_7_DAYS.name -> R.id.chipLast7Days
                AnalyticsTimeRange.LAST_30_DAYS.name -> R.id.chipLast30Days
                else -> R.id.chipAllTime
            })
        }

        // Install once from onCreate, not via setup() reached by onResume.
        // repeatOnLifecycle manages start/stop collection; currentAccess() keeps
        // the same server-authorized account as the only eligible publisher.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        if (!currentAccess()) return@collect
                        when (state) {
                            UiState.Loading -> {
                                if (!binding.swipeRefresh.isRefreshing) {
                                    binding.progress.visibility = View.VISIBLE
                                    binding.content.visibility = View.GONE
                                }
                            }
                            is UiState.Error -> {
                                binding.swipeRefresh.isRefreshing = false
                                binding.progress.visibility = View.GONE
                                binding.content.visibility = View.VISIBLE
                                AppBulletin.showError(this@AdminAnalyticsActivity, state.message)
                            }
                            is UiState.Success -> {
                                binding.swipeRefresh.isRefreshing = false
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
        authorizeAnalytics()
    }

    private fun setup() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { if (currentAccess()) viewModel.load(selectedTimeRange()) }
        binding.swipeRefresh.setOnRefreshListener { if (currentAccess()) viewModel.load(selectedTimeRange()) }
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
            if (currentAccess()) viewModel.load(selectedTimeRange())
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = examAdapter
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter


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
        if (qa.examId.isBlank()) return
        editorLoadInFlight = true
        val requestEpoch = ++editorEpoch
        val requestUid = openingUid
        val requestEmail = openingEmail
        editorLoadJob = lifecycleScope.launch {
            binding.progress.visibility = View.VISIBLE
            try {
                val fullQuestion = resolveQuestionEditorTarget(examRepo.getQuestions(qa.examId),
                    qa.examId, qa.questionId, qa.questionText, legacyId = qa.id)
                if (requestEpoch != editorEpoch || !questionActionsAllowed) return@launch
                if (fullQuestion == null) {
                    AppBulletin.showError(this@AdminAnalyticsActivity,
                        "Question is unavailable or ambiguous. Reload before editing.")
                    return@launch
                }
                val dialog = createQuestionEditorDialog(this@AdminAnalyticsActivity,
                    com.eve.app.data.model.Exam(id = fullQuestion.examId, examName = qa.examName),
                    isBusy = { !questionActionsAllowed || editorSaveJob?.isActive == true },
                    title = "Edit Question", initialQuestion = fullQuestion,
                    positiveLabel = "Save Changes", submittingLabel = R.string.question_form_saving,
                    successMessage = "Question updated successfully",
                    neutralLabel = if (canManageExams) "Open in Exam Editor" else null,
                    onNeutral = {
                        if (currentAccess() && canManageExams) {
                            startActivity(Intent(this@AdminAnalyticsActivity, EditExamActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, qa.examId)
                                putExtra(Constants.EXTRA_EXAM_NAME, qa.examName)
                            })
                        }
                    }) { updated, onFailure, onDone ->
                        if (!questionActionsAllowed || editorSaveJob?.isActive == true) {
                            onFailure("Question editor authorization is unavailable. Reopen and retry.")
                        } else {
                            editorSaveJob = lifecycleScope.launch {
                                binding.progress.visibility = View.VISIBLE
                                try {
                                    // Cached page rights are not permission to mutate after revocation.
                                    val fresh = AdminPermissionSnapshot.load(
                                        requestEmail, "manage_questions",
                                        membership = { adminRepo.isAdminFresh(requestUid, requestEmail) },
                                        permissions = { adminRepo.getAdminMe() }
                                    )
                                    if (fresh == null || requestEpoch != editorEpoch ||
                                        !questionActionsAllowed || !currentIdentity()) {
                                        if (requestEpoch == editorEpoch && currentAccess()) {
                                            onFailure("Question permission expired. Reopen and retry.")
                                        }
                                        return@launch
                                    }
                                    examRepo.updateQuestion(updated)
                                    if (requestEpoch == editorEpoch && questionActionsAllowed) {
                                        onDone()
                                        viewModel.load(selectedTimeRange())
                                    }
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } catch (error: Exception) {
                                    if (requestEpoch == editorEpoch && questionActionsAllowed) {
                                        onFailure(error.message ?: "Failed to update question")
                                    }
                                } finally {
                                    if (requestEpoch == editorEpoch && ::binding.isInitialized) {
                                        binding.progress.visibility = View.GONE
                                        editorSaveJob = null
                                    }
                                }
                            }
                        }
                    }
                questionEditor = dialog
                dialog.setOnDismissListener {
                    if (questionEditor === dialog) questionEditor = null
                }
                if (requestEpoch == editorEpoch && questionActionsAllowed) dialog.show()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (requestEpoch == editorEpoch && currentAccess()) {
                    AppBulletin.showError(this@AdminAnalyticsActivity,
                        error.message ?: "Failed to load question")
                }
            } finally {
                if (requestEpoch == editorEpoch) {
                    editorLoadInFlight = false
                    binding.progress.visibility = View.GONE
                    editorLoadJob = null
                }
            }
        }
    }

    override fun onDestroy() {
        questionEditor?.dismiss(); questionEditor = null
        super.onDestroy()
    }
}
