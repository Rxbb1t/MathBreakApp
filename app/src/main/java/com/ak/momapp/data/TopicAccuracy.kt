package com.ak.momapp.data

import com.ak.momapp.problem.ProblemTopic

/** One topic's first-try record: how many clean solves out of how many problems. */
data class TopicTally(val correct: Int, val seen: Int) {
    /** Rounded percent, or null before the first problem. */
    val percent: Int?
        get() = if (seen == 0) null else (correct * 100 + seen / 2) / seen
}

/**
 * DataStore encoding for the per-topic first-try tallies, stored as one
 * string: "CORE:3:4,LOGIC:1:2" (topic:correct:seen). Unknown topic names
 * are dropped on read, so a removed topic can't break decoding.
 */
object TopicAccuracy {

    fun encode(tallies: Map<ProblemTopic, TopicTally>): String =
        tallies.entries
            .sortedBy { it.key.ordinal }
            .joinToString(",") { (topic, tally) -> "${topic.name}:${tally.correct}:${tally.seen}" }

    fun decode(raw: String?): Map<ProblemTopic, TopicTally> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 3) return@mapNotNull null
            val topic = ProblemTopic.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val correct = parts[1].toIntOrNull() ?: return@mapNotNull null
            val seen = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (correct < 0 || seen < correct) return@mapNotNull null
            topic to TopicTally(correct, seen)
        }.toMap()
    }

    /** Returns the tallies with one more vote for [topic]. */
    fun record(raw: String?, topic: ProblemTopic, correct: Boolean): String {
        val tallies = decode(raw).toMutableMap()
        val tally = tallies[topic] ?: TopicTally(0, 0)
        tallies[topic] = TopicTally(
            correct = tally.correct + if (correct) 1 else 0,
            seen = tally.seen + 1,
        )
        return encode(tallies)
    }
}
