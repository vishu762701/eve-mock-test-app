package com.eve.app.ui.practice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eve.app.data.model.Exam
import com.eve.app.data.model.PracticeTopic
import com.eve.app.databinding.ActivityPracticeBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class PracticeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPracticeBinding
    private val viewModel: PracticeViewModel by viewModels()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.spExam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedExam = exams.getOrNull(position)
                selectedExam?.let { viewModel.loadTopics(it.id) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.exams.collect(::renderExams) }
                launch { viewModel.topics.collect(::renderTopics) }
            }
        }
    }

    private fun renderExams(state: UiState<List<Exam>>) {
        when (state) {
            is UiState.Loading -> binding.progress.visibility = View.VISIBLE
            is UiState.Error -> { binding.progress.visibility = View.GONE; binding.tvMessage.text = state.message; binding.tvMessage.visibility = View.VISIBLE }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                exams = state.data
                binding.spExam.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, exams.map { it.examName })
                binding.tvMessage.visibility = if (exams.isEmpty()) View.VISIBLE else View.GONE
                if (exams.isEmpty()) binding.tvMessage.text = "Practice ke liye abhi koi exam available nahi hai."
            }
        }
    }

    private fun renderTopics(state: UiState<List<PracticeTopic>>) {
        binding.chipGroupTopics.removeAllViews()
        when (state) {
            is UiState.Loading -> { binding.progress.visibility = View.VISIBLE; binding.tvMessage.visibility = View.GONE }
            is UiState.Error -> { binding.progress.visibility = View.GONE; binding.tvMessage.text = state.message; binding.tvMessage.visibility = View.VISIBLE }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                if (state.data.isEmpty()) binding.tvMessage.text = "Is exam me topic-tagged questions nahi hain. Admin se Topic field bharwane ko bolo."
                state.data.forEach { topic -> addTopicChip(topic) }
            }
        }
    }

    private fun addTopicChip(topic: PracticeTopic) {
        binding.chipGroupTopics.addView(Chip(this).apply {
            text = "${topic.name} • ${topic.questionCount} questions"
            isClickable = true
            setOnClickListener { startPractice(topic) }
        })
    }

    private fun startPractice(topic: PracticeTopic) {
        val exam = selectedExam ?: return
        // Small set: maximum 10 random questions, 1 minute/question up to 10 minutes.
        val count = topic.questionCount.coerceAtMost(10)
        startActivity(Intent(this, TestActivity::class.java)
            .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
            .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
            .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
            .putExtra(Constants.EXTRA_TIME_LIMIT, count.coerceAtLeast(1))
            .putExtra(Constants.EXTRA_TOPIC, topic.name)
            .putExtra(Constants.EXTRA_PRACTICE_MODE, true))
    }
}
