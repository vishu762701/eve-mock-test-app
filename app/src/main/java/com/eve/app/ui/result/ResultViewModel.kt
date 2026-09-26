package com.eve.app.ui.result

import androidx.lifecycle.ViewModel
import com.eve.app.data.model.AnswerItem

/**
 * ViewModel for [ResultActivity] that preserves the submitted [AnswerItem] list
 * across configuration changes (e.g. screen rotation, theme changes) without
 * re-reading from Binder / Intent extras.
 */
class ResultViewModel : ViewModel() {

    var allItems: List<AnswerItem> = emptyList()
        private set

    fun initAnswers(answers: List<AnswerItem>) {
        if (allItems.isEmpty() && answers.isNotEmpty()) {
            allItems = answers
        }
    }
}
