package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedTestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { loadTests() }

        binding.rvGeneratedTests.layoutManager = LinearLayoutManager(this)
        binding.rvGeneratedTests.adapter = adapter

        loadTests()
    }

    private fun loadTests() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val tests = examRepo.getGeneratedTests()
                binding.progressBar.visibility = View.GONE
                adapter.submit(tests)
                binding.emptyGroup.visibility = if (tests.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@GeneratedTestsActivity,
                    "Failed to load generated tests: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun toggleTestStatus(test: GeneratedTest, isLive: Boolean) {
        val newStatus = if (isLive) "live" else "paused"
        lifecycleScope.launch {
            try {
                examRepo.updateGeneratedTestStatus(test.id, newStatus)
                Toast.makeText(
                    this@GeneratedTestsActivity,
                    "Test is now ${newStatus.uppercase()}",
                    Toast.LENGTH_SHORT
                ).show()
                loadTests()
            } catch (e: Exception) {
                Toast.makeText(
                    this@GeneratedTestsActivity,
                    "Failed to update status: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                loadTests()
            }
        }
    }

    private fun previewTest(test: GeneratedTest) {
        if (test.questions.isEmpty()) {
            Toast.makeText(this, "No questions found in this test payload.", Toast.LENGTH_SHORT).show()
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
                lifecycleScope.launch {
                    try {
                        examRepo.deleteGeneratedTest(test.id)
                        Toast.makeText(this@GeneratedTestsActivity, "Test deleted", Toast.LENGTH_SHORT).show()
                        loadTests()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@GeneratedTestsActivity,
                            "Failed to delete: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
