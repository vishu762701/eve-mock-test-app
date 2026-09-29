package com.eve.app.ui.home

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityExamTestsBinding
import com.eve.app.ui.common.ErrorStateView
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AttemptKey
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ExamTestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExamTestsBinding
    private val examRepo = ExamRepository()
    private lateinit var adapter: ExamTestsAdapter

    private var examId: String = ""
    private var examName: String = ""
    private var currentExam: Exam? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.eve_bg)
        binding = ActivityExamTestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()

        binding.tvTitle.text = examName.ifBlank { "Tests" }
        binding.btnBack.setOnClickListener { finish() }

        binding.compactErrorView.displayMode = ErrorStateView.DisplayMode.COMPACT

        adapter = ExamTestsAdapter(
            onSubExamClick = { subExam ->
                val intent = Intent(this, ExamTestsActivity::class.java).apply {
                    putExtra(Constants.EXTRA_EXAM_ID, subExam.id)
                    putExtra(Constants.EXTRA_EXAM_NAME, subExam.examName)
                }
                startActivity(intent)
            },
            onTestClick = { test, isCompleted ->
                handleTestClick(test, isCompleted)
            }
        )

        binding.rvTests.layoutManager = LinearLayoutManager(this)
        val spacingPx = (12 * resources.displayMetrics.density).toInt()
        binding.rvTests.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                if (position > 0) {
                    outRect.top = spacingPx
                }
            }
        })
        binding.rvTests.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.compactErrorView.hide()
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val allExams = examRepo.getExams()
                currentExam = allExams.find { it.id == examId }
                if (currentExam != null && examName.isBlank()) {
                    examName = currentExam!!.examName
                    binding.tvTitle.text = examName
                }

                val children = allExams.filter { it.parentExamId == examId }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.examName })

                if (children.isNotEmpty()) {
                    // SUB-EXAM MODE
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvTests.visibility = View.VISIBLE
                    adapter.submitList(children.map { ExamTestsListItem.SubExamItem(it) })
                } else {
                    // TEST MODE
                    val liveTests = examRepo.getLiveGeneratedTests(examId)
                    binding.progressBar.visibility = View.GONE

                    if (liveTests.isEmpty()) {
                        binding.rvTests.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                        adapter.submitList(emptyList())
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvTests.visibility = View.VISIBLE

                        val sortedTests = liveTests.sortedWith(
                            compareBy<GeneratedTest> {
                                extractEndingInteger(it.testNumber) ?: Int.MAX_VALUE
                            }.thenBy { it.generatedAt }
                        )

                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        val attemptedLocks = if (uid.isNotBlank()) {
                            try { examRepo.getAttemptedExamIds(uid) } catch (_: Exception) { emptyList() }
                        } else emptyList()

                        val durationMinutes = currentExam?.timeLimitMinutes ?: 30

                        val testItems = sortedTests.map { test ->
                            val testKey = AttemptKey.forTest(examId, test.id)
                            val isCompleted = attemptedLocks.contains(testKey) || HomeViewModel.isAttemptSubmitted(testKey)
                            val qCount = if (test.questionCount > 0) test.questionCount else test.questions.size
                            val subtitle = "$qCount questions \u2022 $durationMinutes minutes"
                            val title = test.testNumber.ifBlank { "Test" }

                            ExamTestsListItem.TestItem(
                                test = test,
                                title = title,
                                subtitle = subtitle,
                                isCompleted = isCompleted
                            )
                        }

                        adapter.submitList(testItems)
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.compactErrorView.show(
                    type = ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Couldn't load tests: ${e.localizedMessage}",
                    onRetry = { loadData() }
                )
            }
        }
    }

    private fun handleTestClick(test: GeneratedTest, isCompleted: Boolean) {
        val testKey = AttemptKey.forTest(examId, test.id)
        val title = if (test.displayTitle.isNotBlank()) {
            test.displayTitle
        } else {
            "$examName - ${test.testNumber}"
        }

        if (isCompleted) {
            ExamLaunchHelper.openPreviousAttemptResult(
                context = this,
                scope = lifecycleScope,
                examId = testKey,
                examName = title,
                exam = currentExam,
                onLoading = { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                }
            )
        } else {
            val intent = Intent(this, TestActivity::class.java).apply {
                putExtra(Constants.EXTRA_EXAM_ID, testKey)
                putExtra(Constants.EXTRA_EXAM_NAME, title)
                putExtra(Constants.EXTRA_EXAM_CATEGORY, currentExam?.categoryOrOther ?: "Other")
                putExtra(Constants.EXTRA_TIME_LIMIT, currentExam?.timeLimitMinutes ?: 30)
            }
            startActivity(intent)
        }
    }

    companion object {
        fun extractEndingInteger(testNumber: String): Int? {
            val match = Regex("""(\d+)\s*$""").find(testNumber.trim())
            return match?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}
