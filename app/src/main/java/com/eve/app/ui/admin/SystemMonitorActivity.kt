package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.AdminRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Stories.recorder.ButtonWithCounterView
import java.text.DateFormat
import java.util.Date

/**
 * System Monitor screen powered by real ported Telegram UI components:
 * ActionBar (with refresh), HeaderCell, TextDetailSettingsCell, ShadowSectionCell,
 * EmptyTextProgressView, and ButtonWithCounterView (zero MaterialButton / DesignCardView).
 * Every data source is protected by requireAdmin on the Worker.
 */
class SystemMonitorActivity : EveBaseActivity() {

    companion object {
        private const val MENU_REFRESH = 1
    }

    private lateinit var rootView: LinearLayout
    private lateinit var scrollMonitor: ScrollView
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var content: LinearLayout
    private lateinit var overview: LinearLayout
    private lateinit var health: LinearLayout
    private lateinit var operations: LinearLayout
    private lateinit var refresh: ButtonWithCounterView
    private lateinit var more: ButtonWithCounterView

    private var offset = 0
    private var nextOffset: Int? = null
    private var loading = false
    private var canRetryGeneration = false
    private val adminRepo = AdminRepository()
    private var initiatingUid = ""
    private var initiatingEmail = ""
    private var adminReady = false
    private var loadGeneration = 0L
    private var stopEpoch = 0L
    private var readJob: Job? = null
    private var retryJob: Job? = null
    private var retryPending = false
    private var reloadAfterStop = true
    private var reauthorizeOnResume = false
    private val confirmedRetries = mutableSetOf<String>()
    private var retryConfirmationDialog: AlertDialog? = null
    private var retryErrorDialog: AlertDialog? = null

    private fun ownerCurrent(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser
        return adminReady && !isFinishing && !isDestroyed &&
            AdminIdentityGuard.allowed(true, initiatingUid, initiatingEmail, current?.uid, current?.email)
    }

    private fun canPublish(): Boolean =
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun readCurrent(generation: Long): Boolean =
        generation == loadGeneration && canPublish() && !retryPending

    private fun requireRetryCurrent(epoch: Long) {
        if (epoch != stopEpoch || !canPublish()) throw CancellationException("Monitor owner or lifecycle changed")
    }

    private suspend fun requireFreshRetryRight(epoch: Long) {
        requireRetryCurrent(epoch)
        val granted = AdminPermissionSnapshot.load(
            initiatingEmail, "manage_generated_tests",
            membership = { adminRepo.isAdminFresh(initiatingUid, initiatingEmail) },
            permissions = {
                requireRetryCurrent(epoch)
                adminRepo.getAdminMe()
            }
        )
        requireRetryCurrent(epoch)
        if (granted == null || !AdminPermissionSnapshot.allows(granted, initiatingEmail, "view_analytics_logs")) {
            throw SecurityException("Generation retry permission is no longer available")
        }
    }

