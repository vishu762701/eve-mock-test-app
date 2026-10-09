package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import com.eve.app.R
import com.eve.app.util.AppBulletin
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityGeneratedTestsBinding
import kotlinx.coroutines.launch

class GeneratedTestsActivity : EveBaseActivity() {

    private lateinit var binding: ActivityGeneratedTestsBinding
    private val examRepo = ExamRepository()

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

            timePicker.addOnPositiveButtonClickListener {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = selectedDateUtc
                    set(java.util.Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(java.util.Calendar.MINUTE, timePicker.minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val targetMillis = cal.timeInMillis

                lifecycleScope.launch {
                    try {
                        val res = com.eve.app.data.remote.ApiClient.api.scheduleGeneratedTest(test.id, mapOf("availableFrom" to targetMillis))
                        if (res.success) {
                            AppBulletin.showSuccess(
                                this@GeneratedTestsActivity,
                                "Test scheduled: ${com.eve.app.util.TestScheduleHelper.formatOpensAt(targetMillis)}"
                            )
                            loadTests()
                        } else {
                            AppBulletin.showError(this@GeneratedTestsActivity, "Failed to schedule test")
                        }
                    } catch (e: Exception) {
                        AppBulletin.showError(this@GeneratedTestsActivity, "Error scheduling test: ${e.localizedMessage}")
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
        window.setBackgroundDrawableResource(R.color.eve_bg)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityGeneratedTestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadTests() }

        binding.rvGeneratedTests.layoutManager = LinearLayoutManager(this)
        binding.rvGeneratedTests.adapter = adapter

        loadTests()
    }

    override fun onResume() {
        super.onResume()
        hasEmptyPlayed = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private var listLoadJob: kotlinx.coroutines.Job? = null

    private fun loadTests() {
        binding.progressBar.visibility = View.VISIBLE
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
            }
        }
    }

    private fun toggleTestStatus(test: GeneratedTest, isLive: Boolean) {
        val newStatus = if (isLive) "live" else "paused"
        lifecycleScope.launch {
            try {
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
        if (test.questions.isEmpty()) {
            lifecycleScope.launch {
                try {
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

        AlertDialog.Builder(this)
            .setTitle("${test.examName} (${test.questionCount} Questions)")
            .setMessage(formattedText.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private val deleteFlow by lazy { AdminDeleteFlow(this) }

    private fun confirmDelete(test: GeneratedTest) {
        deleteFlow.confirm(test.id, test.displayTitle, exam = false) { loadTests() }
    }
}
