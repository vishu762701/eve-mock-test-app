package com.eve.app.ui.test

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.eve.app.data.model.Question
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.ui.result.ResultActivity
import com.eve.app.util.AnalyticsHelper
import com.eve.app.util.Constants
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.UiState
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()

    private lateinit var examId: String
    private var timeLimit = 30
    private var examName = ""
    private var examCategory = ""
    private var topic = ""
    private var pyqYear = 0
    private var pyqPaper = ""
    private var totalQuestions = 0
    private var submitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID) ?: ""
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME) ?: "Test"
        examCategory = intent.getStringExtra(Constants.EXTRA_EXAM_CATEGORY) ?: ""
        timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 30)
        topic = intent.getStringExtra(Constants.EXTRA_TOPIC).orEmpty()
        pyqYear = intent.getIntExtra(Constants.EXTRA_PYQ_YEAR, 0)
        pyqPaper = intent.getStringExtra(Constants.EXTRA_PYQ_PAPER).orEmpty()

        viewModel.start(examId, timeLimit, topic, pyqYear, pyqPaper)
        // Phase 15: exam start event — is exam ko kitni baar attempt kiya gaya, yeh track karta hai
        AnalyticsHelper.logExamStart(this, examId, examName, examCategory)

        binding.btnPrev.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
        binding.btnNext.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
        binding.btnSubmit.setOnClickListener { confirmSubmit() }
        binding.btnRetry.setOnClickListener { viewModel.retry(examId, timeLimit, topic, pyqYear, pyqPaper) }

        LanguageManager.setupToggleButton(this, binding.btnLanguage) {
            binding.viewPager.adapter?.notifyDataSetChanged()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateNav(position)
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@TestActivity)
                    .setTitle("Test chhodna hai?")
                    .setMessage("Exit karne par aapka progress lost ho jayega.")
                    .setPositiveButton("Exit") { _, _ ->
                        viewModel.stopTimer()
                        finish()
                    }
                    .setNegativeButton("Continue", null)
                    .show()
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.questions.collect { renderQuestions(it) } }
                launch { viewModel.remainingSeconds.collect { renderTimer(it) } }
                launch { viewModel.timeUp.collect { if (it) submit() } }
                launch {
                    NetworkUtil.observe(this@TestActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun renderQuestions(state: UiState<List<Question>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressGroup.visibility = View.VISIBLE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_error)
                if (NetworkUtil.isOnline(this)) {
                    binding.tvMessage.text = "Kuch gadbad ho gayi"
                    binding.tvMessageSub.text = state.message
                } else {
                    binding.tvMessage.text = "No internet connection"
                    binding.tvMessageSub.text =
                        "Is exam ke questions abhi tak cache nahi hue. Network wapas aane par retry karo."
                }
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                val list = state.data
                totalQuestions = list.size
                if (list.isEmpty()) {
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.GONE
                    binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_empty)
                    binding.tvMessage.text = if (pyqYear > 0) "Is year/paper ke PYQ nahi mile"
                    else "Is exam me abhi koi question nahi hai"
                    binding.tvMessageSub.text = if (pyqYear > 0) "Admin Dashboard se isPyq + year tag karke questions upload karo"
                    else "Admin se question upload karne ko bolo"
                    binding.tvTimer.text = "--:--"
                    return
                }
                binding.messageGroup.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                if (binding.viewPager.adapter == null) {
                    binding.viewPager.adapter = QuestionAdapter(
                        list,
                        getSelected = { viewModel.getAnswer(it) },
                        onSelect = { pos, letter -> viewModel.setAnswer(pos, letter) },
                        getBookmarked = { viewModel.isBookmarked(it) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        isHindi = { LanguageManager.isHindi(this) }
                    )
                }
                updateNav(binding.viewPager.currentItem)
            }
        }
    }

    private fun renderTimer(seconds: Long) {
        if (seconds < 0) return
        val m = seconds / 60
        val s = seconds % 60
        binding.tvTimer.text = String.format("%02d:%02d", m, s)
        binding.tvTimer.setTextColor(if (seconds <= 60) Color.parseColor("#FFAB91") else Color.WHITE)
    }

    private fun updateNav(position: Int) {
        if (totalQuestions == 0) return
        val title = sessionTitle()
        binding.tvProgress.text = "$title  •  Question ${position + 1} / $totalQuestions"
        binding.btnPrev.isEnabled = position > 0
        binding.btnNext.isEnabled = position < totalQuestions - 1
    }

    private fun confirmSubmit() {
        val items = viewModel.buildAnswerItems()
        val unattempted = items.count { !it.isAttempted }
        AlertDialog.Builder(this)
            .setTitle("Test submit karein?")
            .setMessage("Unattempted questions: $unattempted")
            .setPositiveButton("Submit") { _, _ -> submit() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submit() {
        if (submitted) return
        submitted = true
        viewModel.stopTimer()
        val items = viewModel.buildAnswerItems()
        val attemptName = sessionTitle()
        viewModel.saveAttempt(examId, attemptName, examCategory, items)

        // Phase 15: exam submit event + score summary (average score / weak exams Console me dikhenge)
        AnalyticsHelper.logExamSubmit(
            context = this,
            examId = examId,
            examName = attemptName,
            category = examCategory,
            correct = items.count { it.isCorrect },
            wrong = items.count { it.isAttempted && !it.isCorrect },
            unattempted = items.count { !it.isAttempted },
            total = items.size
        )

        startActivity(
            Intent(this, ResultActivity::class.java)
                .putParcelableArrayListExtra(Constants.EXTRA_ANSWERS, items)
                .putExtra(Constants.EXTRA_EXAM_ID, examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, attemptName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, examCategory)
        )
        finish()
    }

    private fun sessionTitle(): String = when {
        pyqYear > 0 -> {
            val paper = if (pyqPaper.isBlank()) "" else " • $pyqPaper"
            "$examName • PYQ $pyqYear$paper"
        }
        topic.isNotBlank() -> "$examName • $topic Practice"
        else -> examName
    }
}
