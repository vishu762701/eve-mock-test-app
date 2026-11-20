package com.eve.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.TestAttempt
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.result.ResultActivity
import com.eve.app.ui.result.ResultDataHolder
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.NetworkUtil
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Test History screen powered by real ported Telegram UI components:
 * ActionBar, RecyclerListView, EmptyTextProgressView, HeaderCell,
 * TextDetailSettingsCell, ShadowSectionCell, TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class HistoryActivity : EveBaseActivity() {

    companion object {
        var cachedAttempts: List<TestAttempt>? = null
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ATTEMPT = 1
        private const val VIEW_TYPE_SHADOW = 2
        private const val VIEW_TYPE_INFO = 3
    }

    private val viewModel: HistoryViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private val historyAdapter = TelegramHistoryAdapter { attempt -> openReview(attempt) }

    private var hasEmptyPlayed = false
    private val historyAuth = FirebaseAuth.getInstance()
    private var historyOwnerUid: String? = null
    private fun hasHistoryOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(historyOwnerUid, historyAuth.currentUser?.uid)
    private val historyAuthListener = FirebaseAuth.AuthStateListener {
        if (historyOwnerUid != null && !hasHistoryOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var offlineBanner: TextView
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = historyAuth.currentUser?.uid
        historyOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("history_owner_uid")
        if (!hasHistoryOwner()) { finish(); return }
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

        Theme.syncFromEvePalette(this)

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
            setTitle("Test History")
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

        // 2. Offline indicator banner
        offlineBanner = TextView(this).apply {
            text = "You are offline. Showing cached test history."
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

        // 3. Content container with RecyclerListView + EmptyTextProgressView + Retry CTA
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
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
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
            setText("No test attempts yet\nSubmitted tests will appear here")
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
            setOnClickListener { viewModel.load() }
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

        cachedAttempts?.let { cached ->
            if (cached.isNotEmpty()) {
                historyAdapter.submit(cached)
                actionBar.setSubtitle("${cached.size} ${if (cached.size == 1) "attempt" else "attempts"}")
                listView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    NetworkUtil.observe(this@HistoryActivity).collect { online ->
                        offlineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasHistoryOwner()) { finish(); return }
        historyAuth.addAuthStateListener(historyAuthListener)
    }

    override fun onStop() {
        historyAuth.removeAuthStateListener(historyAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasHistoryOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putString("history_owner_uid", historyOwnerUid)
    }

    private fun render(state: UiState<List<TestAttempt>>) {
        if (!hasHistoryOwner()) { finish(); return }
        when (state) {
            is UiState.Loading -> {
                retryButton.visibility = View.GONE
                if (historyAdapter.attemptCount > 0) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.showProgress()
                }
            }
            is UiState.Success -> {
                retryButton.visibility = View.GONE
                cachedAttempts = state.data
                historyAdapter.submit(state.data)
                val empty = state.data.isEmpty()
                if (empty) {
                    hasEmptyPlayed = true
                    actionBar.setSubtitle(null)
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No test attempts yet\nSubmitted tests will appear here")
                    emptyView.showTextView()
                } else {
                    hasEmptyPlayed = false
                    actionBar.setSubtitle("${state.data.size} ${if (state.data.size == 1) "attempt" else "attempts"}")
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                }
            }
            is UiState.Error -> {
                if (historyAdapter.attemptCount > 0) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    AppBulletin.showError(this@HistoryActivity, "Couldn't refresh history: ${state.message}")
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    val msg = if (NetworkUtil.isOnline(this)) {
                        "Something went wrong\n${state.message}"
                    } else {
                        "No internet connection\nHistory has not been cached yet. Please retry when internet connection is restored."
                    }
                    emptyView.setText(msg)
                    emptyView.showTextView()
                }
            }
        }
    }

    private fun openReview(attempt: TestAttempt) {
        if (!hasHistoryOwner() || attempt.userId != historyOwnerUid) {
            if (!hasHistoryOwner()) finish()
            return
        }
        if (!ResultDataHolder.setAnswers(attempt.answers, attempt.userId, attempt.examId)) return
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putExtra(Constants.EXTRA_RESULT_OWNER_UID, attempt.userId)
                .putExtra(Constants.EXTRA_EXAM_ID, attempt.examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, attempt.examName)
                .putExtra(Constants.EXTRA_ATTEMPT_DATE, dateFormat.format(Date(attempt.timestamp)))
                .putExtra(Constants.EXTRA_FROM_HISTORY, true)
        )
    }

    private inner class TelegramHistoryAdapter(
        private val onClick: (TestAttempt) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<TestAttempt> = emptyList()

        val attemptCount: Int get() = items.size

        fun submit(list: List<TestAttempt>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int = when {
            position == 0 -> VIEW_TYPE_HEADER
            position <= items.size -> VIEW_TYPE_ATTEMPT
            position == items.size + 1 -> VIEW_TYPE_SHADOW
            else -> VIEW_TYPE_INFO
        }

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size + 3

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val ctx = parent.context
            val view: View = when (viewType) {
                VIEW_TYPE_HEADER -> HeaderCell(ctx).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                }
                VIEW_TYPE_ATTEMPT -> TextDetailSettingsCell(ctx).apply {
                    setMultilineDetail(true)
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                }
                VIEW_TYPE_SHADOW -> ShadowSectionCell(ctx)
                else -> TextInfoPrivacyCell(ctx)
            }
            view.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                VIEW_TYPE_HEADER -> {
                    (holder.itemView as HeaderCell).setText("Recent Attempts")
                }
                VIEW_TYPE_ATTEMPT -> {
                    val attempt = items[position - 1]
                    val cell = holder.itemView as TextDetailSettingsCell
                    val hasDivider = position < items.size
                    val dateStr = dateFormat.format(Date(attempt.timestamp))
                    val title = "${attempt.examName}  •  ${attempt.scoreText}/${attempt.total}"
                    val detail = "${attempt.category}  •  $dateStr\nCorrect: ${attempt.correct}   Wrong: ${attempt.wrong}   Unattempted: ${attempt.unattempted}"
                    cell.setTextAndValue(title, detail, hasDivider)
                    cell.setOnClickListener { onClick(attempt) }
                }
                VIEW_TYPE_INFO -> {
                    (holder.itemView as TextInfoPrivacyCell).setText(
                        "Tap any attempt to review your answers, explanations, and score breakdown."
                    )
                }
            }
        }
    }
}
