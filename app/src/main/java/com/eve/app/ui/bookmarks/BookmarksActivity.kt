package com.eve.app.ui.bookmarks

import com.eve.app.ui.common.EveBaseActivity

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
import com.eve.app.designsystem.UiArcSwipeRow
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.EmptyStateAnimationHelper
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.UiState
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class BookmarksActivity : EveBaseActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var adapter: BookmarkAdapter
    private var hasEmptyPlayed = false
    private val pendingRemovalIds = mutableSetOf<String>()
    private var hasResumedOnce = false
    private val bookmarksAuth = FirebaseAuth.getInstance()
    private var bookmarksOwnerUid: String? = null
    private fun hasBookmarksOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(bookmarksOwnerUid, bookmarksAuth.currentUser?.uid)
    private val bookmarksAuthListener = FirebaseAuth.AuthStateListener {
        if (bookmarksOwnerUid != null && !hasBookmarksOwner()) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = bookmarksAuth.currentUser?.uid
        bookmarksOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("bookmarks_owner_uid")
        if (!hasBookmarksOwner()) { finish(); return }
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        SecurityHelper.applyScreenProtection(this)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = BookmarkAdapter(
            onOpenQuestion = { bookmark -> openQuestionInTest(bookmark) },
            onUnbookmark = onUnbookmark@{ bookmark ->
                if (!hasBookmarksOwner()) { finish(); return@onUnbookmark }
                val id = bookmark.questionId
                if (id.isBlank() || !pendingRemovalIds.add(id)) return@onUnbookmark

                // Keep the row visible until the Worker confirms deletion.
                // A denial or network failure must not display a false success.
                lifecycleScope.launch {
                    try {
                        val result = viewModel.unbookmark(id)
                        if (!hasBookmarksOwner() || isFinishing || isDestroyed) return@launch
                        if (result.isFailure) {
                            // UiArcSwipeRow commits/collapses before invoking its
                            // callback. Rebind the *same* row after server denial
                            // so it is neither stranded invisible nor falsely removed.
                            val failedIndex = adapter.currentList.indexOfFirst { it.questionId == id }
                            if (failedIndex >= 0) adapter.notifyItemChanged(failedIndex)
                            com.eve.app.util.AppBulletin.showError(
                                this@BookmarksActivity,
                                "Couldn't remove bookmark. Try again."
                            )
                            return@launch
                        }
                        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
                        com.eve.app.util.AppUndoBar.show(
                            context = this@BookmarksActivity,
                            message = "Bookmark removed",
                            timeLeftMs = com.eve.app.util.AppUndoBar.TIME_LIGHT,
                            onUndo = onUndo@{
                                if (!hasBookmarksOwner() || isFinishing || isDestroyed) {
                                    if (!hasBookmarksOwner()) finish()
                                    return@onUndo
                                }
                                lifecycleScope.launch {
                                    val restored = viewModel.bookmark(bookmark)
                                    if (!hasBookmarksOwner() || isFinishing || isDestroyed) return@launch
                                    if (restored.isSuccess) {
                                        // Refresh the same canonical server-backed list; no
                                        // second local bookmark state or speculative row.
                                        viewModel.load()
                                        com.eve.app.util.AppBulletin.show(
                                            this@BookmarksActivity, "Bookmark restored"
                                        )
                                    } else {
                                        com.eve.app.util.AppBulletin.showError(
                                            this@BookmarksActivity,
                                            "Couldn't restore bookmark. Try again."
                                        )
                                        viewModel.load()
                                    }
                                }
                            }
                        )
                    } finally {
                        pendingRemovalIds.remove(id)
                    }
                }
            },
            isHindi = { LanguageManager.isHindi(this) }
        )

        binding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        binding.rvBookmarks.adapter = adapter
        binding.rvBookmarks.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    com.eve.app.designsystem.UiArcSwipeRow.closeActiveRow()
                }
            }
        })

        binding.btnRetry.setOnClickListener { viewModel.load() }

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

    override fun onStart() {
        super.onStart()
        if (!hasBookmarksOwner()) { finish(); return }
        bookmarksAuth.addAuthStateListener(bookmarksAuthListener)
    }

    override fun onStop() {
        bookmarksAuth.removeAuthStateListener(bookmarksAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasBookmarksOwner()) { finish(); return }
        hasEmptyPlayed = false
        // Bookmarks may change in TestActivity using a separate repository
        // instance. Returning to this screen must refresh its server snapshot.
        if (hasResumedOnce) viewModel.load() else hasResumedOnce = true
    }

    override fun onPause() {
        super.onPause()
        com.eve.app.designsystem.UiArcSwipeRow.closeActiveRow()
    }

    override fun onDestroy() {
        com.eve.app.designsystem.UiArcSwipeRow.closeActiveRow()
        if (::binding.isInitialized) {
            EmptyStateAnimationHelper.stopEmptyState(binding.ivMessageIcon)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putString("bookmarks_owner_uid", bookmarksOwnerUid)
    }

    private fun renderState(state: UiState<List<BookmarkedQuestion>>) {
        if (!hasBookmarksOwner()) { finish(); return }
        when (state) {
            is UiState.Loading -> {
                hasEmptyPlayed = false
                if (adapter.itemCount == 0) {
                    binding.rvBookmarks.visibility = View.GONE
                    binding.progressGroup.visibility = View.VISIBLE
                }
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
                    EmptyStateAnimationHelper.stopEmptyState(binding.ivMessageIcon)
                    binding.tvBookmarkCount.visibility = View.VISIBLE
                    binding.tvBookmarkCount.text = "${state.data.size} saved"
                }
            }
            is UiState.Error -> {
                hasEmptyPlayed = false
                binding.progressGroup.visibility = View.GONE
                if (::adapter.isInitialized && adapter.itemCount > 0) {
                    binding.rvBookmarks.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                    com.eve.app.util.AppBulletin.showError(this@BookmarksActivity, "Couldn't refresh bookmarks: ${state.message}")
                } else {
                    binding.rvBookmarks.visibility = View.GONE
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                    EmptyStateAnimationHelper.showErrorState(binding.ivMessageIcon)
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
    }

    private fun openQuestionInTest(bookmark: BookmarkedQuestion) {
        if (!hasBookmarksOwner()) { finish(); return }
        if (bookmark.examId.isBlank() && bookmark.questionId.isBlank()) {
            com.eve.app.util.AppBulletin.showError(this, "Question details unavailable")
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
