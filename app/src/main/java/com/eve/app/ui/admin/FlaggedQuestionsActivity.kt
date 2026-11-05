package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FlaggedQuestionRepository
import com.eve.app.databinding.ActivityFlaggedQuestionsBinding
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FlaggedQuestionsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityFlaggedQuestionsBinding
    private val adminRepo = AdminRepository()
    private val flagRepo = FlaggedQuestionRepository()
    private val examRepo = ExamRepository()
    private lateinit var adapter: FlaggedQuestionsAdapter

    private var isTechnicalSelected = false
    private var reportReadGeneration = 0L
    private var actionEpoch = 0L
    private var reportLoadJob: Job? = null
    private var editorLoadJob: Job? = null
    private var editorWriteJob: Job? = null
    private var dismissJob: Job? = null
    private var canManageQuestions = false
    private var canManageExams = false
    private var screenAuthorized = false
    private var uiPrepared = false
    private var pendingAuthorizedStartLoad = false
    private var reauthorizeOnResume = false
    private var editorLoadInFlight = false
    private var loadedReports: List<AggregatedFlaggedQuestion> = emptyList()
    private var questionEditor: TelegramNativeAlertDialog? = null
    private var dismissDialog: TelegramNativeAlertDialog? = null
    private var openingUid = ""
    private var openingEmail = ""

    private fun sameIdentity(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser
        return AdminIdentityGuard.allowed(true, openingUid, openingEmail,
            current?.uid, current?.email) && !isFinishing && !isDestroyed
    }

    private fun activeOwner(): Boolean =
        screenAuthorized && sameIdentity() &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private val questionActionsAllowed: Boolean
        get() = activeOwner() && canManageQuestions && editorWriteJob?.isActive != true

    private fun currentReport(item: AggregatedFlaggedQuestion, uid: String, email: String, epoch: Long): Boolean =
        activeOwner() && uid == openingUid && email.equals(openingEmail, ignoreCase = true) &&
            epoch == actionEpoch && matchesReportType(item) &&
            loadedReports.any {
                it.examId == item.examId && it.questionId == item.questionId &&
                    it.reportType == item.reportType && it.flagIds == item.flagIds &&
                    item.flagIds.isNotEmpty()
            }

    private suspend fun freshRights(uid: String, email: String, vararg rights: String): Boolean {
        if (!activeOwner() || uid != openingUid || !email.equals(openingEmail, ignoreCase = true)) return false
        val snapshot = AdminPermissionSnapshot.load(
            email, "moderate_feedback",
            membership = { adminRepo.isAdminFresh(uid, email) },
            permissions = { adminRepo.getAdminMe() }
        )
        return activeOwner() && rights.all { AdminPermissionSnapshot.allows(snapshot, email, it) }
    }

    private fun authorizeReports() {
        pendingAuthorizedStartLoad = false
        screenAuthorized = false
        canManageQuestions = false
        canManageExams = false
        binding.root.visibility = View.INVISIBLE
        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true,
            requiredRight = "moderate_feedback") { permissions ->
            // Old authorizations resolving after STOP are not a new grant.
            if (reauthorizeOnResume) {
                binding.root.visibility = View.INVISIBLE
                return@requireAdmin
            }
            if (!sameIdentity()) {
                binding.root.visibility = View.INVISIBLE
                finish()
                return@requireAdmin
            }
            screenAuthorized = true
            canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
            canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
            if (!uiPrepared) {
                setupUi()
                uiPrepared = true
            }
            // AdminAccessGate can finish synchronously during onCreate, when the
            // Activity is only CREATED. Preserve the STARTED-only read boundary
            // without silently losing the first report fetch.
            if (activeOwner()) loadFlaggedQuestions()
            else pendingAuthorizedStartLoad = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.email.isNullOrBlank()) {
            finish()
            return
        }
        openingUid = user.uid
        openingEmail = user.email.orEmpty()
        binding = ActivityFlaggedQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isTechnicalSelected = savedInstanceState?.getBoolean("admin_report_technical") ?: false
        authorizeReports()
    }

    override fun onStart() {
        super.onStart()
        if (pendingAuthorizedStartLoad && activeOwner()) {
            pendingAuthorizedStartLoad = false
            loadFlaggedQuestions()
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != openingUid || !live.email.equals(openingEmail, ignoreCase = true)) {
                finish()
                return
            }
            authorizeReports()
        }
        // The lifecycle reaches STARTED after Activity.onStart returns.
        // A synchronous onCreate authorization therefore cannot safely read
        // from inside onStart itself. Execute only after lifecycle dispatch;
        // the pending flag and live UID/permission guard prevent duplicates.
        if (pendingAuthorizedStartLoad) {
            binding.root.post {
                if (pendingAuthorizedStartLoad && activeOwner()) {
                    pendingAuthorizedStartLoad = false
                    loadFlaggedQuestions()
                }
            }
        }
    }

    override fun onStop() {
        pendingAuthorizedStartLoad = false
        reauthorizeOnResume = true
        screenAuthorized = false
        canManageQuestions = false
        canManageExams = false
        ++reportReadGeneration
        ++actionEpoch
        reportLoadJob?.cancel()
        editorLoadJob?.cancel()
        editorLoadInFlight = false
        loadedReports = emptyList()
        questionEditor?.dismiss()
        dismissDialog?.dismiss()
        questionEditor = null
        dismissDialog = null
        if (::adapter.isInitialized) adapter.submit(emptyList())
        if (::binding.isInitialized) binding.root.visibility = View.INVISIBLE
        // Already-dispatched Worker writes may finish, but their original
        // epoch/UID no longer permits any UI acknowledgment or replay.
        super.onStop()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { if (activeOwner()) loadFlaggedQuestions() }

        binding.chipContentIssues.isCheckable = true
        binding.chipTechnicalIssues.isCheckable = true
        binding.chipGroupReportType.isSelectionRequired = true
        binding.chipGroupReportType.check(if (isTechnicalSelected) R.id.chipTechnicalIssues else R.id.chipContentIssues)

        adapter = FlaggedQuestionsAdapter(
            onEdit = { item -> if (questionActionsAllowed && matchesReportType(item)) showEditDialog(item) },
            onDismiss = { item -> if (activeOwner() && matchesReportType(item)) confirmDismissFlag(item) },
            canEdit = { questionActionsAllowed },
            canDismiss = { activeOwner() && dismissJob?.isActive != true }
        )
        binding.rvFlaggedQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvFlaggedQuestions.adapter = adapter
        binding.chipGroupReportType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!activeOwner()) return@setOnCheckedStateChangeListener
            val technical = checkedIds.contains(R.id.chipTechnicalIssues)
            if (technical == isTechnicalSelected) return@setOnCheckedStateChangeListener
            isTechnicalSelected = technical
            questionEditor?.dismiss()
            dismissDialog?.dismiss()
            loadFlaggedQuestions()
        }
    }

    private fun loadFlaggedQuestions() {
        if (!activeOwner()) return
        val technical = isTechnicalSelected
        val requestUid = openingUid
        val request = ++reportReadGeneration
        ++actionEpoch
        reportLoadJob?.cancel()
        loadedReports = emptyList()
        adapter.submit(emptyList())
        fun isCurrent() = activeOwner() && requestUid == openingUid &&
            request == reportReadGeneration && technical == isTechnicalSelected
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        reportLoadJob = lifecycleScope.launch {
            try {
                val list = flagRepo.getPendingFlaggedQuestions(isTechnical = technical)
                if (!isCurrent()) return@launch
                loadedReports = list
                binding.progressBar.visibility = View.GONE
                adapter.submit(list)
                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = if (technical) {
                        getString(R.string.admin_reports_empty_technical)
                    } else {
                        getString(R.string.admin_reports_empty_content)
                    }
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (isCurrent()) {
                    binding.progressBar.visibility = View.GONE
                    AppBulletin.showError(this@FlaggedQuestionsActivity,
                        "Failed to load flags: " + error.message)
                }
            }
        }
    }

    private fun matchesReportType(item: AggregatedFlaggedQuestion) =
        (item.reportType == "technical") == isTechnicalSelected

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("admin_report_technical", isTechnicalSelected)
    }

    private fun confirmDismissFlag(item: AggregatedFlaggedQuestion) {
        val uid = openingUid
        val email = openingEmail
        val epoch = actionEpoch
        if (!currentReport(item, uid, email, epoch) || dismissJob?.isActive == true) return
        val technical = item.reportType == "technical"
        val title = if (technical) "Dismiss Bug Report?" else "Dismiss Flag?"
        val message = if (technical) {
            "Mark this technical issue as resolved? (" + item.flagCount + " report(s))"
        } else {
            "Dismiss " + item.flagCount + " flag(s) for this question? It will be marked as resolved."
        }
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Dismiss") { _, _ ->
                if (!currentReport(item, uid, email, epoch) || dismissJob?.isActive == true) return@setPositiveButton
                dismissJob = lifecycleScope.launch {
                    binding.progressBar.visibility = View.VISIBLE
                    try {
                        if (!freshRights(uid, email, "moderate_feedback") ||
                            !currentReport(item, uid, email, epoch)) {
                            if (currentReport(item, uid, email, epoch)) {
                                AppBulletin.showError(this@FlaggedQuestionsActivity, "Report moderation right is no longer available.")
                            }
                            return@launch
                        }
                        val confirmed = flagRepo.dismissFlags(item.flagIds, isTechnical = technical)
                        if (!currentReport(item, uid, email, epoch)) return@launch
                        if (confirmed) {
                            AppBulletin.showSuccess(this@FlaggedQuestionsActivity, "Reports dismissed")
                            loadFlaggedQuestions()
                        } else {
                            AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to dismiss reports")
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (error: Exception) {
                        if (currentReport(item, uid, email, epoch)) {
                            AppBulletin.showError(this@FlaggedQuestionsActivity, "Error: " + error.message)
                        }
                    } finally {
                        if (currentReport(item, uid, email, epoch)) binding.progressBar.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        dismissDialog = dialog
        dialog.setOnDismissListener { if (dismissDialog === dialog) dismissDialog = null }
    }

    private fun showEditDialog(item: AggregatedFlaggedQuestion) {
        val uid = openingUid
        val email = openingEmail
        val epoch = actionEpoch
        if (!questionActionsAllowed || !currentReport(item, uid, email, epoch) ||
            editorLoadInFlight || questionEditor?.isShowing == true) return
        editorLoadInFlight = true
        editorLoadJob = lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val question = resolveQuestionEditorTarget(examRepo.getQuestions(item.examId),
                    item.examId, item.questionId, item.questionText)
                if (!currentReport(item, uid, email, epoch) || !questionActionsAllowed) return@launch
                if (question == null) {
                    AppBulletin.showError(this@FlaggedQuestionsActivity,
                        "Question is unavailable or ambiguous. Reload before editing.")
                    return@launch
                }
                val dialog = createQuestionEditorDialog(this@FlaggedQuestionsActivity,
                    com.eve.app.data.model.Exam(id = question.examId, examName = item.examName),
                    isBusy = {
                        !currentReport(item, uid, email, epoch) ||
                            !questionActionsAllowed || editorWriteJob?.isActive == true
                    },
                    title = "Review & Edit Question", initialQuestion = question,
                    positiveLabel = "Save Changes", submittingLabel = R.string.question_form_saving,
                    successMessage = "Question updated & flags resolved!",
                    neutralLabel = if (canManageExams) "Open in Exam Editor" else null,
                    onNeutral = {
                        if (canManageExams && currentReport(item, uid, email, epoch)) {
                            startActivity(Intent(this@FlaggedQuestionsActivity, EditExamActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, item.examId)
                                putExtra(Constants.EXTRA_EXAM_NAME, item.examName)
                            })
                        }
                    }) { updated, onFailure, onDone ->
                    if (questionActionsAllowed && currentReport(item, uid, email, epoch) &&
                        editorWriteJob?.isActive != true) {
                        editorWriteJob = lifecycleScope.launch {
                            binding.progressBar.visibility = View.VISIBLE
                            var questionSaved = false
                            var confirmed = false
                            try {
                                if (!freshRights(uid, email, "moderate_feedback", "manage_questions") ||
                                    !currentReport(item, uid, email, epoch)) {
                                    if (currentReport(item, uid, email, epoch)) onFailure("Question edit permission was revoked.")
                                    return@launch
                                }
                                examRepo.updateQuestion(updated)
                                questionSaved = true
                                if (!currentReport(item, uid, email, epoch)) return@launch
                                confirmed = flagRepo.dismissFlags(item.flagIds)
                                if (!confirmed && currentReport(item, uid, email, epoch)) {
                                    onFailure("Question updated, but reports were not resolved. Retry or refresh reports.")
                                }
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (error: Exception) {
                                if (currentReport(item, uid, email, epoch)) {
                                    onFailure(if (questionSaved)
                                        "Question updated, but reports could not be resolved. Retry or refresh reports."
                                    else error.message ?: "Failed to update question")
                                }
                            } finally {
                                if (currentReport(item, uid, email, epoch)) binding.progressBar.visibility = View.GONE
                            }
                            if (confirmed && currentReport(item, uid, email, epoch)) {
                                onDone()
                                loadFlaggedQuestions()
                            }
                        }
                    }
                }
                if (!currentReport(item, uid, email, epoch)) return@launch
                questionEditor = dialog
                dialog.setOnDismissListener { if (questionEditor === dialog) questionEditor = null }
                dialog.show()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (currentReport(item, uid, email, epoch)) {
                    AppBulletin.showError(this@FlaggedQuestionsActivity,
                        error.message ?: "Failed to load question")
                }
            } finally {
                if (epoch == actionEpoch) {
                    editorLoadInFlight = false
                    if (activeOwner()) binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        ++reportReadGeneration
        ++actionEpoch
        reportLoadJob?.cancel()
        editorLoadJob?.cancel()
        questionEditor?.dismiss()
        dismissDialog?.dismiss()
        questionEditor = null
        dismissDialog = null
        super.onDestroy()
    }
}
