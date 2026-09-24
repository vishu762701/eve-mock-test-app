package com.eve.app.ui.performance

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.PerformanceData
import com.eve.app.databinding.ActivityPerformanceBinding
import com.eve.app.util.UiState
import kotlinx.coroutines.launch

/**
 * Phase 17: Performance Analytics — Test History (Phase 10) ke saare attempts se
 * subject/topic-wise accuracy, weak areas, aur score trend graph nikalta hai.
 * Koi naya Firestore collection nahi chahiye — sab kuch `attempts` collection se
 * (jo har answer ke saath uske question ka `topic` bhi save karta hai, agar admin ne
 * bhara ho) client-side compute hota hai (dekho PerformanceViewModel.compute()).
 *
 * Home screen ke "Performance" button se yahan aate hain.
 */
class PerformanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerformanceBinding
    private val viewModel: PerformanceViewModel by viewModels()
    private val topicAdapter = TopicStatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { viewModel.load() }

        binding.rvTopics.layoutManager = LinearLayoutManager(this)
        binding.rvTopics.adapter = topicAdapter
        binding.rvTopics.isNestedScrollingEnabled = false

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
            }
        }
    }

    private fun render(state: UiState<PerformanceData>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressGroup.visibility = View.VISIBLE
                binding.contentGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                val data = state.data
                if (data.totalAttempts == 0) {
                    binding.contentGroup.visibility = View.GONE
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.ivMessageIcon.setAnimation(R.raw.error_404)
                    binding.ivMessageIcon.playAnimation()
                    binding.tvMessage.text = "No insights available yet"
                    binding.tvMessageSub.text =
                        "Your performance insights will appear here after you submit a test"
                } else {
                    binding.messageGroup.visibility = View.GONE
                    binding.contentGroup.visibility = View.VISIBLE
                    renderData(data)
                }
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.contentGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setAnimation(R.raw.error_404)
                binding.ivMessageIcon.playAnimation()
                binding.tvMessage.text = "Something went wrong"
                binding.tvMessageSub.text = state.message
            }
        }
    }

    private fun renderData(data: PerformanceData) {
        binding.tvTotalAttempts.text = data.totalAttempts.toString()
        binding.tvOverallAccuracy.text = "${data.overallAccuracy}%"
        binding.chartTrend.submit(data.scoreTrend)

        val weak = data.weakTopics
        if (weak.isEmpty()) {
            binding.cardWeakAreas.visibility = View.GONE
        } else {
            binding.cardWeakAreas.visibility = View.VISIBLE
            binding.tvWeakAreas.text = weak.joinToString("\n") {
                "•  ${it.topic} — ${it.accuracy}% accuracy (${it.correct}/${it.total})"
            }
        }

        topicAdapter.submit(data.topicStats)
        binding.tvNoTopics.visibility = if (data.topicStats.isEmpty()) View.VISIBLE else View.GONE
    }
}
