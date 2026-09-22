package com.eve.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivityAdminBinding
import com.eve.app.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()

    private var exams: List<Exam> = emptyList()
    private var editingQuestion: Question? = null

    private val questionAdapter = QuestionManageAdapter(
        onEdit = { startEdit(it) },
        onDelete = { confirmDeleteQuestion(it) }
    )

    private val adminEmailAdapter = AdminEmailAdapter(
        onRemove = { confirmRemoveAdmin(it) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch {
            // Double check: hardcoded ya Firestore dono se admin check karo
            if (!adminRepo.isAdmin(email)) {
                finish()
                return@launch
            }
            setupUi()
        }
    }

    private fun setupUi() {
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.spCorrect.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, listOf("A", "B", "C", "D")
        )

        binding.spCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Constants.CATEGORIES
        )
        binding.spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.etOtherCategory.visibility =
                    if (Constants.CATEGORIES.getOrNull(position) == Constants.CATEGORY_OTHER) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = questionAdapter

        binding.rvAdmins.layoutManager = LinearLayoutManager(this)
        binding.rvAdmins.adapter = adminEmailAdapter

        binding.spExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val exam = exams.getOrNull(position)
                viewModel.loadQuestions(exam?.id ?: "")
                cancelEdit()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnAddExam.setOnClickListener { addExam() }
        binding.btnDeleteExam.setOnClickListener { confirmDeleteExam() }
        binding.btnUpload.setOnClickListener { submitQuestion() }
        binding.btnCancelEdit.setOnClickListener { cancelEdit() }
        binding.btnAddAdmin.setOnClickListener { addAdmin() }
        binding.btnToggleHindi.setOnClickListener { toggleHindiGroup() }

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
                        val selected = exams.getOrNull(binding.spExam.selectedItemPosition)
                        viewModel.loadQuestions(selected?.id ?: "")
                    }
                }
                launch {
                    viewModel.questions.collect { list ->
                        questionAdapter.submit(list)
                        binding.tvNoQuestions.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.admins.collect { list ->
                        adminEmailAdapter.submit(list)
                        binding.tvNoAdmins.visibility =
                            if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.busy.collect {
                        binding.progress.visibility = if (it) View.VISIBLE else View.GONE
                        binding.btnUpload.isEnabled = !it
                        binding.btnAddExam.isEnabled = !it
                        binding.btnDeleteExam.isEnabled = !it
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
        val selectedCategory = binding.spCategory.selectedItem as? String ?: Constants.CATEGORY_OTHER
        val category = if (selectedCategory == Constants.CATEGORY_OTHER) {
            binding.etOtherCategory.text.toString().trim()
        } else {
            selectedCategory
        }
        if (name.isEmpty() || minutes == null || minutes <= 0) {
            Toast.makeText(this, "Exam name aur valid minutes daalo", Toast.LENGTH_SHORT).show()
            return
        }
        if (category.isEmpty()) {
            Toast.makeText(this, "Category chuno ya naam type karo", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addExam(name, minutes, category) {
            binding.etExamName.text?.clear()
            binding.etExamMinutes.text?.clear()
            binding.spCategory.setSelection(0)
            binding.etOtherCategory.text?.clear()
            binding.etOtherCategory.visibility = View.GONE
        }
    }

    private fun confirmDeleteExam() {
        val exam = exams.getOrNull(binding.spExam.selectedItemPosition) ?: return
        AlertDialog.Builder(this)
            .setTitle("Exam delete karein?")
            .setMessage("'${exam.examName}' aur uske saare questions permanently delete ho jayenge.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExam(exam.id) { cancelEdit() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startEdit(q: Question) {
        editingQuestion = q
        binding.tvFormTitle.text = "3. Question edit karo"
        binding.etQuestion.setText(q.questionText)
        binding.etTopic.setText(q.topic)
        binding.etOptionA.setText(q.optionA)
        binding.etOptionB.setText(q.optionB)
        binding.etOptionC.setText(q.optionC)
        binding.etOptionD.setText(q.optionD)
        val idx = listOf("A", "B", "C", "D").indexOf(q.correctAnswer)
        if (idx >= 0) binding.spCorrect.setSelection(idx)
        binding.etExplanation.setText(q.explanation)
        binding.etQuestionHi.setText(q.questionTextHi)
        binding.etOptionAHi.setText(q.optionAHi)
        binding.etOptionBHi.setText(q.optionBHi)
        binding.etOptionCHi.setText(q.optionCHi)
        binding.etOptionDHi.setText(q.optionDHi)
        binding.etExplanationHi.setText(q.explanationHi)
        // Agar pehle se koi Hindi translation bhari hai to section khud khul jaye
        val hasHindi = listOf(
            q.questionTextHi, q.optionAHi, q.optionBHi, q.optionCHi, q.optionDHi, q.explanationHi
        ).any { it.isNotBlank() }
        if (hasHindi) showHindiGroup()
        binding.btnUpload.text = "Update Question"
        binding.btnCancelEdit.visibility = View.VISIBLE
    }

    private fun toggleHindiGroup() {
        if (binding.groupHindi.visibility == View.VISIBLE) hideHindiGroup() else showHindiGroup()
    }

    private fun showHindiGroup() {
        binding.groupHindi.visibility = View.VISIBLE
        binding.btnToggleHindi.text = "− Hindi translation hide karo"
    }

    private fun hideHindiGroup() {
        binding.groupHindi.visibility = View.GONE
        binding.btnToggleHindi.text = "+ Hindi translation add karo (optional)"
    }

    private fun cancelEdit() {
        editingQuestion = null
        binding.tvFormTitle.text = "3. Naya question upload karo"
        binding.etQuestion.text?.clear()
        binding.etTopic.text?.clear()
        binding.etOptionA.text?.clear()
        binding.etOptionB.text?.clear()
        binding.etOptionC.text?.clear()
        binding.etOptionD.text?.clear()
        binding.spCorrect.setSelection(0)
        binding.etExplanation.text?.clear()
        binding.etQuestionHi.text?.clear()
        binding.etOptionAHi.text?.clear()
        binding.etOptionBHi.text?.clear()
        binding.etOptionCHi.text?.clear()
        binding.etOptionDHi.text?.clear()
        binding.etExplanationHi.text?.clear()
        hideHindiGroup()
        binding.btnUpload.text = "Upload to Firestore"
        binding.btnCancelEdit.visibility = View.GONE
    }

    private fun confirmDeleteQuestion(q: Question) {
        AlertDialog.Builder(this)
            .setTitle("Question delete karein?")
            .setMessage(q.questionText)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuestion(q)
                if (editingQuestion?.id == q.id) cancelEdit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addAdmin() {
        val email = binding.etAdminEmail.text.toString().trim()
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Valid email daalo", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addAdmin(email) {
            binding.etAdminEmail.text?.clear()
        }
    }

    private fun confirmRemoveAdmin(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Admin remove karein?")
            .setMessage("$email ab admin nahi rahega.")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeAdmin(email) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitQuestion() {
        if (exams.isEmpty()) {
            Toast.makeText(this, "Pehle ek exam banao", Toast.LENGTH_SHORT).show()
            return
        }
        val exam = exams[binding.spExam.selectedItemPosition]
        val qText = binding.etQuestion.text.toString().trim()
        val topic = binding.etTopic.text.toString().trim()
        val a = binding.etOptionA.text.toString().trim()
        val b = binding.etOptionB.text.toString().trim()
        val c = binding.etOptionC.text.toString().trim()
        val d = binding.etOptionD.text.toString().trim()
        val correct = binding.spCorrect.selectedItem as String
        val explanation = binding.etExplanation.text.toString().trim()
        val qTextHi = binding.etQuestionHi.text.toString().trim()
        val aHi = binding.etOptionAHi.text.toString().trim()
        val bHi = binding.etOptionBHi.text.toString().trim()
        val cHi = binding.etOptionCHi.text.toString().trim()
        val dHi = binding.etOptionDHi.text.toString().trim()
        val explanationHi = binding.etExplanationHi.text.toString().trim()

        if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            Toast.makeText(this, "Saari fields bharo", Toast.LENGTH_SHORT).show()
            return
        }

        val editing = editingQuestion
        if (editing != null) {
            val updated = editing.copy(
                examId = exam.id,
                questionText = qText,
                topic = topic,
                optionA = a, optionB = b, optionC = c, optionD = d,
                correctAnswer = correct,
                explanation = explanation,
                questionTextHi = qTextHi,
                optionAHi = aHi, optionBHi = bHi, optionCHi = cHi, optionDHi = dHi,
                explanationHi = explanationHi
            )
            viewModel.updateQuestion(updated) { cancelEdit() }
        } else {
            val q = Question(
                examId = exam.id,
                questionText = qText,
                topic = topic,
                optionA = a, optionB = b, optionC = c, optionD = d,
                correctAnswer = correct,
                explanation = explanation,
                questionTextHi = qTextHi,
                optionAHi = aHi, optionBHi = bHi, optionCHi = cHi, optionDHi = dHi,
                explanationHi = explanationHi
            )
            viewModel.addQuestion(q) { cancelEdit() }
        }
    }
}
