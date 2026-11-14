package com.eve.app.ui.bookmarks

import com.eve.app.ui.common.EveBaseActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.databinding.ActivityBookmarksBinding
import com.eve.app.designsystem.TelegramPopupMenu
import com.eve.app.designsystem.TelegramShareAlert
import com.eve.app.designsystem.UiArcSwipeRow
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.EmptyStateAnimationHelper
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.UiState
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.VibrationHelper
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

    private var fullBookmarksList: List<BookmarkedQuestion> = emptyList()
    private var currentFilterTopic: String? = null
    private var currentSearchQuery: String = ""
    private var isSearchOpen: Boolean = false

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

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) { finish() }

        setupSearchAndFilters()
        setupSelectionBar()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isSelectionMode) {
                    exitSelectionMode()
                } else if (isSearchOpen) {
                    closeSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        adapter = BookmarkAdapter(
            onOpenQuestion = { bookmark -> openQuestionInTest(bookmark) },
            onUnbookmark = onUnbookmark@{ bookmark ->
                if (!hasBookmarksOwner()) { finish(); return@onUnbookmark }
                val id = bookmark.questionId
                if (id.isBlank() || !pendingRemovalIds.add(id)) return@onUnbookmark

                lifecycleScope.launch {
                    try {
                        val result = viewModel.unbookmark(id)
                        if (!hasBookmarksOwner() || isFinishing || isDestroyed) return@launch
                        if (result.isFailure) {
                            val failedIndex = adapter.currentList.indexOfFirst { it.questionId == id }
                            if (failedIndex >= 0) adapter.notifyItemChanged(failedIndex)
                            AppBulletin.showError(this@BookmarksActivity, "Couldn't remove bookmark. Try again.")
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
                                        viewModel.load()
                                        AppBulletin.show(this@BookmarksActivity, "Bookmark restored")
                                    } else {
                                        AppBulletin.showError(this@BookmarksActivity, "Couldn't restore bookmark. Try again.")
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
            isHindi = { LanguageManager.isHindi(this) },
            onCopyQuestion = { bookmark -> copyQuestion(bookmark) },
            onShareQuestion = { bookmark -> shareQuestion(bookmark) },
            onLongClickQuestion = { bookmark, anchor -> showBookmarkContextMenu(bookmark, anchor) }
        )

        binding.rvBookmarks.layoutManager = LinearLayoutManager(this)
        binding.rvBookmarks.adapter = adapter
        binding.rvBookmarks.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    UiArcSwipeRow.closeActiveRow()
                }
            }
        })

        binding.pullRefreshLayout.setOnRefreshListener { viewModel.load() }
        com.eve.app.designsystem.DesignPress.bind(binding.btnRetry) { viewModel.load() }

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

    private fun setupSearchAndFilters() {
        binding.btnSearch.setOnClickListener {
            isSearchOpen = true
            binding.toolbarNormal.visibility = View.GONE
            binding.toolbarSearch.visibility = View.VISIBLE
            binding.etSearchBookmarks.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etSearchBookmarks, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.btnSearchBack.setOnClickListener {
            closeSearch()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchBookmarks.setText("")
            currentSearchQuery = ""
            applyFilterAndSearch()
        }

        binding.etSearchBookmarks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (currentSearchQuery.isEmpty()) View.GONE else View.VISIBLE
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun applyFilterAndSearch() {
        var list = fullBookmarksList
        val filter = currentFilterTopic
        if (!filter.isNullOrBlank() && filter != "All") {
            list = list.filter { it.topic.equals(filter, ignoreCase = true) || it.examName.equals(filter, ignoreCase = true) }
        }
        val q = currentSearchQuery.trim()
        if (q.isNotBlank()) {
            val hindi = LanguageManager.isHindi(this)
            list = list.filter {
                it.displayQuestionText(hindi).contains(q, ignoreCase = true) ||
                it.displayOptionText("A", hindi).contains(q, ignoreCase = true) ||
                it.displayOptionText("B", hindi).contains(q, ignoreCase = true) ||
                it.displayOptionText("C", hindi).contains(q, ignoreCase = true) ||
                it.displayOptionText("D", hindi).contains(q, ignoreCase = true) ||
                it.topic.contains(q, ignoreCase = true) ||
                it.examName.contains(q, ignoreCase = true)
            }
        }
        adapter.submitList(list)
        val empty = list.isEmpty()
        binding.rvBookmarks.visibility = if (empty) View.GONE else View.VISIBLE
        binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) {
            if (fullBookmarksList.isNotEmpty()) {
                binding.tvMessage.text = "No matching questions"
                binding.tvMessageSub.text = "Try a different search query or filter pill."
            } else {
                binding.tvMessage.text = "No bookmarks yet"
                binding.tvMessageSub.text = "Questions bookmarked during tests will appear here for review."
            }
        }
        binding.tvBookmarkCount.text = "${list.size} saved"
    }

    private fun updateFilterChips(bookmarks: List<BookmarkedQuestion>) {
        val topics = bookmarks.mapNotNull { it.topic.takeIf { t -> t.isNotBlank() } }.distinct()
        if (topics.isNotEmpty()) {
            binding.filterScroll.visibility = View.VISIBLE
            binding.chipGroupFilters.removeAllViews()
            val allPills = listOf("All") + topics
            allPills.forEach { pill ->
                val chip = com.eve.app.designsystem.TelegramChip(this).apply {
                    text = pill
                    isCheckable = true
                    isChecked = (currentFilterTopic == null && pill == "All") || currentFilterTopic == pill
                    setOnClickListener {
                        VibrationHelper.vibrateLightHaptic(this@BookmarksActivity)
                        currentFilterTopic = if (pill == "All") null else pill
                        applyFilterAndSearch()
                    }
                }
                binding.chipGroupFilters.addView(chip)
            }
        } else {
            binding.filterScroll.visibility = View.GONE
        }
    }

    private fun copyQuestion(bookmark: BookmarkedQuestion) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val hindi = LanguageManager.isHindi(this)
        val text = buildString {
            append("Q. ").append(bookmark.displayQuestionText(hindi)).append("\n")
            append("A. ").append(bookmark.displayOptionText("A", hindi)).append("\n")
            append("B. ").append(bookmark.displayOptionText("B", hindi)).append("\n")
            append("C. ").append(bookmark.displayOptionText("C", hindi)).append("\n")
            append("D. ").append(bookmark.displayOptionText("D", hindi)).append("\n")
            if (bookmark.correctAnswer.isNotBlank()) {
                append("Correct: ").append(bookmark.correctAnswer).append("\n")
            }
        }
        cm.setPrimaryClip(ClipData.newPlainText("Question", text))
        AppBulletin.showSuccess(this, "Question copied to clipboard")
    }

    private fun shareQuestion(bookmark: BookmarkedQuestion) {
        val hindi = LanguageManager.isHindi(this)
        val shareAlert = TelegramShareAlert(
            context = this,
            title = bookmark.examName.ifBlank { "Saved Question" },
            subtitle = if (bookmark.questionNumber > 0) "Question ${bookmark.questionNumber}" else null,
            shareText = "${bookmark.displayQuestionText(hindi)}\n\nShared via Eve",
            shareUrl = null,
            previewBitmap = null
        )
        shareAlert.show()
    }

    private fun closeSearch() {
        isSearchOpen = false
        binding.etSearchBookmarks.setText("")
        currentSearchQuery = ""
        binding.toolbarSearch.visibility = View.GONE
        binding.toolbarNormal.visibility = View.VISIBLE
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearchBookmarks.windowToken, 0)
        applyFilterAndSearch()
    }

    private fun setupSelectionBar() {
        binding.selectionBar.onCloseClicked = {
            exitSelectionMode()
        }
        binding.selectionBar.addAction(
            id = 1,
            iconRes = R.drawable.msg_select,
            contentDescription = "Select All"
        ) {
            adapter.selectAll()
        }
        binding.selectionBar.addAction(
            id = 2,
            iconRes = R.drawable.ic_share,
            contentDescription = "Share Selected"
        ) {
            shareSelectedBookmarks()
        }
        binding.selectionBar.addAction(
            id = 3,
            iconRes = R.drawable.ic_delete,
            contentDescription = "Delete Selected",
            isDestructive = true
        ) {
            deleteSelectedBookmarks()
        }

        adapter.onSelectionChanged = { count ->
            if (count == 0) {
                exitSelectionMode()
            } else {
                binding.selectionBar.setSelectedCount(count, true)
            }
        }
    }

    private fun enterSelectionMode(initialSelectedId: String? = null) {
        if (adapter.isSelectionMode) return
        VibrationHelper.vibrateLightHaptic(this)
        adapter.isSelectionMode = true
        if (initialSelectedId != null) {
            adapter.selectedQuestionIds.add(initialSelectedId)
            adapter.notifyDataSetChanged()
        }
        binding.toolbarNormal.visibility = View.GONE
        binding.toolbarSearch.visibility = View.GONE
        binding.selectionBar.show()
        binding.selectionBar.setSelectedCount(adapter.selectedQuestionIds.size, false)
    }

    private fun exitSelectionMode() {
        if (!adapter.isSelectionMode) return
        VibrationHelper.vibrateLightHaptic(this)
        adapter.isSelectionMode = false
        binding.selectionBar.hide {
            binding.toolbarNormal.visibility = View.VISIBLE
        }
    }

    private fun shareSelectedBookmarks() {
        val selected = adapter.getSelectedBookmarks()
        if (selected.isEmpty()) return
        val hindi = LanguageManager.isHindi(this)
        val text = buildString {
            append("Eve Saved Questions (${selected.size}):\n\n")
            selected.forEachIndexed { i, q ->
                append("${i + 1}. ${q.displayQuestionText(hindi)}\n\n")
            }
            append("Shared via Eve")
        }
        val alert = TelegramShareAlert(
            context = this,
            title = "Saved Questions (${selected.size})",
            subtitle = null,
            shareText = text,
            shareUrl = null,
            previewBitmap = null
        )
        alert.show()
    }

    private fun deleteSelectedBookmarks() {
        val selected = adapter.getSelectedBookmarks()
        if (selected.isEmpty()) return
        exitSelectionMode()
        lifecycleScope.launch {
            selected.forEach { bookmark ->
                viewModel.unbookmark(bookmark.questionId)
            }
            AppBulletin.showSuccess(this@BookmarksActivity, "Removed ${selected.size} bookmarks")
        }
    }

    private fun showBookmarkContextMenu(bookmark: BookmarkedQuestion, anchor: View) {
        VibrationHelper.vibrateLightHaptic(this)
        val popup = TelegramPopupMenu(this)
        popup.addItem(1, "Select", R.drawable.msg_select) {
            enterSelectionMode(bookmark.questionId)
        }
        popup.addItem(2, "Open in Test", R.drawable.ic_back) {
            openQuestionInTest(bookmark)
        }
        popup.addItem(3, "Copy Question", R.drawable.ic_copy) {
            copyQuestion(bookmark)
        }
        popup.addItem(4, "Share Question", R.drawable.ic_share) {
            shareQuestion(bookmark)
        }
        popup.addItem(5, "Remove from Bookmarks", R.drawable.ic_delete, isDestructive = true) {
            removeBookmark(bookmark)
        }
        popup.showAsDropDown(anchor, 0, 0)
    }

    private fun removeBookmark(bookmark: BookmarkedQuestion) {
        if (!hasBookmarksOwner()) { finish(); return }
        val id = bookmark.questionId
        if (id.isBlank() || !pendingRemovalIds.add(id)) return

        lifecycleScope.launch {
            try {
                val result = viewModel.unbookmark(id)
                if (!hasBookmarksOwner() || isFinishing || isDestroyed) return@launch
                if (result.isFailure) {
                    val failedIndex = adapter.currentList.indexOfFirst { it.questionId == id }
                    if (failedIndex >= 0) adapter.notifyItemChanged(failedIndex)
                    AppBulletin.showError(this@BookmarksActivity, "Couldn't remove bookmark. Try again.")
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
                                viewModel.load()
                                AppBulletin.show(this@BookmarksActivity, "Bookmark restored")
                            } else {
                                AppBulletin.showError(this@BookmarksActivity, "Couldn't restore bookmark. Try again.")
                                viewModel.load()
                            }
                        }
                    }
                )
            } finally {
                pendingRemovalIds.remove(id)
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
        if (hasResumedOnce) viewModel.load() else hasResumedOnce = true
    }

    override fun onPause() {
        super.onPause()
        UiArcSwipeRow.closeActiveRow()
    }

    override fun onDestroy() {
        UiArcSwipeRow.closeActiveRow()
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
                binding.pullRefreshLayout.isRefreshing = false
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                fullBookmarksList = state.data
                updateFilterChips(state.data)
                applyFilterAndSearch()
                val empty = state.data.isEmpty()
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
                binding.pullRefreshLayout.isRefreshing = false
                binding.progressGroup.visibility = View.GONE
                if (::adapter.isInitialized && adapter.itemCount > 0) {
                    binding.rvBookmarks.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                    AppBulletin.showError(this@BookmarksActivity, "Couldn't refresh bookmarks: ${state.message}")
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
            AppBulletin.showError(this, "Question details unavailable")
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
