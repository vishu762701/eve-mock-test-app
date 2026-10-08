package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.ui.common.EveBaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Every data source is protected by requireAdmin on the Worker. */
class SystemMonitorActivity : EveBaseActivity() {
    private lateinit var content: LinearLayout
    private lateinit var overview: LinearLayout
    private lateinit var health: LinearLayout
    private lateinit var operations: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var refresh: MaterialButton
    private lateinit var more: MaterialButton
    private var offset = 0
    private var nextOffset: Int? = null
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(getColor(R.color.eve_bg))
        }
        scroll.addView(content)
        setContentView(scroll)
        text(content, "System Health & Operations", 22f)
        button(content, "Back") { finish() }
        refresh = button(content, "Refresh / Retry") { offset = 0; load() }
        progress = ProgressBar(this).also { content.addView(it) }
        overview = section("Dashboard overview")
        health = section("System health")
        operations = section("Generation & submission monitor")
        more = button(content, "Next page") { nextOffset?.let { offset = it; load() } }
        load()
    }

    private fun section(title: String): LinearLayout {
        text(content, title, 19f)
        return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }.also { content.addView(it) }
    }

    private fun text(parent: LinearLayout, value: String, size: Float = 15f) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(getColor(R.color.eve_text))
            setPadding(0, 12, 0, 12)
        })
    }

    private fun button(parent: LinearLayout, value: String, action: () -> Unit): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = value
            setOnClickListener { action() }
        }.also { parent.addView(it) }

    private suspend fun checked(request: suspend () -> ApiResponse<JsonObject>): JsonObject {
        val response = request()
        check(response.success && response.data != null) { response.error ?: "No data available" }
        return response.data!!
    }

    private fun load() {
        if (loading) return
        loading = true
        nextOffset = null
        progress.visibility = View.VISIBLE
        refresh.isEnabled = false
        more.isEnabled = false
        operations.removeAllViews()
        overview.removeAllViews()
        health.removeAllViews()
        lifecycleScope.launch {
            try {
                populate(overview) {
                    val result = checked { ApiClient.api.systemOverview() }
                    val totals = result.getAsJsonObject("totals")
                    listOf("users" to "Registered profiles", "exams" to "Exams", "publishedTests" to "Published tests", "questions" to "Questions (bank + generated)", "attempts" to "Submitted attempts").forEach { (key, label) -> text(overview, "$label: ${totals.get(key).asLong}") }
                    text(overview, "Recent test activity")
                    val rows = result.getAsJsonArray("activity")
                    if (rows.size() == 0) text(overview, "No confirmed submissions yet")
                    rows.forEach { text(overview, "${it.asJsonObject.string("exam_name")} • ${time(it.asJsonObject, "timestamp")}") }
                }
                populate(health) {
                    val checks = checked { ApiClient.api.systemHealth() }
                    checks.entrySet().forEach { (name, value) ->
                        val check = value.asJsonObject
                        text(health, "$name: ${check.string("status")}\n${check.string("detail")}")
                    }
                }
                populate(operations) {
                    val result = checked { ApiClient.api.systemOperations(offset) }
                    nextOffset = result.get("nextOffset").takeUnless { it.isJsonNull }?.asInt
                    text(operations, "Generation jobs • page ${offset / 25 + 1}")
                    val jobs = result.getAsJsonArray("jobs")
                    if (jobs.size() == 0) text(operations, "No tracked jobs on this page. Tracking starts with this release.")
                    jobs.forEach { element ->
                        val job = element.asJsonObject
                        text(operations, "${job.string("exam_name")} • ${job.string("status")}\nRequested ${job.string("requested_count")}, generated ${job.string("generated_count")}, failed ${job.string("failed_count")}\nStarted ${time(job, "started_at")} • completed ${time(job, "completed_at")}\n${job.string("error_category")}\nJob ${job.string("id")}")
                        if (job.string("status") == "failed" && job.get("retryable").asInt == 1) {
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
                loading = false
                progress.visibility = View.GONE
                refresh.isEnabled = true
                more.isEnabled = nextOffset != null
            }
        }
    }

    private suspend fun populate(parent: LinearLayout, block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) {
            text(parent, "Unavailable: ${e.toUserFriendlyMessage()}. Use Refresh / Retry.")
        }
    }

    private fun confirmRetry(id: String) {
        if (loading) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Retry generation?")
            .setMessage("Uses the current exam syllabus and settings. A complete validated test will be created only if generation succeeds.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Retry") { _, _ ->
                if (loading) return@setPositiveButton
                loading = true
                refresh.isEnabled = false
                more.isEnabled = false
                progress.visibility = View.VISIBLE
                lifecycleScope.launch {
                    try { checked { ApiClient.api.retryGenerationJob(id) } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { MaterialAlertDialogBuilder(this@SystemMonitorActivity).setMessage(e.toUserFriendlyMessage()).setPositiveButton("OK", null).show() }
                    finally { loading = false; offset = 0; load() }
                }
            }.show()
    }

    private fun JsonObject.string(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    private fun time(row: JsonObject, key: String): String = row.get(key)?.takeUnless { it.isJsonNull }?.asLong?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "—"
}
