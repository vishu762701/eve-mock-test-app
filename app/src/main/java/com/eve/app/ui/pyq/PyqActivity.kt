package com.eve.app.ui.pyq

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
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
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.UiState
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class PyqActivity : EveBaseActivity() {

    private lateinit var binding: ActivityPyqBinding
    private val viewModel: PyqViewModel by viewModels()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null
    private var sets: List<PyqSet> = emptyList()
    private var selectedYear: Int? = null
    private var restoredExamId: String? = null
    private val pyqAuth = FirebaseAuth.getInstance()
    private var pyqOwnerUid: String? = null
    private fun hasPyqOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(pyqOwnerUid, pyqAuth.currentUser?.uid)
    private val pyqAuthListener = FirebaseAuth.AuthStateListener {
        if (pyqOwnerUid != null && !hasPyqOwner()) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = pyqAuth.currentUser?.uid
        pyqOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("pyq_owner_uid")
        if (!hasPyqOwner()) { finish(); return }
        binding = ActivityPyqBinding.inflate(layoutInflater)
        setContentView(binding.root)
        restoredExamId = savedInstanceState?.getString(STATE_EXAM_ID)
        selectedYear = savedInstanceState?.takeIf { it.containsKey(STATE_YEAR) }?.getInt(STATE_YEAR)

        binding.btnBack.setOnClickListener { finish() }
        com.eve.app.ui.common.TelegramHorizontalScrollLayoutManager.install(binding.chipGroupYears.parent as View)
        binding.spExam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!hasPyqOwner()) { finish(); return }
                val next = exams.getOrNull(position) ?: return
                if (viewModel.selectedExamId != next.id) {
                    selectedYear = null
                    sets = emptyList()
                }
                selectedExam = next
                viewModel.loadSets(next)
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

    override fun onStart() {
        super.onStart()
        if (!hasPyqOwner()) { finish(); return }
        pyqAuth.addAuthStateListener(pyqAuthListener)
    }

    override fun onStop() {
        pyqAuth.removeAuthStateListener(pyqAuthListener)
        super.onStop()
    }

    private fun isCurrentOwner(): Boolean = hasPyqOwner()

    override fun onResume() {
        super.onResume()
        if (!hasPyqOwner()) finish()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.chipGroupYears.removeAllViews()
            binding.chipGroupPapers.removeAllViews()
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pyq_owner_uid", pyqOwnerUid)
        (viewModel.selectedExamId ?: selectedExam?.id)?.let {
            outState.putString(STATE_EXAM_ID, it)
        }
        selectedYear?.let { outState.putInt(STATE_YEAR, it) }
    }

    private fun renderExams(state: UiState<List<Exam>>) {
        if (!isCurrentOwner()) return
        when (state) {
            is UiState.Loading -> {
                if (exams.isEmpty()) {
                    binding.progress.visibility = View.VISIBLE
                }
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                if (exams.isNotEmpty()) {
                    binding.tvMessage.visibility = View.GONE
                    com.eve.app.util.AppBulletin.showError(this@PyqActivity, "Couldn't refresh exams: ${state.message}")
                } else {
                    binding.tvMessage.text = state.message
                    binding.tvMessage.visibility = View.VISIBLE
                }
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                val wantedId = viewModel.selectedExamId ?: restoredExamId ?: selectedExam?.id
                exams = state.data
                binding.spExam.adapter = ArrayAdapter(
                    this,
                    com.eve.app.R.layout.item_exam_dropdown,
                    exams.map { it.examName }
                )
                if (exams.isEmpty()) {
                    selectedExam = null
                    sets = emptyList()
                    viewModel.clearSelection()
                    binding.chipGroupYears.removeAllViews()
                    binding.chipGroupPapers.removeAllViews()
                    binding.tvPapersLabel.visibility = View.GONE
                    binding.tvMessage.text = "No exams are currently available for previous year questions."
                    binding.tvMessage.visibility = View.VISIBLE
                    restoredExamId = null
                    return
                }
                val index = exams.indexOfFirst { it.id == wantedId }.coerceAtLeast(0)
                val exam = exams[index]
                // Restore the year only when restoring the very same exam.
                if (wantedId != exam.id) selectedYear = null
                selectedExam = exam
                binding.spExam.setSelection(index)
                viewModel.loadSets(exam)
                restoredExamId = null
            }
        }
    }

    private fun renderSets(state: UiState<List<PyqSet>>) {
        if (!isCurrentOwner() || selectedExam?.id != viewModel.selectedExamId) return
        when (state) {
            is UiState.Loading -> {
                if (sets.isEmpty()) {
                    binding.chipGroupYears.removeAllViews()
                    binding.chipGroupPapers.removeAllViews()
                    binding.progress.visibility = View.VISIBLE
                    binding.tvMessage.visibility = View.GONE
                    binding.tvPapersLabel.visibility = View.GONE
                }
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                if (sets.isNotEmpty()) {
                    com.eve.app.util.AppBulletin.showError(this@PyqActivity, "Couldn't refresh sets: ${state.message}")
                } else {
                    sets = emptyList()
                    binding.chipGroupYears.removeAllViews()
                    binding.chipGroupPapers.removeAllViews()
                    binding.tvMessage.text = state.message
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvPapersLabel.visibility = View.GONE
                }
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                binding.chipGroupYears.removeAllViews()
                binding.chipGroupPapers.removeAllViews()
                sets = state.data
                if (sets.isEmpty()) {
                    binding.tvMessage.text =
                        "No tagged PYQs found for this exam. Please upload questions tagged with Previous Year Question and year."
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.tvPapersLabel.visibility = View.GONE
                    return
                }
                binding.tvMessage.visibility = View.GONE
                val years = sets.groupBy { it.year }.toSortedMap(compareByDescending { it })
                years.forEach { (year, yearSets) ->
                    val count = yearSets.sumOf { it.questionCount }
                    binding.chipGroupYears.addView(com.eve.app.ui.common.StudentSelectionControls.year(this).apply {
                        text = "$year • $count Qs"
                        isCheckable = true
                        isChecked = selectedYear == year
                        setOnClickListener {
                            selectedYear = year
                            renderPapers(year)
                        }
                    })
                }
                val yearToShow = selectedYear?.takeIf { it in years } ?: years.keys.first()
                selectedYear = yearToShow
                for (i in 0 until binding.chipGroupYears.childCount) {
                    (binding.chipGroupYears.getChildAt(i) as? Chip)?.isChecked = years.keys.elementAt(i) == yearToShow
                }
                renderPapers(yearToShow)
            }
        }
    }

    private fun renderPapers(year: Int) {
        if (!isCurrentOwner() || selectedExam?.id != viewModel.selectedExamId) return
        binding.chipGroupPapers.removeAllViews()
        val yearSets = sets.filter { it.examId == selectedExam?.id && it.year == year }
        if (yearSets.isEmpty()) {
            binding.tvPapersLabel.visibility = View.GONE
            return
        }
        binding.tvPapersLabel.visibility = View.VISIBLE
        yearSets.forEach { set ->
            binding.chipGroupPapers.addView(com.eve.app.ui.common.StudentSelectionControls.action(this).apply {
                text = if (set.paper.isBlank()) "Paper • ${set.subtitle}" else "${set.paper} • ${set.subtitle}"
                isClickable = true
                setOnClickListener {
                    if (!hasPyqOwner()) { finish(); return@setOnClickListener }
                    startPyq(set)
                }
            })
        }
        if (yearSets.size > 1) {
            val combinedCount = yearSets.sumOf { it.questionCount }
            val exam = selectedExam
            if (exam != null) {
                binding.chipGroupPapers.addView(com.eve.app.ui.common.StudentSelectionControls.action(this).apply {
                    text = "Saare papers • $combinedCount Qs"
                    isClickable = true
                    setOnClickListener {
                        if (!hasPyqOwner()) { finish(); return@setOnClickListener }
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
        val exam = selectedExam ?: return
        if (!isCurrentOwner() || viewModel.selectedExamId != exam.id ||
            set.examId != exam.id || set.year != selectedYear) return
        val yearSets = sets.filter { it.examId == exam.id && it.year == set.year }
        val validCombined = yearSets.size > 1 && set.paper.isBlank() &&
            set.examName == exam.examName && set.category == exam.categoryOrOther &&
            set.questionCount == yearSets.sumOf { it.questionCount } &&
            set.timeLimitMinutes == exam.timeLimitMinutes.coerceAtLeast(1)
        if (set !in yearSets && !validCombined) return
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

    private companion object {
        const val STATE_EXAM_ID = "pyq:selected-exam"
        const val STATE_YEAR = "pyq:selected-year"
    }
}
