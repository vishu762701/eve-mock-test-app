package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.databinding.ActivityAdminBinding
import com.eve.app.util.isAdminEmail
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private var exams: List<Exam> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Double check: sirf admin hi is screen par reh sakta hai
        if (!isAdminEmail(FirebaseAuth.getInstance().currentUser?.email)) {
            finish()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spCorrect.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("A", "B", "C", "D")
        )

        binding.btnAddExam.setOnClickListener { addExam() }
        binding.btnUpload.setOnClickListener { uploadQuestion() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exams.collect { list ->
                        exams = list
                        binding.spExam.adapter = ArrayAdapter(
                            this@AdminActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            list.map { it.examName }
                        )
                    }
                }
                launch {
                    viewModel.busy.collect {
                        binding.progress.visibility = if (it) View.VISIBLE else View.GONE
                        binding.btnUpload.isEnabled = !it
                        binding.btnAddExam.isEnabled = !it
                    }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@AdminActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.consumeMessage()
                        }
                    }
                }
            }
        }
    }

    private fun addExam() {
        val name = binding.etExamName.text.toString().trim()
        val minutes = binding.etExamMinutes.text.toString().trim().toIntOrNull()
        if (name.isEmpty() || minutes == null || minutes <= 0) {
            Toast.makeText(this, "Exam name aur valid minutes daalo", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addExam(name, minutes) {
            binding.etExamName.text?.clear()
            binding.etExamMinutes.text?.clear()
        }
    }

    private fun uploadQuestion() {
        if (exams.isEmpty()) {
            Toast.makeText(this, "Pehle ek exam banao", Toast.LENGTH_SHORT).show()
            return
        }
        val exam = exams[binding.spExam.selectedItemPosition]
        val qText = binding.etQuestion.text.toString().trim()
        val a = binding.etOptionA.text.toString().trim()
        val b = binding.etOptionB.text.toString().trim()
        val c = binding.etOptionC.text.toString().trim()
        val d = binding.etOptionD.text.toString().trim()
        val correct = binding.spCorrect.selectedItem as String

        if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            Toast.makeText(this, "Saari fields bharo", Toast.LENGTH_SHORT).show()
            return
        }

        val q = Question(
            examId = exam.id,
            questionText = qText,
            optionA = a, optionB = b, optionC = c, optionD = d,
            correctAnswer = correct
        )
        viewModel.addQuestion(q) {
            binding.etQuestion.text?.clear()
            binding.etOptionA.text?.clear()
            binding.etOptionB.text?.clear()
            binding.etOptionC.text?.clear()
            binding.etOptionD.text?.clear()
            binding.etQuestion.requestFocus()
        }
    }
}
