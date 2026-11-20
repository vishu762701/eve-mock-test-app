package com.eve.app.ui.mistakes

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.MistakeItem
import com.eve.app.ui.admin.tg.TelegramSegmentedTabs
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.LanguageManager
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SecurityHelper
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Mistake Notebook screen powered by real ported Telegram UI components:
 * ActionBar, RecyclerListView, EmptyTextProgressView, ButtonWithCounterView,
 * and TelegramSegmentedTabs while preserving MistakesAdapter and owner isolation.
 */
class MistakesActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private lateinit var adapter: MistakesAdapter
    private var allMistakes: List<MistakeItem> = emptyList()
    private val learnedIds = mutableSetOf<String>()
    private var currentFilter: String = "ALL" // "ALL", "WRONG", "SKIPPED"
    private val mistakesAuth = FirebaseAuth.getInstance()
    private var mistakesOwnerUid: String? = null
    private var mistakeLoadJob: Job? = null
    private var mistakeLoadFailed = false

    private lateinit var actionBar: ActionBar
    private lateinit var offlineBanner: TextView
    private lateinit var filterTabs: TelegramSegmentedTabs
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    private fun hasMistakesOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(mistakesOwnerUid, mistakesAuth.currentUser?.uid)

    private val mistakesAccountListener = FirebaseAuth.AuthStateListener {
        if (mistakesOwnerUid != null && !hasMistakesOwner()) {
            mistakeLoadJob?.cancel()
            learnedIds.clear()
            allMistakes = emptyList()
            if (::listView.isInitialized) listView.visibility = View.GONE
            if (::adapter.isInitialized) adapter.submitList(emptyList())
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = mistakesAuth.currentUser?.uid
        mistakesOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("mistakes_owner_uid")
        if (!hasMistakesOwner()) { finish(); return }
        SecurityHelper.applyScreenProtection(this)

        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Mistake Notebook")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> loadMistakes()
                    }
                }
            })
        }
        root.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        offlineBanner = TextView(this).apply {
            text = "You are offline. Mistake Notebook requires a connection."
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
            setTextColor(Theme.getColor(Theme.key_statisticChartLine_orange))
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            gravity = Gravity.CENTER
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(8f)
            setPadding(padH, padV, padH, padV)
            visibility = View.GONE
        }
        root.addView(
            offlineBanner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        filterTabs = TelegramSegmentedTabs(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setTabs(listOf("All", "Wrong", "Skipped"))
            onTabSelectedListener = { pos ->
                if (!hasMistakesOwner()) {
                    finish()
                } else {
                    currentFilter = when (pos) {
                        1 -> "WRONG"
                        2 -> "SKIPPED"
                        else -> "ALL"
                    }
                    applyFilter()
                }
            }
        }
        root.addView(
            filterTabs,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        loadLearnedPreferences()

        adapter = MistakesAdapter(
            onToggleLearned = { item -> toggleLearned(item.questionId) },
            isLearned = { id -> learnedIds.contains(id) },
            isHindi = { LanguageManager.isHindi(this) }
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@MistakesActivity)
            adapter = this@MistakesActivity.adapter
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
            visibility = View.GONE
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("All Caught Up!\nQuestions you answer incorrectly or skip will appear here.")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        retryButton = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Retry", false)
            visibility = View.GONE
            setOnClickListener {
                if (hasMistakesOwner()) loadMistakes()
            }
        }
        contentFrame.addView(
            retryButton,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48f,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                24f,
                0f,
                24f,
                32f
            )
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    NetworkUtil.observe(this@MistakesActivity).collect { online ->
                        offlineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }

        loadMistakes()
    }

    private fun loadLearnedPreferences() {
        if (!hasMistakesOwner()) { finish(); return }
        val prefs = getSharedPreferences(
            MistakesPreferenceScope.namespaceForUser(requireNotNull(mistakesOwnerUid)),
            Context.MODE_PRIVATE
        )
        val saved = prefs.getStringSet("learned_ids", emptySet())
        if (saved != null) {
            learnedIds.clear()
            learnedIds.addAll(saved)
        }
    }

    private fun saveLearnedPreferences() {
        if (!hasMistakesOwner()) { finish(); return }
        val prefs = getSharedPreferences(
            MistakesPreferenceScope.namespaceForUser(requireNotNull(mistakesOwnerUid)),
            Context.MODE_PRIVATE
        )
        prefs.edit().putStringSet("learned_ids", HashSet(learnedIds)).apply()
    }

    private fun toggleLearned(questionId: String) {
        if (!hasMistakesOwner()) { finish(); return }
        if (questionId.isBlank()) return
        if (learnedIds.contains(questionId)) {
            learnedIds.remove(questionId)
        } else {
            learnedIds.add(questionId)
        }
        saveLearnedPreferences()
        val index = adapter.currentList.indexOfFirst { it.questionId == questionId }
        if (index >= 0) {
            adapter.notifyItemChanged(index, MistakesAdapter.PAYLOAD_LEARNED)
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    private fun loadMistakes() {
        if (!hasMistakesOwner()) { finish(); return }
        mistakeLoadJob?.cancel()
        val requestUid = requireNotNull(mistakesOwnerUid)
        mistakeLoadFailed = false
        retryButton.visibility = View.GONE
        if (allMistakes.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        mistakeLoadJob = lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getMistakes()
                if (!hasMistakesOwner() || requestUid != mistakesOwnerUid) {
                    finish()
                    return@launch
                }
                if (!resp.success || resp.data == null) {
                    throw IllegalStateException(resp.error ?: "Server mistakes response is incomplete")
                }
                allMistakes = resp.data
                updateFilterCounts()
                applyFilter()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (!hasMistakesOwner() || requestUid != mistakesOwnerUid) {
                    finish()
                    return@launch
                }
                showMistakeReadFailure()
            }
        }
    }

    private fun showMistakeReadFailure() {
        mistakeLoadFailed = true
        if (allMistakes.isNotEmpty()) {
            listView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            retryButton.visibility = View.GONE
            AppBulletin.showError(this@MistakesActivity, "Couldn't refresh mistakes")
            return
        }
        listView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.setText("Couldn't load mistakes\nCheck your connection and tap Retry.")
        emptyView.showTextView()
        retryButton.visibility = View.VISIBLE
    }

    private fun updateFilterCounts() {
        val wrongCount = allMistakes.count { it.selected.isNotBlank() }
        val skippedCount = allMistakes.count { it.selected.isBlank() }
        filterTabs.setTabs(
            listOf(
                "All (${allMistakes.size})",
                "Wrong ($wrongCount)",
                "Skipped ($skippedCount)"
            )
        )
    }

    override fun onStart() {
        super.onStart()
        if (!hasMistakesOwner()) { finish(); return }
        mistakesAuth.addAuthStateListener(mistakesAccountListener)
    }

    override fun onStop() {
        mistakesAuth.removeAuthStateListener(mistakesAccountListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasMistakesOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("mistakes_owner_uid", mistakesOwnerUid)
    }

    private fun applyFilter() {
        if (!hasMistakesOwner()) { finish(); return }
        if (mistakeLoadFailed) return
        val filtered = when (currentFilter) {
            "WRONG" -> allMistakes.filter { it.selected.isNotBlank() }
            "SKIPPED" -> allMistakes.filter { it.selected.isBlank() }
            else -> allMistakes
        }

        adapter.submitList(filtered)
        retryButton.visibility = View.GONE
        actionBar.setSubtitle("${filtered.size} ${if (filtered.size == 1) "question" else "questions"}")

        if (filtered.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            if (allMistakes.isEmpty()) {
                emptyView.setText("All Caught Up!\nNo mistakes recorded yet.")
            } else {
                emptyView.setText("No Matching Questions\nThere are no questions in this category.")
            }
            emptyView.showTextView()
        } else {
            emptyView.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mistakeLoadJob?.cancel()
    }
}
