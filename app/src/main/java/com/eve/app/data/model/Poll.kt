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

    /**
     * Calculates percentages normalized to sum up to 100% when votes exist.
     * Adapted from Telegram Android ChatMessageCell.java:29518-29547 (normalizePollPercents).
     */
    fun getNormalizedPercentages(): List<Int> {
        val total = totalVotes
        if (total == 0L || options.isEmpty()) {
            return List(options.size) { 0 }
        }

        class OptionFraction(val index: Int, var percent: Int, val decimal: Double)

        var restPercent = 100
        var hasDifferent = false
        var previousPercent = 0

        val fractions = options.indices.map { index ->
            val count = getVotesForOption(index)
            val rawPercent = (count.toDouble() / total.toDouble()) * 100.0
            val intPercent = rawPercent.toInt()
            val decimal = rawPercent - intPercent

            if (previousPercent == 0) {
                previousPercent = intPercent
            } else if (intPercent != 0 && previousPercent != intPercent) {
                hasDifferent = true
            }
            restPercent -= intPercent
            OptionFraction(index, intPercent, decimal)
        }.toMutableList()

        if (hasDifferent && restPercent > 0 && fractions.isNotEmpty()) {
            val sorted = fractions.sortedWith { o1, o2 ->
                when {
                    o1.decimal > o2.decimal -> -1
                    o1.decimal < o2.decimal -> 1
                    o1.percent > o2.percent -> 1
                    o1.percent < o2.percent -> -1
                    else -> 0
                }
            }
            for (item in sorted) {
                if (restPercent <= 0) break
                if (item.percent > 0) {
                    item.percent += 1
                    restPercent--
                }
            }
        }

        return fractions.map { it.percent }
    }

    fun getPercentageForOption(index: Int): Int {
        val normalized = getNormalizedPercentages()
        return normalized.getOrElse(index) { 0 }
    }
}

data class PollVote(
    val optionIndex: Int = -1,
    val votedAt: Long = System.currentTimeMillis()
)
