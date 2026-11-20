/*
 * Derived from Telegram Android PollCreateActivity.java, Copyright Nikolai Kudashov.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.poll

import android.animation.LayoutTransition
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.repository.PollRepository
import com.eve.app.databinding.ItemPollOptionEditableBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.IconBackgroundColors

/**
 * Poll & Quiz Creation screen powered by real ported Telegram UI components:
 * ActionBar (with Done checkmark), HeaderCell, EditTextBoldCursor, TextCheckCell,
 * TextCell, AlertDialog (duration picker), ShadowSectionCell, and TextInfoPrivacyCell.
 */
class PollCreateActivity : EveBaseActivity() {

    companion object {
        private const val MENU_DONE = 1
        const val MAX_QUESTION_LENGTH = 255
        const val MAX_ANSWER_LENGTH = 100
        const val MAX_SOLUTION_LENGTH = 200
        const val MAX_OPTIONS = 10
        const val MIN_OPTIONS = 2
    }

    private val pollRepository = PollRepository()
    private val optionRows = mutableListOf<ItemPollOptionEditableBinding>()
    private var isQuizMode = false
    private var isAnonymous = true
    private var isMultipleAnswers = false
    private var selectedCorrectIndex = -1
    private var calculatedEndsAt = 0L
    private var selectedDurationLabel = "No Expiry"

