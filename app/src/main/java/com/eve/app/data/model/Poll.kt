package com.eve.app.data.model

data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val endsAt: Long = 0L,
    val active: Boolean = true,
    val createdBy: String = "",
    val voteCounts: Map<String, Long> = emptyMap()
) {
    val totalVotes: Long
        get() = voteCounts.values.sum()

    val isExpired: Boolean
        get() = endsAt > 0 && System.currentTimeMillis() > endsAt

    val isCurrentlyActive: Boolean
        get() = active && !isExpired

    fun getVotesForOption(index: Int): Long =
        voteCounts[index.toString()] ?: 0L

    fun getPercentageForOption(index: Int): Int {
        val total = totalVotes
        if (total == 0L) return 0
        val count = getVotesForOption(index)
        return ((count.toDouble() / total.toDouble()) * 100).toInt()
    }
}

data class PollVote(
    val optionIndex: Int = -1,
    val votedAt: Long = System.currentTimeMillis()
)
