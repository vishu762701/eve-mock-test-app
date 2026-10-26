package com.eve.app.ui.history

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.TestAttempt
import com.eve.app.databinding.ActivityHistoryBinding
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.Constants
import com.eve.app.util.NetworkUtil
import com.eve.app.util.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class HistoryActivity : EveBaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private val adapter = HistoryAdapter { attempt -> openReview(attempt) }

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        com.eve.app.ui.navigation.EveNavigationHost.attach(
            this,
            com.eve.app.ui.navigation.EveBottomPillBar.TAB_HISTORY
        ) {
            binding.rvHistory.smoothScrollToPosition(0)
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
        // UIArc Scroll Area adaptation: keep RecyclerView's native virtualization,
        // scroll gesture, and unobtrusive fading overlay indicator (900ms idle).
        binding.rvHistory.isVerticalScrollBarEnabled = true
        binding.rvHistory.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        binding.rvHistory.setScrollbarFadingEnabled(true)
        binding.rvHistory.setScrollBarDefaultDelayBeforeFade(900)

        binding.btnRetry.setOnClickListener { viewModel.load() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    NetworkUtil.observe(this@HistoryActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun render(state: UiState<List<TestAttempt>>) {
        when (state) {
            is UiState.Loading -> {
                val hasCachedItems = adapter.itemCount > 0
                if (hasCachedItems) {
                    binding.loadingHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                } else {
                    binding.loadingHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                }
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                adapter.submit(state.data)
                val empty = state.data.isEmpty()
                binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    binding.loadingHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.GONE
                    hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
                        binding.ivMessageIcon,
                        hasEmptyPlayed
                    )
                    binding.tvMessage.text = "No test attempts yet"
                    binding.tvMessageSub.text = "Submitted tests will appear here"
                } else {
                    hasEmptyPlayed = false
                    com.eve.app.designsystem.DesignLoading.showContent(binding.loadingHistory, binding.rvHistory)
                }
            }
            is UiState.Error -> {
                binding.loadingHistory.visibility = View.GONE
                binding.progressGroup.visibility = View.GONE
                if (adapter.itemCount > 0) {
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                } else {
                    binding.rvHistory.visibility = View.GONE
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                    com.eve.app.util.EmptyStateAnimationHelper.showErrorState(binding.ivMessageIcon)
                    if (NetworkUtil.isOnline(this)) {
                        binding.tvMessage.text = "Something went wrong"
                        binding.tvMessageSub.text = state.message
                    } else {
                        binding.tvMessage.text = "No internet connection"
                        binding.tvMessageSub.text =
                            "History has not been cached yet. Please retry when internet connection is restored."
                    }
                }
            }
        }
    }

    private fun openReview(attempt: TestAttempt) {
        ResultDataHolder.setAnswers(attempt.answers)
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, attempt.examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, attempt.examName)
                .putExtra(Constants.EXTRA_ATTEMPT_DATE, dateFormat.format(Date(attempt.timestamp)))
                .putExtra(Constants.EXTRA_FROM_HISTORY, true)
        )
    }
}
