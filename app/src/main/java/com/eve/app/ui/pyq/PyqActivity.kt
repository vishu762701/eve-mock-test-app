package com.eve.app.ui.pyq

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.ui.admin.tg.TelegramSegmentedTabs
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import com.eve.app.util.SubmissionRetryPolicy
import com.eve.app.util.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.AlertDialog
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

/**
 * Previous Year Questions (PYQ) screen powered by real ported Telegram UI components:
 * ActionBar, AlertDialog (exam picker), RecyclerListView, HeaderCell, TextCell,
 * TextDetailSettingsCell, ShadowSectionCell, TextInfoPrivacyCell, EmptyTextProgressView,
 * and TelegramSegmentedTabs for year selection.
 */
class PyqActivity : EveBaseActivity() {

    private val viewModel: PyqViewModel by viewModels()
    private val adapter = TelegramPyqAdapter()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null
    private var sets: List<PyqSet> = emptyList()
    private var yearList: List<Int> = emptyList()
    private var selectedYear: Int? = null
    private var displayedPapers: List<Pair<String, PyqSet>> = emptyList()
    private var restoredExamId: String? = null

    private val pyqAuth = FirebaseAuth.getInstance()
    private var pyqOwnerUid: String? = null
    private fun hasPyqOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(pyqOwnerUid, pyqAuth.currentUser?.uid)
    private val pyqAuthListener = FirebaseAuth.AuthStateListener {
        if (pyqOwnerUid != null && !hasPyqOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var yearTabs: TelegramSegmentedTabs
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = pyqAuth.currentUser?.uid
        pyqOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("pyq_owner_uid")
        if (!hasPyqOwner()) { finish(); return }

        restoredExamId = savedInstanceState?.getString(STATE_EXAM_ID)
        selectedYear = savedInstanceState?.takeIf { it.containsKey(STATE_YEAR) }?.getInt(STATE_YEAR)

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
            setTitle("Previous Year Papers")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> {
                            val exam = selectedExam
                            if (exam != null) viewModel.loadSets(exam) else viewModel.loadExams()
                        }
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

        yearTabs = TelegramSegmentedTabs(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            visibility = View.GONE
            onTabSelectedListener = { pos ->
                yearList.getOrNull(pos)?.let { year ->
                    selectedYear = year
                    renderPapers(year)
                }
            }
        }
        root.addView(
            yearTabs,
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

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@PyqActivity)
            adapter = this@PyqActivity.adapter
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
            setText("Loading previous year papers…")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.exams.collect(::renderExams) }
                launch { viewModel.sets.collect(::renderSets) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPyqOwner()) { finish(); return }
        pyqAuth.addAuthStateListener(pyqAuthListener)
    }

    override fun onStop() {
        pyqAuth.removeAuthStateListener(pyqAuthListener)
        super.onStop()
    }

    private fun isCurrentOwner(): Boolean = hasPyqOwner()

