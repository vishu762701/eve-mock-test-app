package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityManageExamsBinding
import kotlinx.coroutines.launch

class ManageExamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageExamsBinding
    private val examRepo = ExamRepository()
    private var examList: List<Exam> = emptyList()
    private var selectedExam: Exam? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageExamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedExam = examList.getOrNull(position)
                displaySelectedExam()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnSave.setOnClickListener {
            saveExamSettings()
        }

        loadExams()
    }

    private fun loadExams() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                examList = examRepo.getExams()
                binding.progressBar.visibility = View.GONE
                val titles = examList.map { "${it.examName} (${it.categoryOrOther})" }
                val adapter = ArrayAdapter(
                    this@ManageExamsActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    titles
                )
                binding.spExam.adapter = adapter
                if (examList.isNotEmpty()) {
                    selectedExam = examList.first()
                    displaySelectedExam()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ManageExamsActivity,
                    "Failed to load exams: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displaySelectedExam() {
        val exam = selectedExam ?: return
        binding.etQuestionCount.setText(if (exam.questionCount > 0) exam.questionCount.toString() else "20")
        binding.switchAutoGen.isChecked = exam.autoGenerationEnabled
        binding.etSyllabus.setText(exam.syllabus)
        binding.etCustomNotes.setText(exam.customPromptNotes)
    }

    private fun saveExamSettings() {
        val exam = selectedExam ?: return
        val count = binding.etQuestionCount.text?.toString()?.toIntOrNull() ?: 20
        val autoGen = binding.switchAutoGen.isChecked
        val syllabus = binding.etSyllabus.text?.toString()?.trim().orEmpty()
        val customNotes = binding.etCustomNotes.text?.toString()?.trim().orEmpty()

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                examRepo.updateExamAiSettings(
                    examId = exam.id,
                    syllabus = syllabus,
                    questionCount = count,
                    customPromptNotes = customNotes,
                    autoGenerationEnabled = autoGen
                )

                // Update local memory
                selectedExam = exam.copy(
                    syllabus = syllabus,
                    questionCount = count,
                    customPromptNotes = customNotes,
                    autoGenerationEnabled = autoGen
                )

                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ManageExamsActivity,
                    "Settings saved for ${exam.examName}!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ManageExamsActivity,
                    "Failed to save: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
