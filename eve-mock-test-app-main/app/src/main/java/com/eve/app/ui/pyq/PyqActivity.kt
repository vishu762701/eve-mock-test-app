package com.eve.app.ui.pyq

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
import com.eve.app.data.model.PyqSet
import com.eve.app.databinding.ActivityPyqBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class PyqActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPyqBinding
    private val viewModel: PyqViewModel by viewModels()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null
    private var sets: List<PyqSet> = emptyList()
    private var selectedYear: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPyqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.spExam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedExam = exams.getOrNull(position)
                selectedYear = null
                selectedExam?.let { viewModel.loadSets(it) }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.exams.collect(::renderExams) }
                launch { viewModel.sets.collect(::renderSets) }
            }
        }
    }

    private fun renderExams(state: UiState<List<Exam>>) {
        when (state) {
            is UiState.Loading -> binding.progress.visibility = View.VISIBLE
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.text = state.message
                binding.tvMessage.visibility = View.VISIBLE
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                exams = state.data
                binding.spExam.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    exams.map { it.examName }
                )
                if (exams.isEmpty()) {
                    binding.tvMessage.text = "PYQ ke liye abhi koi exam available nahi hai."
                    binding.tvMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun renderSets(state: UiState<List<PyqSet>>) {
        binding.chipGroupYears.removeAllViews()
        binding.chipGroupPapers.removeAllViews()
        when (state) {
            is UiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.tvMessage.visibility = View.GONE
                binding.tvPapersLabel.visibility = View.GONE
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.tvMessage.text = state.message
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvPapersLabel.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                sets = state.data
                if (sets.isEmpty()) {
                    binding.tvMessage.text =
                        "Is exam me tagged PYQ nahi hain. Admin form me “Previous Year Question” tick karke year daalo."
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvPapersLabel.visibility = View.GONE
                    return
                }
                binding.tvMessage.visibility = View.GONE
                val years = sets.groupBy { it.year }.toSortedMap(compareByDescending { it })
                years.forEach { (year, yearSets) ->
                    val count = yearSets.sumOf { it.questionCount }
                    binding.chipGroupYears.addView(Chip(this).apply {
                        text = "$year • $count Qs"
                        isCheckable = true
                        isChecked = selectedYear == year
                        setOnClickListener {
                            selectedYear = year
                            renderPapers(year)
                        }
                    })
                }
                val yearToShow = selectedYear ?: years.keys.first()
                selectedYear = yearToShow
                renderPapers(yearToShow)
            }
        }
    }

    private fun renderPapers(year: Int) {
        binding.chipGroupPapers.removeAllViews()
        val yearSets = sets.filter { it.year == year }
        if (yearSets.isEmpty()) {
            binding.tvPapersLabel.visibility = View.GONE
            return
        }
        binding.tvPapersLabel.visibility = View.VISIBLE
        yearSets.forEach { set ->
            binding.chipGroupPapers.addView(Chip(this).apply {
                text = if (set.paper.isBlank()) "Paper • ${set.subtitle}" else "${set.paper} • ${set.subtitle}"
                isClickable = true
                setOnClickListener { startPyq(set) }
            })
        }
        if (yearSets.size > 1) {
            val combinedCount = yearSets.sumOf { it.questionCount }
            val exam = selectedExam
            if (exam != null) {
                binding.chipGroupPapers.addView(Chip(this).apply {
                    text = "Saare papers • $combinedCount Qs"
                    isClickable = true
                    setOnClickListener {
                        startPyq(
                            PyqSet(
                                examId = exam.id,
                                examName = exam.examName,
                                category = exam.categoryOrOther,
                                year = year,
                                paper = "",
                                questionCount = combinedCount,
                                timeLimitMinutes = exam.timeLimitMinutes.coerceAtLeast(1)
                            )
                        )
                    }
                })
            }
        }
    }

    private fun startPyq(set: PyqSet) {
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, set.examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, set.examName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, set.category)
                .putExtra(Constants.EXTRA_TIME_LIMIT, set.timeLimitMinutes)
                .putExtra(Constants.EXTRA_PYQ_MODE, true)
                .putExtra(Constants.EXTRA_PYQ_YEAR, set.year)
                .putExtra(Constants.EXTRA_PYQ_PAPER, set.paper)
        )
    }
}
