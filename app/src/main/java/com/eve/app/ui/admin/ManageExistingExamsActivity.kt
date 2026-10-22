package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DefaultItemAnimator
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.util.Constants
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageExistingExamsBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.EmptyStateAnimationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ManageExistingExamsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityManageExistingExamsBinding
    private val examRepo = ExamRepository()
    private val api = ApiClient.apiService
    private lateinit var adapter: ExistingExamsAdapter

    private var allExamsWithStats: List<ExamWithStats> = emptyList()
    private var hasEmptyPlayed = false
    private var adminAuthorized = false
    private val expandedExamIds = linkedSetOf<String>()
    private var initializedTreeExpansion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        initializedTreeExpansion = savedInstanceState?.getBoolean("tree_expansion_initialized") ?: false
        expandedExamIds.addAll(savedInstanceState?.getStringArrayList("expanded_exam_ids").orEmpty())
        binding = ActivityManageExistingExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        AdminAccessGate.requireAdmin(this, binding.root) {
            adminAuthorized = true
            setupAdminControls()
            loadExams()
        }
    }

    private fun setupAdminControls() {
        binding.btnRefresh.setOnClickListener { loadExams() }

        adapter = ExistingExamsAdapter(
            onExamClick = { exam -> openEditExam(exam) },
            onEditClick = { exam -> openEditExam(exam) },
            onPreviewClick = { exam -> previewExam(exam) },
            onDeleteClick = { exam -> confirmDeleteExam(exam) },
            onExpandToggle = { exam ->
                if (!expandedExamIds.add(exam.id)) expandedExamIds.remove(exam.id)
                filterExams(binding.etSearchExam.text?.toString().orEmpty())
            }
        )

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter
        (binding.rvExams.itemAnimator as? DefaultItemAnimator)?.apply {
            addDuration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
            removeDuration = UiArcMotion.Duration.FAST
        }

        binding.etSearchExam.doAfterTextChanged { text ->
            filterExams(text?.toString().orEmpty())
        }
    }

    override fun onResume() {
        super.onResume()
        if (adminAuthorized) {
            hasEmptyPlayed = false
            loadExams()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putBoolean("tree_expansion_initialized", initializedTreeExpansion)
        outState.putStringArrayList("expanded_exam_ids", ArrayList(expandedExamIds))
    }

    private var listLoadJob: kotlinx.coroutines.Job? = null

    private fun loadExams() {
        binding.loadingExams.visibility = View.VISIBLE
        binding.emptyGroup.visibility = View.GONE

        listLoadJob?.cancel()
        listLoadJob = lifecycleScope.launch {
            try {
                val exams = examRepo.getExams()
                val attemptsMap = try {
                    val analyticsRes = api.getExamAnalytics()
                    analyticsRes.data?.associate { it.examId to it.attemptCount } ?: emptyMap()
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) {
                    emptyMap()
                }

                allExamsWithStats = exams.map { exam ->
                    ExamWithStats(
                        exam = exam,
                        attemptCount = attemptsMap[exam.id] ?: 0L,
                        questionCount = exam.questionCount
                    )
                }.sortedWith(compareBy<ExamWithStats> { it.exam.categoryOrOther }.thenBy { it.exam.examName })
                val realIds = allExamsWithStats.map { it.exam.id }.toSet()
                expandedExamIds.retainAll(realIds)
                if (!initializedTreeExpansion) {
                    expandedExamIds.addAll(allExamsWithStats.filter {
                        it.exam.parentExamId.isBlank()
                    }.map { it.exam.id })
                    initializedTreeExpansion = true
                }

                binding.loadingExams.visibility = View.GONE
                filterExams(binding.etSearchExam.text?.toString().orEmpty())
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                binding.loadingExams.visibility = View.GONE
                AppBulletin.showError(this@ManageExistingExamsActivity, "Failed to load exams: ${e.toUserFriendlyMessage()}")
            }
        }
    }

    private fun filterExams(query: String) {
        val filtered = ExamTree.flatten(allExamsWithStats, expandedExamIds, query)
        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.emptyGroup.visibility = View.VISIBLE
            binding.rvExams.visibility = View.GONE
            hasEmptyPlayed = EmptyStateAnimationHelper.showEmptyState(binding.lottieEmpty, hasEmptyPlayed)
        } else {
            binding.emptyGroup.visibility = View.GONE
            binding.rvExams.visibility = View.VISIBLE
            EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
        }
    }

    private fun openEditExam(exam: Exam) {
        val intent = Intent(this, EditExamActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, exam.id)
            putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
        }
        startActivity(intent)
    }

    private fun previewExam(exam: Exam) {
        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, exam.id)
            putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
            putExtra("is_preview", true)
        }
        startActivity(intent)
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeleteExam(exam: Exam) {
        deleteFlow.confirm(exam.id, exam.examName, exam = true) { loadExams() }
    }
}
