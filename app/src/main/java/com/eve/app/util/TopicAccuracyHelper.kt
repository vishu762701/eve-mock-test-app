package com.eve.app.util

import com.eve.app.data.model.AnswerItem

data class TopicAccuracy(
    val topic: String,
    val correct: Int,
    val total: Int,
    val accuracy: Double
)

object TopicAccuracyHelper {

    /**
     * Aggregates answers by topic and sorts them weakest-first (lowest accuracy first).
     */
    fun aggregate(items: List<AnswerItem>): List<TopicAccuracy> {
        if (items.isEmpty()) return emptyList()
        val groups = items.groupBy { it.topic.ifBlank { "General" } }
        return groups.map { (topic, list) ->
            val total = list.size
            val correct = list.count { it.isCorrect }
            val accuracy = if (total > 0) (correct.toDouble() / total * 100.0) else 0.0
            TopicAccuracy(
                topic = topic,
                correct = correct,
                total = total,
                accuracy = Math.round(accuracy * 10.0) / 10.0
            )
        }.sortedWith(compareBy({ it.accuracy }, { it.total }))
    }
}
