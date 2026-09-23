package com.eve.app.ui.daily

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.DailyQuizDay
import com.eve.app.databinding.ActivityDailyQuizBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.DateUtil
import com.eve.app.util.StreakStore
import com.eve.app.util.UiState
import kotlinx.coroutines.launch

class DailyQuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyQuizBinding
    private val viewModel: DailyQuizViewModel by viewModels()
    private var today: DailyQuizDay? = null

    private val adapter = DailyQuizAdapter { startQuiz(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvDays.layoutManager = LinearLayoutManager(this)
        binding.rvDays.adapter = adapter
        binding.btnStartToday.setOnClickListener { today?.let { startQuiz(it) } }

        renderStreak()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.days.collect(::render)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        renderStreak()
        viewModel.load()
    }

    private fun renderStreak() {
        val streak = StreakStore.streak(this)
        binding.tvStreak.text = when {
            streak <= 0 -> "Start your streak — attempt today's quiz."
            else -> "🔥 $streak day streak  •  Keep practicing daily to maintain your streak!"
        }
    }

    private fun render(state: UiState<List<DailyQuizDay>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.tvMessage.visibility = View.GONE
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = state.message
                adapter.submit(emptyList())
                bindToday(null)
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                val todayDay = viewModel.today(state.data)
                today = todayDay
                bindToday(todayDay)
                val past = state.data.filter { it.date != DateUtil.todayIso() }
                adapter.submit(past)
                binding.tvMessage.visibility = if (past.isEmpty() && todayDay == null) View.VISIBLE else View.GONE
                if (past.isEmpty() && todayDay == null) {
                    binding.tvMessage.text =
                        "No Daily GK quiz uploaded yet. Add questions for today from Admin Dashboard → Daily GK."
                }
            }
        }
    }

    private fun bindToday(day: DailyQuizDay?) {
        binding.tvTodayTitle.text = DateUtil.display(DateUtil.todayIso())
        if (day == null) {
            binding.tvTodaySub.text = "Today's questions have not been uploaded yet. Try a quiz from the archive."
            binding.btnStartToday.isEnabled = false
            binding.btnStartToday.text = "Quiz unavailable"
            return
        }
        binding.btnStartToday.isEnabled = true
        val qLabel = if (day.questionCount == 1) "1 question" else "${day.questionCount} questions"
        if (StreakStore.attemptedToday(this) && StreakStore.lastDate(this) == day.date) {
            val score = StreakStore.lastScore(this)
            val total = StreakStore.lastTotal(this)
            binding.tvTodaySub.text = "$qLabel  •  ${day.timeLimitMinutes} min\nAttempted today  •  Score $score/$total"
            binding.btnStartToday.text = "Retake Quiz"
        } else {
            binding.tvTodaySub.text = "$qLabel  •  ${day.timeLimitMinutes} min  •  1 min / question"
            binding.btnStartToday.text = "Start today's quiz"
        }
    }

    private fun startQuiz(day: DailyQuizDay) {
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, Constants.DAILY_GK_EXAM_ID)
                .putExtra(Constants.EXTRA_EXAM_NAME, "Daily GK")
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, Constants.DAILY_GK_CATEGORY)
                .putExtra(Constants.EXTRA_TIME_LIMIT, day.timeLimitMinutes)
                .putExtra(Constants.EXTRA_DAILY_MODE, true)
                .putExtra(Constants.EXTRA_QUIZ_DATE, day.date)
        )
    }
}
