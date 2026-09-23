package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityAdminAnalyticsBinding
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminAnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminAnalyticsBinding
    private val viewModel: AdminAnalyticsViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val examAdapter = AnalyticsExamAdapter()
    private val questionAdapter = AnalyticsQuestionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setup()
        }
    }

    private fun setup() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { viewModel.load() }
        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = examAdapter
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.load()
                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            UiState.Loading -> {
                                binding.progress.visibility = View.VISIBLE
                                binding.content.visibility = View.GONE
                            }
                            is UiState.Error -> {
                                binding.progress.visibility = View.GONE
                                binding.content.visibility = View.VISIBLE
                                Toast.makeText(this@AdminAnalyticsActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                            is UiState.Success -> {
                                binding.progress.visibility = View.GONE
                                binding.content.visibility = View.VISIBLE
                                val data = state.data
                                examAdapter.submit(data.exams)
                                // Weak-question detection requires at least 3 attempted answers to avoid
                                // ranking a question from one unlucky student.
                                questionAdapter.submit(data.questions.filter { it.attempts >= 3 }.take(30))
                                val total = data.exams.sumOf { it.attemptCount }
                                val students = data.exams.sumOf { it.uniqueUsers }
                                binding.tvSummary.text = "Total test submissions: $total • Unique students (sum by exam): $students"
                                val top = data.exams.firstOrNull()
                                binding.tvMostAttempted.text = top?.let {
                                    "Most attempted: ${it.examName} — ${it.attemptCount} attempts"
                                } ?: "Most attempted: —"
                                binding.tvEmpty.visibility =
                                    if (data.exams.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                    }
                }
            }
        }
    }
}
