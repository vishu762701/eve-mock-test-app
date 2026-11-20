package com.eve.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.ExamRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

/**
 * Admin Manage Existing Exams screen powered by real ported Telegram UI components:
 * ActionBar (with live ActionBarMenuItem search + refresh), RecyclerListView,
 * and EmptyTextProgressView, while preserving ExistingExamsAdapter tree expansion and RBAC.
 */
class ManageExistingExamsActivity : EveBaseActivity() {

    companion object {
        private const val MENU_SEARCH = 1
        private const val MENU_REFRESH = 2
    }

    private val examRepo = ExamRepository()
    private val api = ApiClient.apiService
    private lateinit var adapter: ExistingExamsAdapter

    private var allExamsWithStats: List<ExamWithStats> = emptyList()
    private var currentQuery: String = ""
    private var hasEmptyPlayed = false
    private var adminAuthorized = false
    private var authorizedUid = ""
    private var authorizedEmail = ""
    private var reauthorizeOnResume = false
    private val expandedExamIds = linkedSetOf<String>()
    private var initializedTreeExpansion = false
    private var listLoadJob: kotlinx.coroutines.Job? = null

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        initializedTreeExpansion = savedInstanceState?.getBoolean("tree_expansion_initialized") ?: false
        expandedExamIds.addAll(savedInstanceState?.getStringArrayList("expanded_exam_ids").orEmpty())

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Manage Existing Exams")
            val menu = createMenu()
            val searchItem = menu.addItem(MENU_SEARCH, R.drawable.msg_search)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    override fun onSearchCollapse() {
                        currentQuery = ""
                        if (::adapter.isInitialized) filterExams("")
                    }

                    override fun onTextChanged(editText: EditText?) {
                        currentQuery = editText?.text?.toString().orEmpty()
                        if (::adapter.isInitialized) filterExams(currentQuery)
                    }
                })
            searchItem.setSearchFieldHint("Search exams…")
            menu.addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (adminAuthorized) loadExams()
                    }
                }
            })
        }
        rootView.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@ManageExistingExamsActivity)
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
            (itemAnimator as? DefaultItemAnimator)?.apply {
                addDuration = 250L
                removeDuration = 180L
            }
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
            setText("No exams found")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_exams") {
            val live = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            authorizedUid = live.uid
            authorizedEmail = live.email.orEmpty()
            adminAuthorized = true
            setupAdminControls()
            loadExams()
        }
    }

    override fun onStop() {
        reauthorizeOnResume = true
        deleteFlow.dismissActiveDialog()
        if (::adapter.isInitialized) adapter.dismissActivePopup()
        super.onStop()
    }

    override fun onDestroy() {
        listLoadJob?.cancel()
        listLoadJob = null
        deleteFlow.dismissActiveDialog()
        if (::adapter.isInitialized) adapter.dismissActivePopup()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        hasEmptyPlayed = false
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "manage_exams"
            ) {
                if (!adminAuthorized) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                loadExams()
            }
        }
    }

    private fun setupAdminControls() {
        adapter = ExistingExamsAdapter(
            onExamClick = { exam -> openEditExam(exam) },
            onEditClick = { exam -> openEditExam(exam) },
            onPreviewClick = { exam -> previewExam(exam) },
            onDeleteClick = { exam -> confirmDeleteExam(exam) },
            onExpandToggle = { exam ->
                if (!expandedExamIds.add(exam.id)) expandedExamIds.remove(exam.id)
                filterExams(currentQuery)
            }
        )
        listView.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putBoolean("tree_expansion_initialized", initializedTreeExpansion)
        outState.putStringArrayList("expanded_exam_ids", ArrayList(expandedExamIds))
    }

    private fun loadExams() {
        if (allExamsWithStats.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        listLoadJob?.cancel()
        listLoadJob = lifecycleScope.launch {
            try {
                val exams = examRepo.getExams()
                val attemptsMap = try {
                    val analyticsRes = api.getExamAnalytics()
                    analyticsRes.data?.associate { it.examId to it.attemptCount } ?: emptyMap()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyMap()
                }

                allExamsWithStats = exams.map { exam ->
                    ExamWithStats(
                        exam = exam,
                        attemptCount = attemptsMap[exam.id] ?: 0L,
                        questionCount = exam.questionCount
                    )
                }.sortedWith(compareBy<ExamWithStats> { it.exam.categoryOrOther }.thenBy { it.exam.examName })
                val realIds = allExamsWithStats.map { it.exam.id }.toSet()
                expandedExamIds.retainAll(realIds)
                if (!initializedTreeExpansion) {
                    expandedExamIds.addAll(
                        allExamsWithStats.filter { it.exam.parentExamId.isBlank() }.map { it.exam.id }
                    )
                    initializedTreeExpansion = true
                }

                filterExams(currentQuery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (allExamsWithStats.isNotEmpty()) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    AppBulletin.showError(
                        this@ManageExistingExamsActivity,
                        "Failed to load exams: ${e.toUserFriendlyMessage()}"
                    )
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Failed to load exams\n${e.toUserFriendlyMessage()}")
                    emptyView.showTextView()
                    AppBulletin.showError(
                        this@ManageExistingExamsActivity,
                        "Failed to load exams: ${e.toUserFriendlyMessage()}"
                    )
                }
            }
        }
    }

    private fun filterExams(query: String) {
        val filtered = ExamTree.flatten(allExamsWithStats, expandedExamIds, query)
        val oldIds = adapter.currentList.mapTo(mutableSetOf()) { it.exam.id }
        adapter.prepareEntrances(
            filtered.filter { it.treeDepth > 0 && it.exam.id !in oldIds }.mapTo(mutableSetOf()) { it.exam.id }
        )
        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            hasEmptyPlayed = true
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.setText(if (query.isNotBlank()) "No exams match \"$query\"" else "No exams found")
            emptyView.showTextView()
            actionBar.setSubtitle(null)
        } else {
            hasEmptyPlayed = false
            emptyView.visibility = View.GONE
            listView.visibility = View.VISIBLE
            actionBar.setSubtitle("${filtered.size} ${if (filtered.size == 1) "exam" else "exams"}")
        }
    }

    private fun openEditExam(exam: Exam) {
        val intent = Intent(this, EditExamActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, exam.id)
            putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
        }
        startActivity(intent)
    }

    private fun previewExam(exam: Exam) {
        val intent = Intent(this, TestActivity::class.java).apply {
            putExtra(Constants.EXTRA_EXAM_ID, exam.id)
            putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
            putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
            putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
            putExtra("is_preview", true)
        }
        startActivity(intent)
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDeleteExam(exam: Exam) {
        deleteFlow.confirm(exam.id, exam.examName, exam = true) { loadExams() }
    }
}
