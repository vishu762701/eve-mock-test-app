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

        viewModel.start(examId, timeLimit)

        binding.btnPrev.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
        binding.btnNext.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
        binding.btnSubmit.setOnClickListener { confirmSubmit() }
        binding.btnRetry.setOnClickListener { viewModel.retry(examId, timeLimit) }

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
                    binding.tvMessage.text = "Is exam me abhi koi question nahi hai"
                    binding.tvMessageSub.text = "Admin se question upload karne ko bolo"
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
        binding.tvProgress.text = "$examName  •  Question ${position + 1} / $totalQuestions"
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
        viewModel.saveAttempt(examId, examName, examCategory, items)
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putParcelableArrayListExtra(Constants.EXTRA_ANSWERS, items)
        )
        finish()
    }
}
