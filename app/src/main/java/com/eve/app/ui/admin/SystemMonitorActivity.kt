package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import com.eve.app.data.repository.AdminRepository
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.ui.common.EveBaseActivity
import com.google.android.material.button.MaterialButton
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Every data source is protected by requireAdmin on the Worker. */
class SystemMonitorActivity : EveBaseActivity() {
    private lateinit var binding: com.eve.app.databinding.ActivitySystemMonitorBinding
    private lateinit var content: LinearLayout
    private lateinit var overview: LinearLayout
    private lateinit var health: LinearLayout
    private lateinit var operations: LinearLayout
    private lateinit var progress: com.eve.app.designsystem.TelegramRadialProgressView
    private lateinit var refresh: MaterialButton
    private lateinit var more: MaterialButton
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
    private var retryConfirmationDialog: TelegramNativeAlertDialog? = null
    private var retryErrorDialog: TelegramNativeAlertDialog? = null

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
        if (granted == null || !AdminPermissionSnapshot.allows(granted, initiatingEmail, "view_analytics_logs")) throw SecurityException("Generation retry permission is no longer available")
    }

    override fun onStart() {
        super.onStart()
        if (adminReady && !ownerCurrent()) { finish(); return }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized && !retryPending) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != initiatingUid || live.email != initiatingEmail) {
                finish()
                return
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.scrollMonitor.visibility = View.GONE
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "view_analytics_logs"
            ) { permissions ->
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                binding.progressBar.visibility = View.GONE
                binding.scrollMonitor.visibility = View.VISIBLE
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
            more.isEnabled = false
            progress.visibility = View.GONE
        }
        retryConfirmationDialog?.dismiss()
        retryErrorDialog?.dismiss()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.eve.app.databinding.ActivitySystemMonitorBinding.inflate(layoutInflater)
        content = binding.layoutMonitorContent
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollMonitor.visibility = View.GONE

        val initiatingUser = FirebaseAuth.getInstance().currentUser
        initiatingUid = initiatingUser?.uid.orEmpty()
        initiatingEmail = initiatingUser?.email.orEmpty()
        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "view_analytics_logs") { permissions ->
            adminReady = true
            if (!ownerCurrent()) return@requireAdmin
            binding.progressBar.visibility = View.GONE
            binding.scrollMonitor.visibility = View.VISIBLE
            canRetryGeneration = permissions.isOwner || "manage_generated_tests" in permissions.rights
            binding.btnBack.setOnClickListener { finish() }
            refresh = button(content, "Refresh / Retry") { offset = 0; load() }
            progress = com.eve.app.designsystem.TelegramRadialProgressView(this).also {
                val size = resources.getDimensionPixelSize(R.dimen.design_ring_size)
                content.addView(it, LinearLayout.LayoutParams(size, size))
            }
            overview = section("Dashboard overview")
            health = section("System health")
            operations = section("Generation & submission monitor")
            more = button(content, "Next page") { nextOffset?.let { offset = it; load() } }
            load()
        }
    }

    private fun section(title: String): LinearLayout {
        text(content, title, com.eve.app.designsystem.DesignType.Role.HEADING)
        val card = com.eve.app.designsystem.DesignCardView(this)
        content.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = resources.getDimensionPixelSize(R.dimen.design_card_gap)
        })
        return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }.also { card.addView(it) }
    }

    private fun text(parent: LinearLayout, value: String, role: com.eve.app.designsystem.DesignType.Role = com.eve.app.designsystem.DesignType.Role.BODY) {
        parent.addView(TextView(this).apply {
            text = value
            com.eve.app.designsystem.DesignType.apply(this, role)
            setTextColor(getColor(R.color.text_primary))
            minimumHeight = resources.getDimensionPixelSize(R.dimen.design_row_min)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.design_row_gap)
            }
        })
    }

    private fun button(parent: LinearLayout, value: String, action: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = value
            setOnClickListener { action() }
        }.also { parent.addView(it) }

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
        progress.visibility = View.VISIBLE
        refresh.isEnabled = false
        more.isEnabled = false
        operations.removeAllViews()
        overview.removeAllViews()
        health.removeAllViews()
        readJob = lifecycleScope.launch {
            try {
                populate(overview, generation) {
                    val result = checked(generation) { ApiClient.api.systemOverview() }
                    val totals = result.getAsJsonObject("totals")
                    listOf("users" to "Registered profiles", "exams" to "Exams", "publishedTests" to "Published tests", "questions" to "Questions (bank + generated)", "attempts" to "Submitted attempts").forEach { (key, label) -> text(overview, "$label: ${totals.get(key).asLong}") }
                    text(overview, "Recent test activity")
                    val rows = result.getAsJsonArray("activity")
                    if (rows.size() == 0) text(overview, "No confirmed submissions yet")
                    rows.forEach { text(overview, "${it.asJsonObject.string("exam_name")} • ${time(it.asJsonObject, "timestamp")}") }
                }
                populate(health, generation) {
                    val checks = checked(generation) { ApiClient.api.systemHealth() }
                    checks.entrySet().forEach { (name, value) ->
                        val check = value.asJsonObject
                        text(health, "$name: ${check.string("status")}\n${check.string("detail")}")
                    }
                }
                populate(operations, generation) {
                    val result = checked(generation) { ApiClient.api.systemOperations(pageOffset) }
                    nextOffset = result.get("nextOffset").takeUnless { it.isJsonNull }?.asInt
                    text(operations, "Generation jobs • page ${pageOffset / 25 + 1}")
                    val jobs = result.getAsJsonArray("jobs")
                    if (jobs.size() == 0) text(operations, "No tracked jobs on this page. Tracking starts with this release.")
                    jobs.forEach { element ->
                        val job = element.asJsonObject
                        text(operations, "${job.string("exam_name")} • ${job.string("status")}\nRequested ${job.string("requested_count")}, generated ${job.string("generated_count")}, failed ${job.string("failed_count")}\nStarted ${time(job, "started_at")} • completed ${time(job, "completed_at")}\n${job.string("error_category")}\nJob ${job.string("id")}")
                        if (canRetryGeneration && job.string("id") !in confirmedRetries && job.string("status") == "failed" && job.get("retryable").asInt == 1) {
                            button(operations, "Retry ${job.string("exam_name")}") { confirmRetry(job.string("id")) }
                        }
                    }
                    text(operations, "Confirmed submissions")
                    val submissions = result.getAsJsonArray("submissions")
                    if (submissions.size() == 0) text(operations, "No submissions on this page")
                    submissions.forEach { element ->
                        val row = element.asJsonObject
                        text(operations, "${row.string("exam_name")} • ${time(row, "timestamp")}\n${if (row.get("counted").asInt == 1) "Counted" else "Practice or outside timing window"} • ${row.string("id")}")
                    }
                    text(operations, "Recorded failures (30 day retention)")
                    val failures = result.getAsJsonArray("failures")
                    if (failures.size() == 0) text(operations, "No recorded failures on this page. Device network failures cannot be observed by the backend.")
                    failures.forEach { element ->
                        val row = element.asJsonObject
                        text(operations, "${row.string("operation")} • ${time(row, "timestamp")}\n${row.string("category")} • HTTP ${row.string("status")} • ${if (row.get("retryable").asInt == 1) "Retryable by the original client" else "Requires correction"}\nRequest ${row.string("correlation_id")}")
                    }
                }
            } finally {
                if (readCurrent(generation)) {
                    loading = false
                    progress.visibility = View.GONE
                    refresh.isEnabled = true
                    more.isEnabled = nextOffset != null
                }
            }
        }
    }

    private suspend fun populate(parent: LinearLayout, generation: Long, block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
            if (readCurrent(generation)) text(parent, "Unavailable: ${e.toUserFriendlyMessage()}. Use Refresh / Retry.")
        }
    }

    private fun confirmRetry(id: String) {
        // Telegram AlertDialog.java:325-328: never present a stale Activity window;
        // keep only one owner-controlled Retry confirmation visible.
        if (!canRetryGeneration || loading || !canPublish() || retryPending || id.isBlank() ||
            id in confirmedRetries || retryConfirmationDialog?.isShowing == true) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Retry generation?")
            .setMessage("Uses the current exam syllabus and settings. A complete validated test will be created only if generation succeeds.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Retry") { _, _ ->
                if (loading || retryPending || !canPublish() || id in confirmedRetries) return@setPositiveButton
                val epoch = stopEpoch
                retryPending = true // Acquire before launching the suspended permission read.
                ++loadGeneration
                readJob?.cancel()
                refresh.isEnabled = false
                more.isEnabled = false
                progress.visibility = View.VISIBLE
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
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        if (epoch == stopEpoch && canPublish()) {
                            retryErrorDialog?.dismiss()
                            retryErrorDialog = TelegramNativeAlertDialog.Builder(this@SystemMonitorActivity)
                                .setMessage(e.toUserFriendlyMessage())
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    } finally {
                        retryPending = false
                        if (epoch == stopEpoch && canPublish()) {
                            progress.visibility = View.GONE
                            refresh.isEnabled = true
                            more.isEnabled = nextOffset != null
                            // Do not disguise failed or malformed acknowledgments as a refreshed success.
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
    private fun time(row: JsonObject, key: String): String = row.get(key)?.takeUnless { it.isJsonNull }?.asLong?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "—"
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
