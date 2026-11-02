package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FlaggedQuestionRepository
import com.eve.app.databinding.ActivityFlaggedQuestionsBinding
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FlaggedQuestionsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityFlaggedQuestionsBinding
    private val adminRepo = AdminRepository()
    private val flagRepo = FlaggedQuestionRepository()
    private val examRepo = ExamRepository()
    private lateinit var adapter: FlaggedQuestionsAdapter
    private var isTechnicalSelected: Boolean = false
    private var canManageQuestions = false
    private var canManageExams = false
    private var editorLoadInFlight = false
    private var questionEditor: TelegramNativeAlertDialog? = null
    private val questionActionsAllowed: Boolean
        get() = canManageQuestions && !isFinishing && !isDestroyed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlaggedQuestionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "moderate_feedback") { permissions ->
            canManageQuestions = permissions.isOwner || "manage_questions" in permissions.rights
            canManageExams = permissions.isOwner || "manage_exams" in permissions.rights
            setupUi()
            loadFlaggedQuestions()
        }
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadFlaggedQuestions() }

        binding.chipGroupReportType.setOnCheckedStateChangeListener { _, checkedIds ->
            isTechnicalSelected = (checkedIds.firstOrNull() == binding.chipTechnicalIssues.id)
            loadFlaggedQuestions()
        }

        adapter = FlaggedQuestionsAdapter(
            onEdit = { item -> showEditDialog(item) },
            onDismiss = { item -> confirmDismissFlag(item) },
            canEdit = { questionActionsAllowed },
            canDismiss = { !isFinishing && !isDestroyed }
        )

        binding.rvFlaggedQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvFlaggedQuestions.adapter = adapter
    }

    private fun loadFlaggedQuestions() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val list = flagRepo.getPendingFlaggedQuestions(isTechnical = isTechnicalSelected)
                binding.progressBar.visibility = View.GONE
                adapter.submit(list)
                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = if (isTechnicalSelected) {
                        "🎉 No pending technical / bug reports!\nAll systems operational."
                    } else {
                        "🎉 No pending flagged questions!\nAll reported issues have been resolved."
                    }
                } else {
                    binding.tvEmpty.visibility = View.GONE
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to load flags: ${e.message}")
            }
        }
    }

    private fun confirmDismissFlag(item: AggregatedFlaggedQuestion) {
        if (isFinishing || isDestroyed) return
        val title = if (isTechnicalSelected) "Dismiss Bug Report?" else "Dismiss Flag?"
        val msg = if (isTechnicalSelected) {
            "Mark this technical issue as resolved? (${item.flagCount} report(s))"
        } else {
            "Dismiss ${item.flagCount} flag(s) for this question? It will be marked as resolved."
        }

        TelegramNativeAlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Dismiss") { _, _ ->
                if (isFinishing || isDestroyed) return@setPositiveButton
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try {
                        val success = flagRepo.dismissFlags(item.flagIds, isTechnical = isTechnicalSelected)
                        binding.progressBar.visibility = View.GONE
                        if (success) {
                            AppBulletin.showSuccess(this@FlaggedQuestionsActivity, "Reports dismissed")
                            loadFlaggedQuestions()
                        } else {
                            AppBulletin.showError(this@FlaggedQuestionsActivity, "Failed to dismiss reports")
                        }
                    } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        AppBulletin.showError(this@FlaggedQuestionsActivity, "Error: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(item: AggregatedFlaggedQuestion) {
        if (!questionActionsAllowed || editorLoadInFlight || questionEditor?.isShowing == true) return
        editorLoadInFlight = true
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val fullQuestion = resolveQuestionEditorTarget(examRepo.getQuestions(item.examId),
                    item.examId, item.questionId, item.questionText)
                if (!questionActionsAllowed) return@launch
                if (fullQuestion == null) {
                    AppBulletin.showError(this@FlaggedQuestionsActivity, "Question is unavailable or ambiguous. Reload before editing.")
                    return@launch
                }
                val dialog = createQuestionEditorDialog(this@FlaggedQuestionsActivity,
                    com.eve.app.data.model.Exam(id = fullQuestion.examId, examName = item.examName),
                    isBusy = { !questionActionsAllowed }, title = "Review & Edit Question", initialQuestion = fullQuestion,
                    positiveLabel = "Save Changes", submittingLabel = R.string.question_form_saving,
                    successMessage = "Question updated & flags resolved!",
                    neutralLabel = if (canManageExams) "Open in Exam Editor" else null,
                    onNeutral = {
                        if (canManageExams && !isFinishing && !isDestroyed) {
                            startActivity(Intent(this@FlaggedQuestionsActivity, EditExamActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, item.examId)
                                putExtra(Constants.EXTRA_EXAM_NAME, item.examName)
                            })
                        }
                    }) { updated, onFailure, onDone ->
                        lifecycleScope.launch {
                            binding.progressBar.visibility = View.VISIBLE
                            var questionSaved = false
                            var confirmed = false
                            try {
                                examRepo.updateQuestion(updated)
                                questionSaved = true
                                confirmed = flagRepo.dismissFlags(item.flagIds)
                                if (!confirmed) onFailure("Question updated, but reports were not resolved. Retry or refresh reports.")
                            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (error: Exception) {
                                onFailure(if (questionSaved) "Question updated, but reports could not be resolved. Retry or refresh reports."
                                    else error.message ?: "Failed to update question")
                            } finally { binding.progressBar.visibility = View.GONE }
                            if (confirmed && questionActionsAllowed) { onDone(); loadFlaggedQuestions() }
                        }
                }
                questionEditor = dialog
                dialog.setOnDismissListener { if (questionEditor === dialog) questionEditor = null }
                dialog.show()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                if (!isFinishing && !isDestroyed) AppBulletin.showError(this@FlaggedQuestionsActivity,
                    error.message ?: "Failed to load question")
            } finally { editorLoadInFlight = false; binding.progressBar.visibility = View.GONE }
        }
    }

    override fun onDestroy() {
        questionEditor?.dismiss(); questionEditor = null
        super.onDestroy()
    }
}
