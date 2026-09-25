package com.eve.app.ui.bookmarks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.databinding.ActivityBookmarksBinding
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.EmptyStateAnimationHelper
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.UiState
import kotlinx.coroutines.launch

class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var adapter: BookmarkAdapter
    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = BookmarkAdapter(
            onOpenQuestion = { bookmark -> openQuestionInTest(bookmark) },
            onUnbookmark = { bookmark ->
                viewModel.unbookmark(bookmark.questionId)
                Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
            },
            isHindi = { LanguageManager.isHindi(this) }
        )

        binding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        binding.rvBookmarks.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.load() }

        LanguageManager.setupToggleButton(this, binding.btnLanguage) {
            adapter.notifyDataSetChanged()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { renderState(it) } }
                launch {
                    NetworkUtil.observe(this@BookmarksActivity).collect { online ->
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

    private fun renderState(state: UiState<List<BookmarkedQuestion>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressGroup.visibility = View.VISIBLE
                binding.messageGroup.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                adapter.submitList(state.data)
                val empty = state.data.isEmpty()
                binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
                binding.rvBookmarks.visibility = if (empty) View.GONE else View.VISIBLE
                if (empty) {
                    binding.tvBookmarkCount.visibility = View.GONE
                    hasEmptyPlayed = EmptyStateAnimationHelper.showEmptyState(
                        binding.ivMessageIcon,
                        hasEmptyPlayed
                    )
                    binding.tvMessage.text = "No bookmarks yet"
                    binding.tvMessageSub.text = "Star questions during a test to review or practice them here anytime."
                } else {
                    hasEmptyPlayed = false
                    binding.tvBookmarkCount.visibility = View.VISIBLE
                    binding.tvBookmarkCount.text = "${state.data.size} saved"
                }
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setAnimation(R.raw.error_404)
                binding.ivMessageIcon.playAnimation()
                if (NetworkUtil.isOnline(this)) {
                    binding.tvMessage.text = "Something went wrong"
                    binding.tvMessageSub.text = state.message
                } else {
                    binding.tvMessage.text = "No internet connection"
                    binding.tvMessageSub.text = "Bookmarks could not be loaded offline."
                }
            }
        }
    }

    private fun openQuestionInTest(bookmark: BookmarkedQuestion) {
        if (bookmark.examId.isBlank() && bookmark.questionId.isBlank()) {
            Toast.makeText(this, "Question details unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, bookmark.examId)
            putExtra(Constants.EXTRA_EXAM_NAME, bookmark.examName.ifBlank { "Bookmarked Question" })
            putExtra(Constants.EXTRA_TOPIC, bookmark.topic)
            putExtra(Constants.EXTRA_PYQ_YEAR, bookmark.pyqYear)
            putExtra(Constants.EXTRA_PYQ_PAPER, bookmark.pyqPaper)
            putExtra(Constants.EXTRA_INITIAL_QUESTION_ID, bookmark.questionId)
            putExtra(Constants.EXTRA_FROM_BOOKMARK, true)
        }
        startActivity(intent)
    }
}
