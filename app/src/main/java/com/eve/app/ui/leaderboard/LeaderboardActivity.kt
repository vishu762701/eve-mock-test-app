package com.eve.app.ui.leaderboard

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Leaderboard / Rank screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextDetailSettingsCell, ShadowSectionCell,
 * RecyclerListView, EmptyTextProgressView, TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class LeaderboardActivity : EveBaseActivity() {

    companion object {
        val inMemoryLeaderboardCache = mutableMapOf<String, List<LeaderboardEntry>>()
    }

    private val viewModel: LeaderboardViewModel by viewModels()
    private lateinit var examId: String

    private lateinit var adapter: LeaderboardAdapter

    private var hasEmptyPlayed = false
    private val leaderboardAuth = FirebaseAuth.getInstance()
    private var leaderboardOwnerUid: String? = null
    private fun hasLeaderboardOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(
            leaderboardOwnerUid, leaderboardAuth.currentUser?.uid)
    private val leaderboardAuthListener = FirebaseAuth.AuthStateListener {
        if (leaderboardOwnerUid != null && !hasLeaderboardOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var rankSectionContainer: LinearLayout
    private lateinit var rankDetailCell: TextDetailSettingsCell
    private lateinit var scorersHeader: HeaderCell
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUid = leaderboardAuth.currentUser?.uid
        leaderboardOwnerUid = if (savedInstanceState == null) currentUid
            else savedInstanceState.getString("leaderboard_owner_uid")
        if (!hasLeaderboardOwner()) { finish(); return }
        // The highlight identity must be the verified opening account, not an
        // earlier property-initializer snapshot of mutable FirebaseAuth.
        adapter = LeaderboardAdapter(leaderboardOwnerUid)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

        Theme.syncFromEvePalette(this)

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID) ?: "overall"
        val examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME)
        val subtitleText = if (examId == "overall" || examId.isBlank()) {
            examId = "overall"
            if (!examName.isNullOrBlank()) examName else "Overall Leaderboard"
        } else {
            if (!examName.isNullOrBlank()) examName else "Exam Leaderboard"
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        // 1. Real Telegram ActionBar
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Leaderboard")
            setSubtitle(subtitleText)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
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

        // 2. Your Standing Section (shown when current user has a rank)
        rankSectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        rankSectionContainer.addView(HeaderCell(this).apply {
            setText("Your Standing")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        rankDetailCell = TextDetailSettingsCell(this).apply {
            setMultilineDetail(true)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        rankSectionContainer.addView(rankDetailCell, matchWrap())
        rankSectionContainer.addView(ShadowSectionCell(this), matchWrap())
        root.addView(rankSectionContainer, matchWrap())

        // 3. Top Scorers Header + RecyclerListView + EmptyTextProgressView + Retry CTA
        scorersHeader = HeaderCell(this).apply {
            setText("Top Scorers")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
        }
        root.addView(scorersHeader, matchWrap())

        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@LeaderboardActivity)
            this.adapter = this@LeaderboardActivity.adapter
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
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
            setOnClickListener { viewModel.load(examId) }
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

        val cached = inMemoryLeaderboardCache[examId]
        if (!cached.isNullOrEmpty()) {
            adapter.submit(cached)
            scorersHeader.visibility = View.VISIBLE
            listView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.rankInfo.collect { renderRank(it) } }
            }
        }

        viewModel.load(examId)
    }

    override fun onStart() {
        super.onStart()
        if (!hasLeaderboardOwner()) { finish(); return }
        leaderboardAuth.addAuthStateListener(leaderboardAuthListener)
    }

    override fun onStop() {
        leaderboardAuth.removeAuthStateListener(leaderboardAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasLeaderboardOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putString("leaderboard_owner_uid", leaderboardOwnerUid)
    }

    private fun render(state: UiState<List<LeaderboardEntry>>) {
        if (!hasLeaderboardOwner()) { finish(); return }
        when (state) {
            is UiState.Loading -> {
                retryButton.visibility = View.GONE
                if (adapter.itemCount == 0) {
                    scorersHeader.visibility = View.GONE
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.showProgress()
                }
            }
            is UiState.Success -> {
                retryButton.visibility = View.GONE
                inMemoryLeaderboardCache[examId] = state.data
                adapter.submit(state.data)
                val empty = state.data.isEmpty()
                scorersHeader.visibility = if (empty) View.GONE else View.VISIBLE
                listView.visibility = if (empty) View.GONE else View.VISIBLE
                emptyView.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    hasEmptyPlayed = true
                    val title = if (examId == "overall") "No overall scores yet" else "No scores recorded yet"
                    val sub = if (examId == "overall") {
                        "Complete any test to see overall rankings here"
                    } else {
                        "Rankings will appear after the first attempt is submitted"
                    }
                    emptyView.setText("$title\n$sub")
                    emptyView.showTextView()
                } else {
                    hasEmptyPlayed = false
                }
            }
            is UiState.Error -> {
                if (adapter.itemCount > 0) {
                    scorersHeader.visibility = View.VISIBLE
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    AppBulletin.showError(this@LeaderboardActivity, "Couldn't refresh leaderboard: ${state.message}")
                } else {
                    scorersHeader.visibility = View.GONE
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    emptyView.setText("Something went wrong\n${state.message}")
                    emptyView.showTextView()
                }
            }
        }
    }

    private fun renderRank(rankInfo: RankInfo?) {
        if (!hasLeaderboardOwner()) { finish(); return }
        if (rankInfo == null) {
            rankSectionContainer.visibility = View.GONE
            return
        }
        rankSectionContainer.visibility = View.VISIBLE
        val percentileText = if (rankInfo.totalParticipants <= 1) {
            "You're the first to attempt this test"
        } else {
            val studentWord = if (rankInfo.totalParticipants == 1) "student" else "students"
            "Top ${rankInfo.topPercent}% of ${rankInfo.totalParticipants} $studentWord"
        }
        rankDetailCell.setTextAndValue(
            "Rank #${rankInfo.rank}  •  Score ${rankInfo.scoreText}/${rankInfo.total}",
            percentileText,
            false
        )
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
