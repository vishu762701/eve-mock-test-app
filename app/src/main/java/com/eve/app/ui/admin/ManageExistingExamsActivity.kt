package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.util.Constants
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageExistingExamsBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityManageExistingExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadExams() }

        adapter = ExistingExamsAdapter(
            onExamClick = { exam -> openEditExam(exam) },
            onEditClick = { exam -> openEditExam(exam) },
            onPreviewClick = { exam -> previewExam(exam) },
            onDeleteClick = { exam -> confirmDeleteExam(exam) }
        )

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter

        binding.etSearchExam.doAfterTextChanged { text ->
            filterExams(text?.toString().orEmpty())
        }

        loadExams()
    }

    override fun onResume() {
        super.onResume()
        hasEmptyPlayed = false
        loadExams()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun loadExams() {
        binding.shimmerSkeletonExams.visibility = View.VISIBLE
        binding.emptyGroup.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val exams = examRepo.getExams()
                val attemptsMap = try {
                    val analyticsRes = api.getExamAnalytics()
                    analyticsRes.data?.associate { it.examId to it.attemptCount } ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }

                allExamsWithStats = exams.map { exam ->
                    ExamWithStats(
                        exam = exam,
                        attemptCount = attemptsMap[exam.id] ?: 0L,
                        questionCount = exam.questionCount
                    )
                }.sortedWith(compareBy<ExamWithStats> { it.exam.categoryOrOther }.thenBy { it.exam.examName })

                binding.shimmerSkeletonExams.visibility = View.GONE
                filterExams(binding.etSearchExam.text?.toString().orEmpty())
            } catch (e: Exception) {
                binding.shimmerSkeletonExams.visibility = View.GONE
                AppBulletin.showError(this@ManageExistingExamsActivity, "Failed to load exams: ${e.toUserFriendlyMessage()}")
            }
        }
    }

    private fun filterExams(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allExamsWithStats
        } else {
            allExamsWithStats.filter {
                it.exam.examName.lowercase().contains(q) ||
                        it.exam.categoryOrOther.lowercase().contains(q)
            }
        }

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

    private fun confirmDeleteExam(exam: Exam) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Exam?")
            .setMessage("Are you sure? This cannot be undone. All tests and questions for '${exam.examName}' will be permanently deleted.")
            .setPositiveButton("Delete") { _, _ ->
                val previousList = adapter.currentList.toMutableList()
                val updated = previousList.filter { it.exam.id != exam.id }
                adapter.submitList(updated)

                lifecycleScope.launch {
                    try {
                        examRepo.deleteExam(exam.id)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@ManageExistingExamsActivity, "Failed to delete: ${e.message}")
                    }
                }

                AppUndoBar.show(
                    context = this@ManageExistingExamsActivity,
                    message = "Exam '${exam.examName}' deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        adapter.submitList(previousList)
                        lifecycleScope.launch {
                            try {
                                examRepo.addExam(
                                    name = exam.examName,
                                    minutes = exam.timeLimitMinutes,
                                    category = exam.category,
                                    testNumber = exam.testNumber,
                                    questionCount = exam.questionCount,
                                    autoGenEnabled = exam.autoGenerationEnabled,
                                    autoGenTime = exam.autoGenTime,
                                    timezone = exam.timezone,
                                    generationPrompt = exam.generationPrompt,
                                    imageUrl = exam.imageUrl,
                                    negativeMarkingText = exam.negativeMarkingText,
                                    negativeMarkingValue = exam.negativeMarkingValue,
                                    parentExamId = exam.parentExamId
                                )
                                loadExams()
                                AppBulletin.showSuccess(this@ManageExistingExamsActivity, "Exam restored")
                            } catch (e: Exception) {
                                AppBulletin.showError(this@ManageExistingExamsActivity, "Failed to restore: ${e.message}")
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
