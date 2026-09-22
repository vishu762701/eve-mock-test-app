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
import com.eve.app.util.UiState
import kotlinx.coroutines.launch

class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val viewModel: TestViewModel by viewModels()

    private lateinit var examId: String
    private var timeLimit = 30
    private var examName = ""
    private var totalQuestions = 0
    private var submitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID) ?: ""
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME) ?: "Test"
        timeLimit = intent.getIntExtra(Constants.EXTRA_TIME_LIMIT, 30)

        viewModel.start(examId, timeLimit)

        binding.btnPrev.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
        binding.btnNext.setOnClickListener {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
        binding.btnSubmit.setOnClickListener { confirmSubmit() }

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
            }
        }
    }

    private fun renderQuestions(state: UiState<List<Question>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.tvMessage.visibility = View.GONE
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = state.message
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                val list = state.data
                totalQuestions = list.size
                if (list.isEmpty()) {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvMessage.text = "Is exam me abhi koi question nahi hai"
                    binding.tvTimer.text = "--:--"
                    return
                }
                binding.tvMessage.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                if (binding.viewPager.adapter == null) {
                    binding.viewPager.adapter = QuestionAdapter(
                        list,
                        getSelected = { viewModel.getAnswer(it) },
                        onSelect = { pos, letter -> viewModel.setAnswer(pos, letter) }
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
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putParcelableArrayListExtra(Constants.EXTRA_ANSWERS, items)
        )
        finish()
    }
}
