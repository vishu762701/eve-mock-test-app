package com.eve.app.ui.admin

import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import com.eve.app.R
import com.eve.app.util.AppBulletin
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityGeneratedTestsBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class GeneratedTestsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityGeneratedTestsBinding
    private val examRepo = ExamRepository()
    private var authorizedUid: String = ""
    private var authorizedEmail: String = ""
    private var adminReady: Boolean = false
    private var reauthorizeOnResume: Boolean = false
    private var reloadAfterStop: Boolean = false
    private var previewDialog: android.app.Dialog? = null

    private fun ownerCurrent(): Boolean {
        val current = FirebaseAuth.getInstance().currentUser ?: return false
        return current.uid == authorizedUid && current.email.orEmpty() == authorizedEmail
    }

    private val adapter = GeneratedTestAdapter(
        onStatusToggle = { test, isLive ->
            toggleTestStatus(test, isLive)
        },
        onPreview = { test ->
            previewTest(test)
        },
        onDelete = { test ->
            confirmDelete(test)
        },
        onSchedule = { test ->
            scheduleTest(test)
        }
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
                    com.eve.app.util.TestScheduleHelper.localOpeningTimeFromPicker(
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
                        // Reuse the canonical repository and verify the Worker row before success UI.
                        examRepo.scheduleGeneratedTest(test.id, targetMillis)
                        AppBulletin.showSuccess(
                            this@GeneratedTestsActivity,
                            "Test scheduled: ${com.eve.app.util.TestScheduleHelper.formatOpensAt(targetMillis)}"
                        )
                        loadTests()
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (e: Exception) {
                        AppBulletin.showError(this@GeneratedTestsActivity, "Schedule unconfirmed: ${e.localizedMessage}")
                    }
                }
            }
            timePicker.show(supportFragmentManager, "time_picker")
        }
        datePicker.show(supportFragmentManager, "date_picker")
    }

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityGeneratedTestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_generated_tests") {
            val current = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            authorizedUid = current.uid
            authorizedEmail = current.email.orEmpty()
            adminReady = true
            if (!ownerCurrent()) {
                finish()
                return@requireAdmin
            }

            binding.btnBack.setOnClickListener { finish() }
            binding.btnRefresh.setOnClickListener { loadTests() }
            binding.swipeRefresh.setOnRefreshListener { loadTests() }

            binding.rvGeneratedTests.layoutManager = LinearLayoutManager(this)
            binding.rvGeneratedTests.adapter = adapter

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
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != authorizedUid || live.email != authorizedEmail) {
                finish()
                return
            }
            com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_generated_tests") {
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                authorizedUid = live.uid
                authorizedEmail = live.email.orEmpty()
                if (reloadAfterStop) {
                    reloadAfterStop = false
                    loadTests()
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
        if (::binding.isInitialized) {
            com.eve.app.util.EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private var listLoadJob: kotlinx.coroutines.Job? = null

    private fun loadTests() {
        if (!ownerCurrent()) return
        if (!binding.swipeRefresh.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }
        listLoadJob?.cancel()
        listLoadJob = lifecycleScope.launch {
            try {
                val tests = examRepo.getGeneratedTests()
                binding.progressBar.visibility = View.GONE
                adapter.submit(tests)
                val empty = tests.isEmpty()
                binding.emptyGroup.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(
                        binding.lottieEmpty,
                        hasEmptyPlayed
                    )
                } else {
                    com.eve.app.util.EmptyStateAnimationHelper.stopEmptyState(binding.lottieEmpty)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(
                    this@GeneratedTestsActivity,
                    "Failed to load generated tests: ${e.localizedMessage}"
                )
            } finally {
                binding.swipeRefresh.isRefreshing = false
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
                loadTests()
            } catch (e: Exception) {
                AppBulletin.showError(
                    this@GeneratedTestsActivity,
                    "Failed to update status: ${e.localizedMessage}"
                )
                loadTests()
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
                        AppBulletin.showError(this@GeneratedTestsActivity, "No valid question payload is available for preview")
                    } else {
                        previewTest(detail)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { AppBulletin.showError(this@GeneratedTestsActivity, "Could not load test preview. Refresh and retry.") }
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

        previewDialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("${test.examName} (${test.questionCount} Questions)")
            .setMessage(formattedText.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDelete(test: GeneratedTest) {
        if (!ownerCurrent()) return
        deleteFlow.confirm(test.id, test.displayTitle, exam = false) { loadTests() }
    }
}
