package com.eve.app.ui.practice

import com.eve.app.ui.common.EveBaseActivity

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
import com.eve.app.ui.common.TelegramHorizontalScrollLayoutManager
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PracticeActivity : EveBaseActivity() {
    private lateinit var binding: ActivityPracticeBinding
    private val viewModel: PracticeViewModel by viewModels()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null

    private val practiceAuth = FirebaseAuth.getInstance()
    private var practiceOwnerUid: String? = null
    private fun hasPracticeOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(practiceOwnerUid, practiceAuth.currentUser?.uid)
    private val practiceAuthListener = FirebaseAuth.AuthStateListener {
        if (practiceOwnerUid != null && !hasPracticeOwner()) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = practiceAuth.currentUser?.uid
        practiceOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("practice_owner_uid")
        if (!hasPracticeOwner()) { finish(); return }

        binding = ActivityPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TelegramHorizontalScrollLayoutManager.install(binding.scrollPractice)
        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) { finish() }
        binding.spExam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!hasPracticeOwner()) { finish(); return }
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

    override fun onStart() {
        super.onStart()
        if (!hasPracticeOwner()) { finish(); return }
        practiceAuth.addAuthStateListener(practiceAuthListener)
    }

    override fun onStop() {
        practiceAuth.removeAuthStateListener(practiceAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPracticeOwner()) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("practice_owner_uid", practiceOwnerUid)
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.chipGroupTopics.removeAllViews()
        }
        super.onDestroy()
    }

    private var currentTopics: List<PracticeTopic> = emptyList()

    private fun renderExams(state: UiState<List<Exam>>) {
        if (!hasPracticeOwner()) return
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
                    com.eve.app.util.AppBulletin.showError(this@PracticeActivity, "Couldn't refresh exams: ${state.message}")
                } else {
                    binding.tvMessage.text = state.message
                    binding.tvMessage.visibility = View.VISIBLE
                }
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                exams = state.data
                val prevSelectedId = selectedExam?.id
                selectedExam = exams.firstOrNull { it.id == prevSelectedId } ?: exams.firstOrNull()
                binding.spExam.adapter = ArrayAdapter(this, com.eve.app.R.layout.item_exam_dropdown, exams.map { it.examName })
                if (exams.isEmpty()) {
                    viewModel.loadTopics("")
                    binding.chipGroupTopics.removeAllViews()
                }
                if (prevSelectedId != null) {
                    val newIndex = exams.indexOfFirst { it.id == prevSelectedId }
                    if (newIndex >= 0) {
                        binding.spExam.setSelection(newIndex)
                    }
                }
                binding.tvMessage.visibility = if (exams.isEmpty()) View.VISIBLE else View.GONE
                if (exams.isEmpty()) binding.tvMessage.text = "No exams are currently available for practice."
            }
        }
    }

    private fun renderTopics(state: UiState<List<PracticeTopic>>) {
        if (!hasPracticeOwner()) return
        when (state) {
            is UiState.Loading -> {
                if (currentTopics.isEmpty()) {
                    binding.chipGroupTopics.removeAllViews()
                    binding.progress.visibility = View.VISIBLE
                    binding.tvMessage.visibility = View.GONE
                }
            }
            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                if (currentTopics.isNotEmpty()) {
                    com.eve.app.util.AppBulletin.showError(this@PracticeActivity, "Couldn't refresh topics: ${state.message}")
                } else {
                    binding.chipGroupTopics.removeAllViews()
                    binding.tvMessage.text = state.message
                    binding.tvMessage.visibility = View.VISIBLE
                }
            }
            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                currentTopics = state.data
                binding.chipGroupTopics.removeAllViews()
                binding.tvMessage.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                if (state.data.isEmpty()) {
                    binding.tvMessage.text = if (exams.isEmpty()) "No exams are currently available for practice."
                    else "No topic-tagged questions found for this exam. Please ask an admin to add topics."
                }
                state.data.forEach { topic -> addTopicChip(topic) }
            }
        }
    }

    private fun addTopicChip(topic: PracticeTopic) {
        binding.chipGroupTopics.addView(com.eve.app.ui.common.StudentSelectionControls.action(this).apply {
            text = "${topic.name} • ${topic.questionCount} questions"
            isClickable = true
            com.eve.app.designsystem.DesignPress.bind(this)
            setOnClickListener {
                if (!hasPracticeOwner()) { finish(); return@setOnClickListener }
                startPractice(topic)
            }
        })
    }

    private fun startPractice(topic: PracticeTopic) {
        if (!hasPracticeOwner()) { finish(); return }
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
