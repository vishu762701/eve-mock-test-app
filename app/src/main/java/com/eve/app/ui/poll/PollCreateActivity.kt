/*
 * Derived from Telegram Android PollCreateActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.poll

import android.animation.LayoutTransition
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.PollRepository
import com.eve.app.databinding.ActivityPollCreateBinding
import com.eve.app.databinding.ItemPollOptionEditableBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PollCreateActivity : EveBaseActivity() {

    companion object {
        const val MAX_QUESTION_LENGTH = 255
        const val MAX_ANSWER_LENGTH = 100
        const val MAX_SOLUTION_LENGTH = 200
        const val MAX_OPTIONS = 10
        const val MIN_OPTIONS = 2
    }

    private lateinit var binding: ActivityPollCreateBinding
    private val pollRepository = PollRepository()

    private val optionRows = mutableListOf<ItemPollOptionEditableBinding>()
    private var isQuizMode = false
    private var isAnonymous = true
    private var isMultipleAnswers = false
    private var selectedCorrectIndex = -1
    private var calculatedEndsAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.layoutOptionsContainer.layoutTransition = LayoutTransition()

        setupActionBar()
        setupHeaders()
        setupQuestionInput()
        setupOptions()
        setupSettings()
        setupQuizExplanation()
        setupDurationSpinner()
    }

    private fun setupActionBar() {
        binding.actionBar.setTitle(if (isQuizMode) "New Quiz" else "New Poll")
        binding.actionBar.onBackPressed = { finish() }
        binding.actionBar.showDoneAction { submitPoll() }
        binding.actionBar.bindSystemBack(this)
    }

    private fun setupHeaders() {
        binding.headerQuestion.setText("POLL QUESTION")
        binding.headerOptions.setText("POLL OPTIONS")
        binding.headerSettings.setText("POLL SETTINGS")
        binding.headerExplanation.setText("QUIZ EXPLANATION")
        binding.infoPrivacyExplanation.setText("Users will see this explanation after choosing an option.")
        binding.headerDuration.setText("POLL DURATION")
    }

    private fun setupQuestionInput() {
        binding.etQuestion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                val left = MAX_QUESTION_LENGTH - len
                binding.tvQuestionCounter.text = left.toString()
                binding.tvQuestionCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(this@PollCreateActivity).textSecondary
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupOptions() {
        addOptionRow("Option 1")
        addOptionRow("Option 2")

        binding.btnAddOption.setOnClickListener {
            if (optionRows.size < MAX_OPTIONS) {
                addOptionRow("Option ${optionRows.size + 1}")
            }
        }
    }

    private fun addOptionRow(hintText: String) {
        val rowBinding = ItemPollOptionEditableBinding.inflate(
            layoutInflater,
            binding.layoutOptionsContainer,
            false
        )
        rowBinding.etOptionText.hint = hintText

        rowBinding.etOptionText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                val left = MAX_ANSWER_LENGTH - len
                rowBinding.tvOptionCounter.text = left.toString()
                rowBinding.tvOptionCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(this@PollCreateActivity).textSecondary
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        rowBinding.radioQuizCorrect.visibility = if (isQuizMode) View.VISIBLE else View.GONE
        rowBinding.radioQuizCorrect.setOnClickListener {
            val idx = optionRows.indexOf(rowBinding)
            if (idx >= 0) selectCorrectOption(idx)
        }

        rowBinding.btnDeleteOption.setOnClickListener {
            if (optionRows.size > MIN_OPTIONS) {
                val idx = optionRows.indexOf(rowBinding)
                binding.layoutOptionsContainer.removeView(rowBinding.root)
                optionRows.removeAt(idx)
                if (selectedCorrectIndex == idx) {
                    selectedCorrectIndex = -1
                } else if (selectedCorrectIndex > idx) {
                    selectedCorrectIndex--
                }
                updateOptionDeleteVisibility()
                updateOptionAddButtonVisibility()
            } else {
                AppBulletin.showError(this, "A poll must have at least 2 options")
            }
        }

        binding.layoutOptionsContainer.addView(rowBinding.root)
        optionRows.add(rowBinding)
        updateOptionDeleteVisibility()
        updateOptionAddButtonVisibility()
    }

    private fun updateOptionDeleteVisibility() {
        val canDelete = optionRows.size > MIN_OPTIONS
        for (row in optionRows) {
            row.btnDeleteOption.visibility = if (canDelete) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateOptionAddButtonVisibility() {
        binding.btnAddOption.visibility = if (optionRows.size < MAX_OPTIONS) View.VISIBLE else View.GONE
    }

    private fun selectCorrectOption(index: Int) {
        selectedCorrectIndex = index
        for (i in optionRows.indices) {
            optionRows[i].radioQuizCorrect.isChecked = (i == index)
        }
    }

    private fun setupSettings() {
        binding.cellAnonymous.setTextAndCheck("Anonymous Voting", isAnonymous, true)
        binding.cellAnonymous.setOnClickListener {
            isAnonymous = !isAnonymous
            binding.cellAnonymous.setChecked(isAnonymous)
        }

        binding.cellMultipleAnswers.setTextAndCheck("Multiple Answers", isMultipleAnswers, true)
        binding.cellMultipleAnswers.setOnClickListener {
            if (!isQuizMode) {
                isMultipleAnswers = !isMultipleAnswers
                binding.cellMultipleAnswers.setChecked(isMultipleAnswers)
            }
        }

        binding.cellQuizMode.setTextAndCheck("Quiz Mode", isQuizMode, false)
        binding.cellQuizMode.setOnClickListener {
            setQuizModeEnabled(!isQuizMode)
        }
    }

    private fun setQuizModeEnabled(enabled: Boolean) {
        isQuizMode = enabled
        binding.cellQuizMode.setChecked(enabled)
        binding.actionBar.setTitle(if (isQuizMode) "New Quiz" else "New Poll")

        if (isQuizMode) {
            isMultipleAnswers = false
            binding.cellMultipleAnswers.setChecked(false)
            binding.cellMultipleAnswers.alpha = 0.5f
            binding.cellMultipleAnswers.isEnabled = false
            binding.layoutQuizSection.visibility = View.VISIBLE
            for (row in optionRows) {
                row.radioQuizCorrect.visibility = View.VISIBLE
            }
        } else {
            binding.cellMultipleAnswers.alpha = 1f
            binding.cellMultipleAnswers.isEnabled = true
            binding.layoutQuizSection.visibility = View.GONE
            for (row in optionRows) {
                row.radioQuizCorrect.visibility = View.GONE
            }
            selectedCorrectIndex = -1
        }
    }

    private fun setupQuizExplanation() {
        binding.etExplanation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                val left = MAX_SOLUTION_LENGTH - len
                binding.tvExplanationCounter.text = left.toString()
                binding.tvExplanationCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(this@PollCreateActivity).textSecondary
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupDurationSpinner() {
        val durations = listOf("No Expiry", "1 Day", "3 Days", "7 Days")
        binding.spDuration.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            durations
        )
        binding.spDuration.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val now = System.currentTimeMillis()
                calculatedEndsAt = when (position) {
                    0 -> 0L
                    1 -> now + 1L * 24 * 60 * 60 * 1000
                    2 -> now + 3L * 24 * 60 * 60 * 1000
                    3 -> now + 7L * 24 * 60 * 60 * 1000
                    else -> 0L
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun submitPoll() {
        val question = binding.etQuestion.text.toString().trim()
        if (question.isBlank()) {
            AppBulletin.showError(this, "Please enter a question")
            binding.etQuestion.requestFocus()
            return
        }
        if (question.length > MAX_QUESTION_LENGTH) {
            AppBulletin.showError(this, "Question exceeds maximum $MAX_QUESTION_LENGTH characters")
            return
        }

        val options = optionRows.map { it.etOptionText.text.toString().trim() }.filter { it.isNotBlank() }
        if (options.size < MIN_OPTIONS) {
            AppBulletin.showError(this, "Please provide at least 2 non-empty options")
            return
        }
        for (opt in options) {
            if (opt.length > MAX_ANSWER_LENGTH) {
                AppBulletin.showError(this, "Option exceeds maximum $MAX_ANSWER_LENGTH characters")
                return
            }
        }

        val explanation = binding.etExplanation.text.toString().trim()
        if (isQuizMode) {
            if (selectedCorrectIndex < 0 || selectedCorrectIndex >= options.size) {
                AppBulletin.showError(this, "Please choose the correct answer for the quiz")
                return
            }
            if (explanation.length > MAX_SOLUTION_LENGTH) {
                AppBulletin.showError(this, "Explanation exceeds maximum $MAX_SOLUTION_LENGTH characters")
                return
            }
        }

        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "admin"

        lifecycleScope.launch {
            try {
                pollRepository.createPoll(
                    question = question,
                    options = options,
                    endsAt = calculatedEndsAt,
                    active = true,
                    createdBy = userEmail,
                    quizMode = isQuizMode,
                    correctOptionIndex = selectedCorrectIndex,
                    explanation = explanation
                )
                AppBulletin.showSuccess(this@PollCreateActivity, if (isQuizMode) "Quiz created" else "Poll created")
                setResult(RESULT_OK)
                finish()
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                AppBulletin.showError(this@PollCreateActivity, "Failed to create poll: ${e.message}")
            }
        }
    }
}
