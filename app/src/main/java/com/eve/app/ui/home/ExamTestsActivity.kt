package com.eve.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.local.TestSessionStore
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.AttemptKey
import com.eve.app.util.Constants
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.TestScheduleHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.IconBackgroundColors
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Exam Tests & Sub-Exams screen powered by real ported Telegram UI components:
 * ActionBar, RecyclerListView, EmptyTextProgressView, HeaderCell, TextCell,
 * TextDetailSettingsCell, ShadowSectionCell, TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class ExamTestsActivity : EveBaseActivity() {

    private val examRepo = ExamRepository()
    private val adapter = TelegramExamTestsAdapter()

    private var examId: String = ""
    private var examName: String = ""
    private var currentExam: Exam? = null
    private var loadJob: kotlinx.coroutines.Job? = null
    private var lastLoadedTime = 0L

    private val examTestsAuth = FirebaseAuth.getInstance()
    private var examTestsOwnerUid: String? = null
    private fun hasExamTestsOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(examTestsOwnerUid, examTestsAuth.currentUser?.uid)
    private val examTestsAuthListener = FirebaseAuth.AuthStateListener {
        if (examTestsOwnerUid != null && !hasExamTestsOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = examTestsAuth.currentUser?.uid
        examTestsOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("exam_tests_owner_uid")
        if (!hasExamTestsOwner()) { finish(); return }

        examId = intent.getStringExtra(Constants.EXTRA_EXAM_ID).orEmpty()
        examName = intent.getStringExtra(Constants.EXTRA_EXAM_NAME).orEmpty()

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
            setTitle(examName.ifBlank { "Tests" })
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> loadData(showLoading = true)
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

        // 2. Content frame with RecyclerListView + EmptyTextProgressView + Retry CTA
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
            layoutManager = LinearLayoutManager(this@ExamTestsActivity)
            adapter = this@ExamTestsActivity.adapter
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
            setText("No tests available yet\nPublished tests and sub-exams will appear here.")
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
            setOnClickListener { loadData(showLoading = true) }
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

        val cached = inMemoryCache[examId]
        if (!cached.isNullOrEmpty()) {
            adapter.submit(cached)
            listView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(30000L)
                    loadData(showLoading = false)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasExamTestsOwner()) { finish(); return }
        examTestsAuth.addAuthStateListener(examTestsAuthListener)
    }

    override fun onStop() {
        examTestsAuth.removeAuthStateListener(examTestsAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasExamTestsOwner()) { finish(); return }
        if (!::listView.isInitialized) return
        val now = System.currentTimeMillis()
        if (adapter.rawCount() == 0) {
            loadData(showLoading = true)
        } else if (now - lastLoadedTime > 15_000L && loadJob?.isActive != true) {
            loadData(showLoading = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("exam_tests_owner_uid", examTestsOwnerUid)
    }

    override fun onDestroy() {
        loadJob?.cancel()
        loadJob = null
        super.onDestroy()
    }

    private fun loadData(showLoading: Boolean = true) {
        if (!hasExamTestsOwner()) return
        val hasCachedItems = adapter.rawCount() > 0
        if (showLoading) {
            retryButton.visibility = View.GONE
            if (!hasCachedItems) {
                listView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.showProgress()
            }
        }

        loadJob?.cancel()
        loadJob = lifecycleScope.launch {
            try {
                val allExams = examRepo.getExams()
                currentExam = allExams.find { it.id == examId }
                if (currentExam != null && examName.isBlank()) {
                    examName = currentExam!!.examName
                    actionBar.setTitle(examName)
                }

                // 1. Direct sub-exams
                val children = allExams.filter { it.parentExamId == examId }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.examName })
                val subExamItems = children.map { ExamTestsListItem.SubExamItem(it) }

                // 2. Direct tests for this exam
                val liveTests = examRepo.getLiveGeneratedTests(examId)
                val sortedTests = liveTests.sortedWith(
                    compareBy<GeneratedTest> {
                        extractEndingInteger(it.testNumber) ?: Int.MAX_VALUE
                    }.thenBy { it.generatedAt }
                )

                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val attemptedLocks = if (uid.isNotBlank()) {
                    try { examRepo.getAttemptedExamIds(uid) } catch (_: Exception) { emptyList() }
                } else emptyList()

                val durationMinutes = currentExam?.timeLimitMinutes ?: 30
                val sessionStore = TestSessionStore(this@ExamTestsActivity)
                val serverNow = System.currentTimeMillis()

                val testItems = sortedTests.map { test ->
                    val testKey = AttemptKey.forTest(examId, test.id)
                    val isCompleted = attemptedLocks.contains(testKey) || HomeViewModel.isAttemptSubmitted(testKey)
                    val isResume = !isCompleted && sessionStore.hasSession(testKey)
                    val isLocked = !isCompleted && TestScheduleHelper.isLocked(test.availableFrom, serverNow)
                    val qCount = if (test.questionCount > 0) test.questionCount else test.questions.size
                    val subtitle = "$qCount questions \u2022 $durationMinutes minutes"
                    val opensText = if (isLocked) TestScheduleHelper.formatOpensAt(test.availableFrom) else ""
                    val title = test.testNumber.ifBlank { "Test" }

                    ExamTestsListItem.TestItem(
                        test = test,
                        title = title,
                        subtitle = subtitle,
                        isCompleted = isCompleted,
                        isResume = isResume,
                        isLocked = isLocked,
                        opensText = opensText
                    )
                }

                val combinedItems = buildExamTestsList(testItems, subExamItems)
                retryButton.visibility = View.GONE

                if (combinedItems.isEmpty()) {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No tests available yet\nPublished tests and sub-exams will appear here.")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                    adapter.submit(emptyList())
                } else {
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                    val totalCount = testItems.size + subExamItems.size
                    actionBar.setSubtitle("$totalCount ${if (totalCount == 1) "item" else "items"}")
                    adapter.submit(combinedItems)
                    inMemoryCache[examId] = combinedItems
                }
                lastLoadedTime = System.currentTimeMillis()
            } catch (e: Exception) {
                if (hasCachedItems) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    AppBulletin.showError(
                        this@ExamTestsActivity,
                        "Couldn't refresh tests: ${e.localizedMessage ?: "Unknown error"}"
                    )
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Couldn't load tests\n${e.localizedMessage ?: "Unknown error"}")
                    emptyView.showTextView()
                    retryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun handleTestClick(test: GeneratedTest, isCompleted: Boolean) {
        if (!hasExamTestsOwner()) { finish(); return }
        val testKey = AttemptKey.forTest(examId, test.id)
        val title = if (test.displayTitle.isNotBlank()) {
            test.displayTitle
        } else {
            "$examName - ${test.testNumber}"
        }

        if (isCompleted) {
            ExamLaunchHelper.openPreviousAttemptResult(
                context = this,
                scope = lifecycleScope,
                examId = testKey,
                examName = title,
                exam = currentExam,
                onLoading = { loading ->
                    if (loading) {
                        AppBulletin.show(this@ExamTestsActivity, "Loading result…")
                    }
                }
            )
        } else {
            val intent = Intent(this, TestActivity::class.java).apply {
                putExtra(Constants.EXTRA_EXAM_ID, testKey)
                putExtra(Constants.EXTRA_EXAM_NAME, title)
                putExtra(Constants.EXTRA_EXAM_CATEGORY, currentExam?.categoryOrOther ?: "Other")
                putExtra(Constants.EXTRA_TIME_LIMIT, currentExam?.timeLimitMinutes ?: 30)
            }
            startActivity(intent)
        }
    }

    private inner class TelegramExamTestsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val items = mutableListOf<ExamTestsListItem>()

        fun rawCount(): Int = items.size

        fun submit(newItems: List<ExamTestsListItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size + 2

        override fun getItemViewType(position: Int): Int {
            if (position == items.size) return VIEW_TYPE_SHADOW
            if (position == items.size + 1) return VIEW_TYPE_INFO
            return when (items[position]) {
                is ExamTestsListItem.HeaderItem -> VIEW_TYPE_HEADER
                is ExamTestsListItem.SubExamItem -> VIEW_TYPE_SUB_EXAM
                is ExamTestsListItem.TestItem -> VIEW_TYPE_TEST
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = when (viewType) {
                VIEW_TYPE_HEADER -> HeaderCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                }
                VIEW_TYPE_SUB_EXAM -> TextCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                }
                VIEW_TYPE_TEST -> TextDetailSettingsCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                    setMultilineDetail(true)
                }
                VIEW_TYPE_SHADOW -> ShadowSectionCell(parent.context)
                else -> TextInfoPrivacyCell(parent.context)
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
                    val header = items[position] as ExamTestsListItem.HeaderItem
                    (holder.itemView as HeaderCell).setText(header.title)
                }
                VIEW_TYPE_SUB_EXAM -> {
                    val sub = (items[position] as ExamTestsListItem.SubExamItem).exam
                    val divider = position + 1 < items.size && items[position + 1] !is ExamTestsListItem.HeaderItem
                    val cell = holder.itemView as TextCell
                    cell.setTextAndColorfulIcon(
                        sub.examName,
                        R.drawable.msg_info,
                        IconBackgroundColors.BLUE.bottom,
                        divider
                    )
                    cell.setOnClickListener {
                        if (!hasExamTestsOwner()) { finish(); return@setOnClickListener }
                        val intent = Intent(this@ExamTestsActivity, ExamTestsActivity::class.java).apply {
                            putExtra(Constants.EXTRA_EXAM_ID, sub.id)
                            putExtra(Constants.EXTRA_EXAM_NAME, sub.examName)
                        }
                        startActivity(intent)
                    }
                }
                VIEW_TYPE_TEST -> {
                    val item = items[position] as ExamTestsListItem.TestItem
                    val divider = position + 1 < items.size && items[position + 1] !is ExamTestsListItem.HeaderItem
                    val cell = holder.itemView as TextDetailSettingsCell
                    val stateLabel = when {
                        item.isLocked -> item.opensText.ifBlank { "Scheduled" }
                        item.isCompleted -> "Completed • Tap to view result"
                        item.isResume -> "In Progress • Tap to resume"
                        else -> "Tap to start"
                    }
                    cell.setTextAndValue(
                        item.title,
                        "${item.subtitle}\n$stateLabel",
                        divider
                    )
                    cell.setOnClickListener {
                        if (!hasExamTestsOwner()) { finish(); return@setOnClickListener }
                        if (item.isLocked) {
                            AppBulletin.show(this@ExamTestsActivity, item.opensText.ifBlank { "This test is locked." })
                        } else {
                            handleTestClick(item.test, item.isCompleted)
                        }
                    }
                }
                VIEW_TYPE_INFO -> {
                    (holder.itemView as TextInfoPrivacyCell).setText(
                        "Scheduled tests unlock automatically when their start window begins."
                    )
                }
            }
        }
    }

    companion object {
        private const val MENU_REFRESH = 1
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SUB_EXAM = 1
        private const val VIEW_TYPE_TEST = 2
        private const val VIEW_TYPE_SHADOW = 3
        private const val VIEW_TYPE_INFO = 4

        private val inMemoryCache = mutableMapOf<String, List<ExamTestsListItem>>()

        fun extractEndingInteger(testNumber: String): Int? {
            val match = Regex("""(\d+)\s*$""").find(testNumber.trim())
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

        fun buildExamTestsList(
            testItems: List<ExamTestsListItem.TestItem>,
            subExamItems: List<ExamTestsListItem.SubExamItem>
        ): List<ExamTestsListItem> {
            val result = mutableListOf<ExamTestsListItem>()
            if (testItems.isNotEmpty() && subExamItems.isNotEmpty()) {
                result.add(ExamTestsListItem.HeaderItem("Tests"))
                result.addAll(testItems)
                result.add(ExamTestsListItem.HeaderItem("Sub-Exams"))
                result.addAll(subExamItems)
            } else if (testItems.isNotEmpty()) {
                result.addAll(testItems)
            } else if (subExamItems.isNotEmpty()) {
                result.addAll(subExamItems)
            }
            return result
        }
    }
}
