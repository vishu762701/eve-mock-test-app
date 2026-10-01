package com.eve.app.ui.leaderboard

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.eve.app.databinding.ActivityLeaderboardBinding
import com.eve.app.util.Constants
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Leaderboard / Rank screen. Displays top scorers for an exam, and if the current
 * user attempted the exam, displays a prominent card at the top with their rank
 * and percentile.
 */
class LeaderboardActivity : EveBaseActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val viewModel: LeaderboardViewModel by viewModels()
    private lateinit var examId: String

    private val adapter = LeaderboardAdapter(FirebaseAuth.getInstance().currentUser?.uid)

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID) ?: "overall"
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        if (examId == "overall" || examId.isBlank()) {
            examId = "overall"
            binding.tvExamName.text = if (!examName.isNullOrBlank()) examName else "Overall Leaderboard"
        } else if (!examName.isNullOrBlank()) {
            binding.tvExamName.text = examName
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        binding.rvLeaderboard.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.load(examId) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.rankInfo.collect { renderRank(it) } }
            }
        }

        viewModel.load(examId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun render(state: UiState<List<LeaderboardEntry>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressGroup.visibility = View.VISIBLE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                adapter.submit(state.data)
                val empty = state.data.isEmpty()
                binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
                        binding.ivMessageIcon,
                        hasEmptyPlayed
                    )
                    binding.tvMessage.text = if (examId == "overall") "No overall scores yet" else "No scores recorded yet"
                    binding.tvMessageSub.text = if (examId == "overall") "Complete any test to see overall rankings here" else "Rankings will appear after the first attempt is submitted"
                } else {
                    hasEmptyPlayed = false
                }
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                com.eve.app.util.EmptyStateAnimationHelper.showErrorState(binding.ivMessageIcon)
                binding.tvMessage.text = "Something went wrong"
                binding.tvMessageSub.text = state.message
            }
        }
    }

    private fun renderRank(rankInfo: RankInfo?) {
        if (rankInfo == null) {
            binding.cardYourRank.visibility = View.GONE
            return
        }
        binding.cardYourRank.visibility = View.VISIBLE
        binding.tvRankMedallion.text = "#${rankInfo.rank}"
        binding.tvYourRank.text = "#${rankInfo.rank}"
        binding.tvYourPercentile.text = if (rankInfo.totalParticipants <= 1) {
            "You're the first to attempt this test"
        } else {
            val studentWord = if (rankInfo.totalParticipants == 1) "student" else "students"
            "Top ${rankInfo.topPercent}% of ${rankInfo.totalParticipants} $studentWord"
        }

        val scoreSpannable = android.text.SpannableStringBuilder().apply {
            append(rankInfo.scoreText)
            val secondaryColor = androidx.core.content.ContextCompat.getColor(this@LeaderboardActivity, R.color.eve_text_secondary)
            val start = length
            append("/${rankInfo.total}")
            setSpan(
                android.text.style.ForegroundColorSpan(secondaryColor),
                start,
                length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                android.text.style.RelativeSizeSpan(0.85f),
                start,
                length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvYourScore.text = scoreSpannable
    }
}
