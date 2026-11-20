package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.TestScheduleHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

/**
 * Admin Generated Tests screen powered by real ported Telegram UI components:
 * ActionBar (with refresh), RecyclerListView, and EmptyTextProgressView,
 * while preserving GeneratedTestAdapter, live/pause toggle, scheduling, preview, and RBAC.
 */
class GeneratedTestsActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private val examRepo = ExamRepository()
    private var authorizedUid: String = ""
    private var authorizedEmail: String = ""
    private var adminReady: Boolean = false
    private var reauthorizeOnResume: Boolean = false
    private var reloadAfterStop: Boolean = false
    private var previewDialog: android.app.Dialog? = null
    private var hasEmptyPlayed = false
    private var listLoadJob: kotlinx.coroutines.Job? = null

    private lateinit var rootView: LinearLayout
    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView

    private fun ownerCurrent(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser ?: return false
        return current.uid == authorizedUid && current.email.orEmpty() == authorizedEmail
    }

    private val adapter = GeneratedTestAdapter(
        onStatusToggle = { test, isLive -> toggleTestStatus(test, isLive) },
        onPreview = { test -> previewTest(test) },
        onDelete = { test -> confirmDelete(test) },
        onSchedule = { test -> scheduleTest(test) }
    )

    private fun scheduleTest(test: GeneratedTest) {
        if (!ownerCurrent()) return
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Opening Date")
            .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateUtc ->
            val timePicker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
                .setHour(9)
                .setMinute(0)
                .setTitleText("Select Opening Time")
                .build()

            timePicker.addOnPositiveButtonClickListener timeSelected@{
                val targetMillis = try {
                    TestScheduleHelper.localOpeningTimeFromPicker(
                        selectedDateUtc, timePicker.hour, timePicker.minute
                    )
                } catch (_: IllegalArgumentException) {
                    AppBulletin.showError(
                        this@GeneratedTestsActivity,
                        "This local opening time does not exist. Select a different time."
                    )
                    return@timeSelected
                }

                lifecycleScope.launch {
                    try {
                        examRepo.scheduleGeneratedTest(test.id, targetMillis)
                        AppBulletin.showSuccess(
                            this@GeneratedTestsActivity,
                            "Test scheduled: ${TestScheduleHelper.formatOpensAt(targetMillis)}"
                        )
                        loadTests()
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (e: Exception) {
                        AppBulletin.showError(
                            this@GeneratedTestsActivity,
                            "Schedule unconfirmed: ${e.localizedMessage}"
                        )
                    }
                }
            }
            timePicker.show(supportFragmentManager, "time_picker")
        }
        datePicker.show(supportFragmentManager, "date_picker")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

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
            setTitle("Generated Tests")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (adminReady) loadTests()
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
            layoutManager = LinearLayoutManager(this@GeneratedTestsActivity)
            adapter = this@GeneratedTestsActivity.adapter
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
            setText("No generated tests yet")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_generated_tests") {
            val current = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            authorizedUid = current.uid
            authorizedEmail = current.email.orEmpty()
            adminReady = true
            if (!ownerCurrent()) {
                finish()
                return@requireAdmin
            }
            loadTests()
        }
    }

    override fun onStart() {
        super.onStart()
        if (adminReady && !ownerCurrent()) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        hasEmptyPlayed = false
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            val hasItems = adapter.itemCount > 0
            if (!hasItems) {
                listView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.showProgress()
            }
            AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_generated_tests") {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                if (reloadAfterStop) {
                    reloadAfterStop = false
                    loadTests(showProgress = !hasItems)
                }
            }
        }
    }

    override fun onStop() {
        reauthorizeOnResume = true
        reloadAfterStop = true
        listLoadJob?.cancel()
        previewDialog?.dismiss()
        previewDialog = null
        deleteFlow.dismissActiveDialog()
        super.onStop()
    }

    override fun onDestroy() {
        listLoadJob?.cancel()
        listLoadJob = null
        previewDialog?.dismiss()
        previewDialog = null
        deleteFlow.dismissActiveDialog()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun loadTests(showProgress: Boolean = true) {
        if (!ownerCurrent()) return
        val hasItems = adapter.itemCount > 0
        if (showProgress && !hasItems) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }
        listLoadJob?.cancel()
        listLoadJob = lifecycleScope.launch {
            try {
                val tests = examRepo.getGeneratedTests()
                adapter.submit(tests)
                if (tests.isEmpty()) {
                    hasEmptyPlayed = true
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("No generated tests yet")
                    emptyView.showTextView()
                    actionBar.setSubtitle(null)
                } else {
                    hasEmptyPlayed = false
                    emptyView.visibility = View.GONE
                    listView.visibility = View.VISIBLE
                    actionBar.setSubtitle("${tests.size} ${if (tests.size == 1) "test" else "tests"}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (adapter.itemCount > 0) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Failed to load generated tests")
                    emptyView.showTextView()
                }
                AppBulletin.showError(
                    this@GeneratedTestsActivity,
                    "Failed to load generated tests: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun toggleTestStatus(test: GeneratedTest, isLive: Boolean) {
        if (!ownerCurrent()) return
        val newStatus = if (isLive) "live" else "paused"
        lifecycleScope.launch {
            try {
                if (!ownerCurrent()) return@launch
                examRepo.updateGeneratedTestStatus(test.id, newStatus)
                AppBulletin.showSuccess(
                    this@GeneratedTestsActivity,
                    "Test is now ${newStatus.uppercase()}"
                )
                loadTests(showProgress = false)
            } catch (e: Exception) {
                AppBulletin.showError(
                    this@GeneratedTestsActivity,
                    "Failed to update status: ${e.localizedMessage}"
                )
                loadTests(showProgress = false)
            }
        }
    }

    private fun previewTest(test: GeneratedTest) {
        if (!ownerCurrent()) return
        if (test.questions.isEmpty()) {
            lifecycleScope.launch {
                try {
                    if (!ownerCurrent()) return@launch
                    val detail = examRepo.getGeneratedTest(test.id)
                    if (detail == null || detail.questions.isEmpty()) {
                        AppBulletin.showError(
                            this@GeneratedTestsActivity,
                            "No valid question payload is available for preview"
                        )
                    } else {
                        previewTest(detail)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    AppBulletin.showError(
                        this@GeneratedTestsActivity,
                        "Could not load test preview. Refresh and retry."
                    )
                }
            }
            return
        }
        val formattedText = StringBuilder()
        test.questions.forEachIndexed { index, q ->
            formattedText.append("Q${index + 1}: ${q.questionText}\n")
            formattedText.append("A) ${q.optionA}\n")
            formattedText.append("B) ${q.optionB}\n")
            formattedText.append("C) ${q.optionC}\n")
            formattedText.append("D) ${q.optionD}\n")
            formattedText.append("Correct: ${q.correctAnswer}\n")
            if (q.explanation.isNotBlank()) {
                formattedText.append("Explanation: ${q.explanation}\n")
            }
            formattedText.append("\n--------------------\n\n")
        }

        previewDialog = AlertDialog.Builder(this)
            .setTitle("${test.examName} (${test.questionCount} Questions)")
            .setMessage(formattedText.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDelete(test: GeneratedTest) {
        if (!ownerCurrent()) return
        deleteFlow.confirm(test.id, test.displayTitle, exam = false) { loadTests(showProgress = false) }
    }
}
