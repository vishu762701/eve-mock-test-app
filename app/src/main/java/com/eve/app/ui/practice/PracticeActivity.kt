package com.eve.app.ui.practice

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
import com.eve.app.data.model.PracticeTopic
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
 * Topic Practice screen powered by real ported Telegram UI components:
 * ActionBar, RecyclerListView, AlertDialog (exam picker), HeaderCell, TextCell,
 * TextDetailSettingsCell, ShadowSectionCell, TextInfoPrivacyCell, and EmptyTextProgressView.
 */
class PracticeActivity : EveBaseActivity() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EXAM_PICKER = 1
        private const val VIEW_TYPE_SHADOW = 2
        private const val VIEW_TYPE_TOPIC = 3
        private const val VIEW_TYPE_INFO = 4
    }

    private val viewModel: PracticeViewModel by viewModels()
    private val adapter = TelegramPracticeAdapter()
    private var exams: List<Exam> = emptyList()
    private var selectedExam: Exam? = null
    private var currentTopics: List<PracticeTopic> = emptyList()

    private val practiceAuth = FirebaseAuth.getInstance()
    private var practiceOwnerUid: String? = null
    private fun hasPracticeOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(practiceOwnerUid, practiceAuth.currentUser?.uid)
    private val practiceAuthListener = FirebaseAuth.AuthStateListener {
        if (practiceOwnerUid != null && !hasPracticeOwner()) finish()
    }

    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = practiceAuth.currentUser?.uid
        practiceOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("practice_owner_uid")
        if (!hasPracticeOwner()) { finish(); return }

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
            setTitle("Topic Practice")
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

        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@PracticeActivity)
            adapter = this@PracticeActivity.adapter
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
            setText("Loading exams…")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.exams.collect(::renderExams) }
                launch { viewModel.topics.collect(::renderTopics) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPracticeOwner()) { finish(); return }
        practiceAuth.addAuthStateListener(practiceAuthListener)
    }

    override fun onStop() {
        practiceAuth.removeAuthStateListener(practiceAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPracticeOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("practice_owner_uid", practiceOwnerUid)
    }

    private fun renderExams(state: UiState<List<Exam>>) {
        if (!hasPracticeOwner()) return
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
                exams = state.data
                val prevSelectedId = selectedExam?.id
                selectedExam = exams.firstOrNull { it.id == prevSelectedId } ?: exams.firstOrNull()
                if (exams.isEmpty()) {
                    viewModel.loadTopics("")
                    currentTopics = emptyList()
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No exams are currently available for practice.")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                } else {
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                    selectedExam?.let {
                        actionBar.setSubtitle(it.examName)
                        viewModel.loadTopics(it.id)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun renderTopics(state: UiState<List<PracticeTopic>>) {
        if (!hasPracticeOwner()) return
        when (state) {
            is UiState.Loading -> {
                if (exams.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    emptyView.showProgress()
                }
            }
            is UiState.Error -> {
                if (currentTopics.isNotEmpty()) {
                    AppBulletin.showError(this, "Couldn't refresh topics: ${state.message}")
                } else {
                    currentTopics = emptyList()
                    adapter.notifyDataSetChanged()
                    AppBulletin.showError(this, state.message)
                }
            }
            is UiState.Success -> {
                currentTopics = state.data
                if (exams.isNotEmpty()) {
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showExamPicker() {
        if (!hasPracticeOwner() || exams.isEmpty()) return
        val names = exams.map { it.examName as CharSequence }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Exam")
            .setItems(names) { _, which ->
                val chosen = exams.getOrNull(which) ?: return@setItems
                selectedExam = chosen
                actionBar.setSubtitle(chosen.examName)
                viewModel.loadTopics(chosen.id)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startPractice(topic: PracticeTopic) {
        if (!hasPracticeOwner()) { finish(); return }
        val exam = selectedExam ?: return
        // Small set: maximum 10 random questions, 1 minute/question up to 10 minutes.
        val count = topic.questionCount.coerceAtMost(10)
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                .putExtra(Constants.EXTRA_TIME_LIMIT, count.coerceAtLeast(1))
                .putExtra(Constants.EXTRA_TOPIC, topic.name)
                .putExtra(Constants.EXTRA_PRACTICE_MODE, true)
        )
    }

    private inner class TelegramPracticeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        // 0: Header("Selected Exam")
        // 1: Exam Picker TextCell
        // 2: Shadow
        // 3: Header("Topics")
        // 4..4+topics-1: Topic rows (or single info row if empty)
        // last-1: Shadow (if topics non-empty)
        // last: Info
        override fun getItemCount(): Int {
            if (exams.isEmpty()) return 0
            return if (currentTopics.isEmpty()) 5 else 4 + currentTopics.size + 2
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0 || position == 3) return VIEW_TYPE_HEADER
            if (position == 1) return VIEW_TYPE_EXAM_PICKER
            if (position == 2) return VIEW_TYPE_SHADOW
            if (currentTopics.isEmpty()) return VIEW_TYPE_INFO
            val afterTopics = 4 + currentTopics.size
            return when {
                position < afterTopics -> VIEW_TYPE_TOPIC
                position == afterTopics -> VIEW_TYPE_SHADOW
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
                VIEW_TYPE_TOPIC -> TextDetailSettingsCell(parent.context).apply {
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
                    header.setText(if (position == 0) "Exam" else "Practice Topics")
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
                VIEW_TYPE_TOPIC -> {
                    val topic = currentTopics[position - 4]
                    val divider = position - 4 < currentTopics.lastIndex
                    val cell = holder.itemView as TextDetailSettingsCell
                    val qCount = topic.questionCount
                    cell.setTextAndValue(
                        topic.name,
                        "$qCount ${if (qCount == 1) "question" else "questions"} • Quick practice set",
                        divider
                    )
                    cell.setOnClickListener {
                        if (!hasPracticeOwner()) { finish(); return@setOnClickListener }
                        startPractice(topic)
                    }
                }
                VIEW_TYPE_INFO -> {
                    val cell = holder.itemView as TextInfoPrivacyCell
                    cell.setText(
                        if (currentTopics.isEmpty()) {
                            "No topic-tagged questions found for this exam. Please ask an admin to add topics."
                        } else {
                            "Each practice set picks up to 10 random questions from the chosen topic (1 minute per question)."
                        }
                    )
                }
            }
        }
    }
}
