package com.eve.app.ui.leaderboard

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
 * Phase 16: Leaderboard/Rank. Kisi bhi exam ke top scorers dikhata hai, aur agar current
 * user ne wo exam attempt kiya hai to upar ek highlighted card me uska apna rank +
 * "top X%" bhi dikhta hai. Data `leaderboard` collection se aata hai jo Cloud Function
 * automatically maintain karta hai (dekho LeaderboardEntry.kt ka comment).
 *
 * ResultActivity ("View Leaderboard" button) aur HistoryActivity (purana attempt review
 * karte waqt) — dono jagah se yahan aa sakte ho.
 */
class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private val viewModel: LeaderboardViewModel by viewModels()
    private lateinit var examId: String

    private val adapter = LeaderboardAdapter(FirebaseAuth.getInstance().currentUser?.uid)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    binding.ivMessageIcon.setImageResource(R.drawable.ic_state_empty)
                    binding.tvMessage.text = if (examId == "overall") "No overall scores yet" else "Abhi koi scorer nahi hai"
                    binding.tvMessageSub.text = if (examId == "overall") "Complete any test to see overall rankings here" else "Is exam ka pehla attempt submit karte hi yahan dikhega"
                }
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setImageResource(R.drawable.ic_state_error)
                binding.tvMessage.text = "Kuch gadbad ho gayi"
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
        binding.tvYourRank.text = "Your Rank: #${rankInfo.rank}"
        binding.tvYourPercentile.text =
            "Top ${rankInfo.topPercent}% of ${rankInfo.totalParticipants} students"
        binding.tvYourScore.text = "${rankInfo.scoreText}/${rankInfo.total}"
    }
}
