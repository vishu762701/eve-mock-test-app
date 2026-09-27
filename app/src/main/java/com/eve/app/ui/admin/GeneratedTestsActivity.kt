package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import com.eve.app.util.AppBulletin
import com.eve.app.util.AppUndoBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityGeneratedTestsBinding
import kotlinx.coroutines.launch

class GeneratedTestsActivity : AppCompatActivity() {

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
        }
    )

    private var hasEmptyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        binding = ActivityGeneratedTestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadTests() }

        binding.rvGeneratedTests.layoutManager = LinearLayoutManager(this)
        binding.rvGeneratedTests.adapter = adapter

        loadTests()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun loadTests() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
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
                    hasEmptyPlayed = false
                }
            } catch (e: Exception) {
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
            AppBulletin.showError(this, "No questions found in this test payload.")
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

    private fun confirmDelete(test: GeneratedTest) {
        AlertDialog.Builder(this)
            .setTitle("Delete Generated Test")
            .setMessage("Are you sure you want to permanently delete this test?")
            .setPositiveButton("Delete") { _, _ ->
                val originalList = adapter.getItems().toMutableList()
                val filtered = originalList.filter { it.id != test.id }
                adapter.submit(filtered)
                binding.emptyGroup.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

                AppUndoBar.show(
                    context = this@GeneratedTestsActivity,
                    message = "Test deleted",
                    timeLeftMs = AppUndoBar.TIME_IMPORTANT,
                    onUndo = {
                        adapter.submit(originalList)
                        binding.emptyGroup.visibility = if (originalList.isEmpty()) View.VISIBLE else View.GONE
                        AppBulletin.show(this@GeneratedTestsActivity, "Delete cancelled")
                    },
                    onExecuteDelete = {
                        lifecycleScope.launch {
                            try {
                                examRepo.deleteGeneratedTest(test.id)
                                loadTests()
                            } catch (e: Exception) {
                                adapter.submit(originalList)
                                binding.emptyGroup.visibility = if (originalList.isEmpty()) View.VISIBLE else View.GONE
                                AppBulletin.showError(
                                    this@GeneratedTestsActivity,
                                    "Failed to delete: ${e.localizedMessage}"
                                )
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