    private lateinit var actionBar: ActionBar
    private lateinit var etQuestion: EditTextBoldCursor
    private lateinit var tvQuestionCounter: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var btnAddOption: TextCell
    private lateinit var cellAnonymous: TextCheckCell
    private lateinit var cellMultipleAnswers: TextCheckCell
    private lateinit var cellQuizMode: TextCheckCell
    private lateinit var layoutQuizSection: LinearLayout
    private lateinit var etExplanation: EditTextBoldCursor
    private lateinit var tvExplanationCounter: TextView
    private lateinit var cellDuration: TextCell

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("New Poll")
            createMenu().addItem(MENU_DONE, R.drawable.ic_ab_done)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_DONE -> submitPoll()
                    }
                }
            })
        }
        root.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(list, matchWrap())
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 1. Poll Question
        list.addView(headerCell("POLL QUESTION"), matchWrap())
        val qWrap = inputWrap()
        etQuestion = styledEditText("Ask a question", singleLine = false)
        tvQuestionCounter = counterText(MAX_QUESTION_LENGTH.toString())
        qWrap.addView(etQuestion, matchWrap())
        qWrap.addView(tvQuestionCounter, matchWrap())
        list.addView(qWrap, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // 2. Poll Options
        list.addView(headerCell("POLL OPTIONS"), matchWrap())
        optionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            layoutTransition = LayoutTransition()
        }
        list.addView(optionsContainer, matchWrap())

        btnAddOption = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndColorfulIcon("Add an Option", R.drawable.msg_info, IconBackgroundColors.BLUE.bottom, false)
            setOnClickListener {
                if (optionRows.size < MAX_OPTIONS) {
                    addOptionRow("Option ${optionRows.size + 1}")
                }
            }
        }
        list.addView(btnAddOption, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // 3. Poll Settings
        list.addView(headerCell("POLL SETTINGS"), matchWrap())
        cellAnonymous = TextCheckCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Anonymous Voting", isAnonymous, true)
            setOnClickListener {
                isAnonymous = !isAnonymous
                setChecked(isAnonymous)
            }
        }
        list.addView(cellAnonymous, matchWrap())

        cellMultipleAnswers = TextCheckCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Multiple Answers", isMultipleAnswers, true)
            setOnClickListener {
                if (!isQuizMode) {
                    isMultipleAnswers = !isMultipleAnswers
                    setChecked(isMultipleAnswers)
                }
            }
        }
        list.addView(cellMultipleAnswers, matchWrap())

        cellQuizMode = TextCheckCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Quiz Mode", isQuizMode, false)
            setOnClickListener { setQuizModeEnabled(!isQuizMode) }
        }
        list.addView(cellQuizMode, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // 4. Quiz Explanation (hidden unless quiz mode)
        layoutQuizSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        layoutQuizSection.addView(headerCell("QUIZ EXPLANATION"), matchWrap())
        val expWrap = inputWrap()
        etExplanation = styledEditText("Add an explanation (optional)", singleLine = false)
        tvExplanationCounter = counterText(MAX_SOLUTION_LENGTH.toString())
        expWrap.addView(etExplanation, matchWrap())
        expWrap.addView(tvExplanationCounter, matchWrap())
        layoutQuizSection.addView(expWrap, matchWrap())
        layoutQuizSection.addView(TextInfoPrivacyCell(this).apply {
            setText("Users will see this explanation after choosing an option.")
        }, matchWrap())
        list.addView(layoutQuizSection, matchWrap())

        // 5. Poll Duration
        list.addView(headerCell("POLL DURATION"), matchWrap())
        cellDuration = TextCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndValue("Duration", selectedDurationLabel, false)
            setOnClickListener { showDurationPicker() }
        }
        list.addView(cellDuration, matchWrap())

        setupCounters()
        addOptionRow("Option 1")
        addOptionRow("Option 2")
    }

    private fun setupCounters() {
        etQuestion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val left = MAX_QUESTION_LENGTH - (s?.length ?: 0)
                tvQuestionCounter.text = left.toString()
                tvQuestionCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etExplanation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val left = MAX_SOLUTION_LENGTH - (s?.length ?: 0)
                tvExplanationCounter.text = left.toString()
                tvExplanationCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
                )
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun addOptionRow(hintText: String) {
        val rowBinding = ItemPollOptionEditableBinding.inflate(layoutInflater, optionsContainer, false)
        rowBinding.etOptionText.hint = hintText

        rowBinding.etOptionText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val left = MAX_ANSWER_LENGTH - (s?.length ?: 0)
                rowBinding.tvOptionCounter.text = left.toString()
                rowBinding.tvOptionCounter.setTextColor(
                    if (left < 0) 0xFFFF3B30.toInt()
                    else Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
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
                optionsContainer.removeView(rowBinding.root)
                optionRows.removeAt(idx)
                if (selectedCorrectIndex == idx) selectedCorrectIndex = -1
                else if (selectedCorrectIndex > idx) selectedCorrectIndex--
                updateOptionDeleteVisibility()
                updateOptionAddButtonVisibility()
            } else {
                AppBulletin.showError(this, "A poll must have at least 2 options")
            }
        }

        optionsContainer.addView(rowBinding.root)
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
        btnAddOption.visibility = if (optionRows.size < MAX_OPTIONS) View.VISIBLE else View.GONE
    }

    private fun selectCorrectOption(index: Int) {
        selectedCorrectIndex = index
        for (i in optionRows.indices) {
            optionRows[i].radioQuizCorrect.isChecked = (i == index)
        }
    }

    private fun setQuizModeEnabled(enabled: Boolean) {
        isQuizMode = enabled
        cellQuizMode.setChecked(enabled)
        actionBar.setTitle(if (isQuizMode) "New Quiz" else "New Poll")

        if (isQuizMode) {
            isMultipleAnswers = false
            cellMultipleAnswers.setChecked(false)
            cellMultipleAnswers.alpha = 0.5f
            cellMultipleAnswers.isEnabled = false
            layoutQuizSection.visibility = View.VISIBLE
            for (row in optionRows) row.radioQuizCorrect.visibility = View.VISIBLE
        } else {
            cellMultipleAnswers.alpha = 1f
            cellMultipleAnswers.isEnabled = true
            layoutQuizSection.visibility = View.GONE
            for (row in optionRows) row.radioQuizCorrect.visibility = View.GONE
            selectedCorrectIndex = -1
        }
    }

    private fun showDurationPicker() {
        val labels = arrayOf<CharSequence>("No Expiry", "1 Day", "3 Days", "7 Days")
        AlertDialog.Builder(this)
            .setTitle("Poll Duration")
            .setItems(labels) { _, position ->
                val now = System.currentTimeMillis()
                selectedDurationLabel = labels[position].toString()
                calculatedEndsAt = when (position) {
                    1 -> now + 1L * 24 * 60 * 60 * 1000
                    2 -> now + 3L * 24 * 60 * 60 * 1000
                    3 -> now + 7L * 24 * 60 * 60 * 1000
                    else -> 0L
                }
                cellDuration.setTextAndValue("Duration", selectedDurationLabel, false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitPoll() {
        val question = etQuestion.text.toString().trim()
        if (question.isBlank()) {
            AppBulletin.showError(this, "Please enter a question")
            etQuestion.requestFocus()
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

        val explanation = etExplanation.text.toString().trim()
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

    private fun headerCell(title: String) = HeaderCell(this).apply {
        setText(title)
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
    }

    private fun inputWrap() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        val padH = AndroidUtilities.dp(21f)
        val padV = AndroidUtilities.dp(10f)
        setPadding(padH, padV, padH, padV)
    }

    private fun styledEditText(hintText: String, singleLine: Boolean) = EditTextBoldCursor(this).apply {
        hint = hintText
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
        setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setCursorSize(AndroidUtilities.dp(20f))
        setCursorWidth(1.5f)
        isSingleLine = singleLine
        if (!singleLine) minLines = 2
    }

    private fun counterText(initial: String) = TextView(this).apply {
        text = initial
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
        setPadding(0, AndroidUtilities.dp(4f), 0, 0)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}