    override fun onResume() {
        super.onResume()
        if (!hasPyqOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pyq_owner_uid", pyqOwnerUid)
        (viewModel.selectedExamId ?: selectedExam?.id)?.let {
            outState.putString(STATE_EXAM_ID, it)
        }
        selectedYear?.let { outState.putInt(STATE_YEAR, it) }
    }

    private fun showExamPicker() {
        if (!hasPyqOwner() || exams.isEmpty()) return
        val names = exams.map { it.examName as CharSequence }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Exam")
            .setItems(names) { _, which ->
                val next = exams.getOrNull(which) ?: return@setItems
                if (viewModel.selectedExamId != next.id) {
                    selectedYear = null
                    sets = emptyList()
                    displayedPapers = emptyList()
                }
                selectedExam = next
                actionBar.setSubtitle(next.examName)
                viewModel.loadSets(next)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderExams(state: UiState<List<Exam>>) {
        if (!isCurrentOwner()) return
        when (state) {
            is UiState.Loading -> {
                if (exams.isEmpty()) {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.showProgress()
                }
            }
            is UiState.Error -> {
                if (exams.isNotEmpty()) {
                    AppBulletin.showError(this, "Couldn't refresh exams: ${state.message}")
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText(state.message)
                    emptyView.showTextView()
                }
            }
            is UiState.Success -> {
                val wantedId = viewModel.selectedExamId ?: restoredExamId ?: selectedExam?.id
                exams = state.data
                if (exams.isEmpty()) {
                    selectedExam = null
                    sets = emptyList()
                    displayedPapers = emptyList()
                    yearTabs.visibility = View.GONE
                    viewModel.clearSelection()
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No exams are currently available for previous year questions.")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                    restoredExamId = null
                    return
                }
                val index = exams.indexOfFirst { it.id == wantedId }.coerceAtLeast(0)
                val exam = exams[index]
                if (wantedId != exam.id) selectedYear = null
                selectedExam = exam
                actionBar.setSubtitle(exam.examName)
                emptyView.visibility = View.GONE
                listView.visibility = View.VISIBLE
                viewModel.loadSets(exam)
                adapter.notifyDataSetChanged()
                restoredExamId = null
            }
        }
    }

    private fun renderSets(state: UiState<List<PyqSet>>) {
        if (!isCurrentOwner() || selectedExam?.id != viewModel.selectedExamId) return
        when (state) {
            is UiState.Loading -> {
                if (sets.isEmpty()) {
                    yearTabs.visibility = View.GONE
                }
            }
            is UiState.Error -> {
                if (sets.isNotEmpty()) {
                    AppBulletin.showError(this, "Couldn't refresh sets: ${state.message}")
                } else {
                    sets = emptyList()
                    displayedPapers = emptyList()
                    yearTabs.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    AppBulletin.showError(this, state.message)
                }
            }
            is UiState.Success -> {
                sets = state.data
                if (sets.isEmpty()) {
                    yearList = emptyList()
                    displayedPapers = emptyList()
                    yearTabs.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    return
                }
                val years = sets.groupBy { it.year }.toSortedMap(compareByDescending { it })
                yearList = years.keys.toList()
                val tabLabels = years.map { (year, yearSets) ->
                    "$year (${yearSets.sumOf { it.questionCount }})"
                }
                yearTabs.setTabs(tabLabels)
                yearTabs.visibility = View.VISIBLE
                val yearToShow = selectedYear?.takeIf { it in years } ?: years.keys.first()
                selectedYear = yearToShow
                renderPapers(yearToShow)
            }
        }
    }

    private fun renderPapers(year: Int) {
        if (!isCurrentOwner() || selectedExam?.id != viewModel.selectedExamId) return
        val yearSets = sets.filter { it.examId == selectedExam?.id && it.year == year }
        val list = mutableListOf<Pair<String, PyqSet>>()
        yearSets.forEach { set ->
            val label = if (set.paper.isBlank()) "Paper" else set.paper
            list.add(label to set)
        }
        val exam = selectedExam
        if (yearSets.size > 1 && exam != null) {
            val combinedCount = yearSets.sumOf { it.questionCount }
            list.add(
                "Saare papers ($year)" to PyqSet(
                    examId = exam.id,
                    examName = exam.examName,
                    category = exam.categoryOrOther,
                    year = year,
                    paper = "",
                    questionCount = combinedCount,
                    timeLimitMinutes = exam.timeLimitMinutes.coerceAtLeast(1)
                )
            )
        }
        displayedPapers = list
        adapter.notifyDataSetChanged()
    }

    private fun startPyq(set: PyqSet) {
        val exam = selectedExam ?: return
        if (!isCurrentOwner() || viewModel.selectedExamId != exam.id ||
            set.examId != exam.id || set.year != selectedYear
        ) return
        val yearSets = sets.filter { it.examId == exam.id && it.year == set.year }
        val validCombined = yearSets.size > 1 && set.paper.isBlank() &&
            set.examName == exam.examName && set.category == exam.categoryOrOther &&
            set.questionCount == yearSets.sumOf { it.questionCount } &&
            set.timeLimitMinutes == exam.timeLimitMinutes.coerceAtLeast(1)
        if (set !in yearSets && !validCombined) return
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, set.examId)
                .putExtra(Constants.EXTRA_EXAM_NAME, set.examName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, set.category)
                .putExtra(Constants.EXTRA_TIME_LIMIT, set.timeLimitMinutes)
                .putExtra(Constants.EXTRA_PYQ_MODE, true)
                .putExtra(Constants.EXTRA_PYQ_YEAR, set.year)
                .putExtra(Constants.EXTRA_PYQ_PAPER, set.paper)
        )
    }

    private inner class TelegramPyqAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount(): Int {
            if (exams.isEmpty()) return 0
            return if (displayedPapers.isEmpty()) 5 else 4 + displayedPapers.size + 2
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == 3) return VIEW_TYPE_HEADER
            if (position == 1) return VIEW_TYPE_EXAM_PICKER
            if (position == 2) return VIEW_TYPE_SHADOW
            if (displayedPapers.isEmpty()) return VIEW_TYPE_INFO
            val afterPapers = 4 + displayedPapers.size
            return when {
                position < afterPapers -> VIEW_TYPE_PAPER
                position == afterPapers -> VIEW_TYPE_SHADOW
                else -> VIEW_TYPE_INFO
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = when (viewType) {
                VIEW_TYPE_HEADER -> HeaderCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                }
                VIEW_TYPE_EXAM_PICKER -> TextCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                }
                VIEW_TYPE_PAPER -> TextDetailSettingsCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
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
                    val header = holder.itemView as HeaderCell
                    header.setText(if (position == 0) "Exam" else "Papers (${selectedYear ?: "--"})")
                }
                VIEW_TYPE_EXAM_PICKER -> {
                    val cell = holder.itemView as TextCell
                    cell.setTextAndColorfulIcon(
                        selectedExam?.examName ?: "Choose Exam",
                        R.drawable.msg_info,
                        IconBackgroundColors.BLUE.bottom,
                        false
                    )
                    cell.setOnClickListener { showExamPicker() }
                }
                VIEW_TYPE_PAPER -> {
                    val (label, pyqSet) = displayedPapers[position - 4]
                    val divider = position - 4 < displayedPapers.lastIndex
                    val cell = holder.itemView as TextDetailSettingsCell
                    cell.setTextAndValue(label, pyqSet.subtitle, divider)
                    cell.setOnClickListener {
                        if (!hasPyqOwner()) { finish(); return@setOnClickListener }
                        startPyq(pyqSet)
                    }
                }
                VIEW_TYPE_INFO -> {
                    val cell = holder.itemView as TextInfoPrivacyCell
                    cell.setText(
                        if (displayedPapers.isEmpty()) {
                            "No tagged PYQs found for this exam. Upload questions tagged with Previous Year Question and year."
                        } else {
                            "Tap any paper to start a timed previous-year practice test."
                        }
                    )
                }
            }
        }
    }

    private companion object {
        const val MENU_REFRESH = 1
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_EXAM_PICKER = 1
        const val VIEW_TYPE_SHADOW = 2
        const val VIEW_TYPE_PAPER = 3
        const val VIEW_TYPE_INFO = 4
        const val STATE_EXAM_ID = "pyq:selected-exam"
        const val STATE_YEAR = "pyq:selected-year"
    }
}