    override fun onStart() {
        super.onStart()
        if (adminReady && !ownerCurrent()) { finish(); return }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::rootView.isInitialized && !retryPending) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != initiatingUid || live.email != initiatingEmail) {
                finish()
                return
            }
            if (!adminReady) {
                loadingView.visibility = View.VISIBLE
                scrollMonitor.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "view_analytics_logs"
            ) { permissions ->
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                loadingView.visibility = View.GONE
                scrollMonitor.visibility = View.VISIBLE
                canRetryGeneration = permissions.isOwner || "manage_generated_tests" in permissions.rights
                if (reloadAfterStop && ::refresh.isInitialized) {
                    reloadAfterStop = false
                    load()
                }
            }
        } else if (adminReady && reloadAfterStop && ::refresh.isInitialized && !retryPending) {
            load()
        }
    }

    override fun onStop() {
        ++stopEpoch
        reauthorizeOnResume = true
        reloadAfterStop = true
        ++loadGeneration
        readJob?.cancel()
        retryJob?.cancel()
        loading = false
        if (::refresh.isInitialized) {
            refresh.isEnabled = true
            refresh.setLoading(false)
            more.isEnabled = false
        }
        retryConfirmationDialog?.dismiss()
        retryErrorDialog?.dismiss()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Theme.syncFromEvePalette(this)

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        val actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("System Monitor")
            createMenu().addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_REFRESH -> if (adminReady && ::refresh.isInitialized) { offset = 0; load() }
                    }
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        scrollMonitor = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollMonitor.addView(content, matchWrap())
        contentFrame.addView(
            scrollMonitor,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        loadingView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            showProgress()
        }
        contentFrame.addView(
            loadingView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        val initiatingUser = FirebaseAuth.getInstance().currentUser
        initiatingUid = initiatingUser?.uid.orEmpty()
        initiatingEmail = initiatingUser?.email.orEmpty()
        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "view_analytics_logs") { permissions ->
            adminReady = true
            if (!ownerCurrent()) return@requireAdmin
            loadingView.visibility = View.GONE
            scrollMonitor.visibility = View.VISIBLE
            canRetryGeneration = permissions.isOwner || "manage_generated_tests" in permissions.rights

            refresh = button(content, "Refresh / Retry") { offset = 0; load() }
            overview = section("Dashboard overview")
            health = section("System health")
            operations = section("Generation & submission monitor")
            more = button(content, "Next page") { nextOffset?.let { offset = it; load() } }
            load()
        }
    }

    private fun section(title: String): LinearLayout {
        content.addView(HeaderCell(this).apply {
            setText(title)
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }
        content.addView(container, matchWrap())
        content.addView(ShadowSectionCell(this), matchWrap())
        return container
    }

    private fun addRow(parent: LinearLayout, title: String, subtitle: String = "") {
        parent.addView(TextDetailSettingsCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            setMultilineDetail(true)
            setTextAndValue(title, subtitle, true)
        }, matchWrap())
    }

    private fun button(parent: LinearLayout, value: String, action: () -> Unit): ButtonWithCounterView {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(10f)
            setPadding(padH, padV, padH, padV)
        }
        val btn = ButtonWithCounterView(this, null).apply {
            setRound()
            setText(value, false)
            setOnClickListener { action() }
        }
        wrap.addView(
            btn,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f))
        )
        parent.addView(wrap, matchWrap())
        return btn
    }

    private suspend fun checked(generation: Long, request: suspend () -> ApiResponse<JsonObject>): JsonObject {
        if (!readCurrent(generation)) throw CancellationException("Obsolete monitor request")
        val response = request()
        if (!readCurrent(generation)) throw CancellationException("Obsolete monitor response")
        check(response.success && response.data != null) { response.error ?: "No data available" }
        return response.data
    }

    private fun load() {
        if (!canPublish() || retryPending) return
        reloadAfterStop = false
        val generation = ++loadGeneration
        val pageOffset = offset
        readJob?.cancel()
        loading = true
        nextOffset = null
        refresh.isEnabled = false
        refresh.setLoading(true)
        more.isEnabled = false
        operations.removeAllViews()
        overview.removeAllViews()
        health.removeAllViews()
        readJob = lifecycleScope.launch {
            try {
                populate(overview, generation) {
                    val result = checked(generation) { ApiClient.api.systemOverview() }
                    val totals = result.getAsJsonObject("totals")
                    listOf(
                        "users" to "Registered profiles",
                        "exams" to "Exams",
                        "publishedTests" to "Published tests",
                        "questions" to "Questions (bank + generated)",
                        "attempts" to "Submitted attempts"
                    ).forEach { (key, label) ->
                        addRow(overview, totals.get(key).asLong.toString(), label)
                    }
                    val rows = result.getAsJsonArray("activity")
                    if (rows.size() == 0) {
                        addRow(overview, "Recent test activity", "No confirmed submissions yet")
                    } else {
                        rows.forEach {
                            addRow(overview, it.asJsonObject.string("exam_name"), time(it.asJsonObject, "timestamp"))
                        }
                    }
                }
                populate(health, generation) {
                    val checks = checked(generation) { ApiClient.api.systemHealth() }
                    checks.entrySet().forEach { (name, value) ->
                        val check = value.asJsonObject
                        addRow(health, "$name: ${check.string("status")}", check.string("detail"))
                    }
                }
                populate(operations, generation) {
                    val result = checked(generation) { ApiClient.api.systemOperations(pageOffset) }
                    nextOffset = result.get("nextOffset").takeUnless { it.isJsonNull }?.asInt
                    addRow(operations, "Generation jobs", "Page ${pageOffset / 25 + 1}")
                    val jobs = result.getAsJsonArray("jobs")
                    if (jobs.size() == 0) {
                        addRow(operations, "No tracked jobs on this page", "Tracking starts with this release.")
                    }
                    jobs.forEach { element ->
                        val job = element.asJsonObject
                        addRow(
                            operations,
                            "${job.string("exam_name")} • ${job.string("status")}",
                            "Requested ${job.string("requested_count")}, generated ${job.string("generated_count")}, failed ${job.string("failed_count")}\n" +
                                "Started ${time(job, "started_at")} • completed ${time(job, "completed_at")}\n" +
                                "${job.string("error_category")} • Job ${job.string("id")}"
                        )
                        if (canRetryGeneration && job.string("id") !in confirmedRetries && job.string("status") == "failed" && job.get("retryable").asInt == 1) {
                            button(operations, "Retry ${job.string("exam_name")}") { confirmRetry(job.string("id")) }
                        }
                    }
                    val submissions = result.getAsJsonArray("submissions")
                    if (submissions.size() == 0) {
                        addRow(operations, "Confirmed submissions", "No submissions on this page")
                    }
                    submissions.forEach { element ->
                        val row = element.asJsonObject
                        addRow(
                            operations,
                            "${row.string("exam_name")} • ${time(row, "timestamp")}",
                            "${if (row.get("counted").asInt == 1) "Counted" else "Practice or outside timing window"} • ${row.string("id")}"
                        )
                    }
                    val failures = result.getAsJsonArray("failures")
                    if (failures.size() == 0) {
                        addRow(operations, "Recorded failures (30d)", "No recorded failures on this page.")
                    }
                    failures.forEach { element ->
                        val row = element.asJsonObject
                        addRow(
                            operations,
                            "${row.string("operation")} • ${time(row, "timestamp")}",
                            "${row.string("category")} • HTTP ${row.string("status")} • ${if (row.get("retryable").asInt == 1) "Retryable" else "Requires correction"}\nRequest ${row.string("correlation_id")}"
                        )
                    }
                }
            } finally {
                if (readCurrent(generation)) {
                    loading = false
                    refresh.setLoading(false)
                    refresh.isEnabled = true
                    more.isEnabled = nextOffset != null
                }
            }
        }
    }

    private suspend fun populate(parent: LinearLayout, generation: Long, block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
            if (readCurrent(generation)) addRow(parent, "Unavailable", "${e.toUserFriendlyMessage()}. Use Refresh / Retry.")
        }
    }

    private fun confirmRetry(id: String) {
        if (!canRetryGeneration || loading || !canPublish() || retryPending || id.isBlank() ||
            id in confirmedRetries || retryConfirmationDialog?.isShowing == true
        ) return
        val dialog = AlertDialog.Builder(this)
            .setTitle("Retry generation?")
            .setMessage("Uses the current exam syllabus and settings. A complete validated test will be created only if generation succeeds.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Retry") { _, _ ->
                if (loading || retryPending || !canPublish() || id in confirmedRetries) return@setPositiveButton
                val epoch = stopEpoch
                retryPending = true
                ++loadGeneration
                readJob?.cancel()
                refresh.isEnabled = false
                refresh.setLoading(true)
                more.isEnabled = false
                retryJob = lifecycleScope.launch {
                    var confirmed = false
                    try {
                        requireFreshRetryRight(epoch)
                        requireRetryCurrent(epoch)
                        val response = ApiClient.api.retryGenerationJob(id)
                        requireRetryCurrent(epoch)
                        check(confirmedGenerationRetry(response)) {
                            response.error ?: "Generation retry was not confirmed by the server"
                        }
                        confirmedRetries.add(id)
                        confirmed = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (epoch == stopEpoch && canPublish()) {
                            retryErrorDialog?.dismiss()
                            retryErrorDialog = AlertDialog.Builder(this@SystemMonitorActivity)
                                .setMessage(e.toUserFriendlyMessage())
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    } finally {
                        retryPending = false
                        if (epoch == stopEpoch && canPublish()) {
                            refresh.setLoading(false)
                            refresh.isEnabled = true
                            more.isEnabled = nextOffset != null
                            if (confirmed) { offset = 0; load() }
                        }
                    }
                }
            }.show()
        retryConfirmationDialog = dialog
        dialog.setOnDismissListener {
            if (retryConfirmationDialog === dialog) retryConfirmationDialog = null
        }
    }

    override fun onDestroy() {
        retryConfirmationDialog?.dismiss()
        retryConfirmationDialog = null
        retryErrorDialog?.dismiss()
        retryErrorDialog = null
        readJob?.cancel()
        retryJob?.cancel()
        super.onDestroy()
    }

    private fun JsonObject.string(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    private fun time(row: JsonObject, key: String): String =
        row.get(key)?.takeUnless { it.isJsonNull }?.asLong?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "—"

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}

/** Matches the existing runGeneration response, never a client-created job or test. */
internal fun confirmedGenerationRetry(response: ApiResponse<JsonObject>): Boolean {
    if (!response.success) return false
    val data = response.data ?: return false
    fun identifier(name: String): Boolean = data.get(name)?.let {
        it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.isNotBlank()
    } == true
    val count = data.get("questionCount") ?: return false
    if (!count.isJsonPrimitive || !count.asJsonPrimitive.isNumber) return false
    val number = count.asDouble
    return identifier("testId") && identifier("jobId") && number.isFinite() &&
        number > 0 && number == kotlin.math.floor(number)
}
