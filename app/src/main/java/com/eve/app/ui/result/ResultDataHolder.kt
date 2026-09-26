package com.eve.app.ui.result

import com.eve.app.data.model.AnswerItem

/**
 * In-memory thread-safe holder for passing [AnswerItem] lists between activities
 * (e.g. from [com.eve.app.ui.test.TestActivity] or [com.eve.app.ui.history.HistoryActivity]
 * to [ResultActivity]) without exceeding Android's ~1MB Binder transaction limit
 * (which causes [android.os.TransactionTooLargeException]).
 */
object ResultDataHolder {

    private var answers: List<AnswerItem>? = null

    @Synchronized
    fun setAnswers(items: List<AnswerItem>) {
        answers = items
    }

    @Synchronized
    fun consumeAnswers(): List<AnswerItem> {
        val result = answers ?: emptyList()
        answers = null
        return result
    }

    @Synchronized
    fun clear() {
        answers = null
    }
}
