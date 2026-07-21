package com.ak.momapp.data

import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.DifficultyTracker
import com.ak.momapp.problem.ProblemTopic

/**
 * One topic's own adaptive level, climbing and falling on the same rules
 * as the overall one but on its own evidence.
 *
 * The point is that being sharp with money and slow with angles is normal,
 * and a single level has to average the two: geometry stays punishing
 * while the shopping problems go stale. Separate ladders let each topic
 * settle where it belongs.
 */
data class TopicLadder(
    val level: Difficulty,
    val correctInARow: Int = 0,
    val missesInARow: Int = 0,
    /** First-try answers on this topic since the last fresh round. */
    val seen: Int = 0,
)

/**
 * DataStore encoding for the per-topic ladders, stored as one string:
 * "CORE:MEDIUM:2:0:9,LOGIC:EASY:0:1:3"
 * (topic:level:correctInARow:missesInARow:seen).
 *
 * Anything malformed is dropped on read, so a renamed topic or a
 * half-written value costs one topic's history rather than all of it.
 * That also covers the older six-field rows from when topics could be
 * pinned: they decode to nothing and the ladder simply starts again.
 */
object TopicLadders {

    /**
     * How many first-try answers a topic needs before its own ladder is
     * believed. Under this it follows the overall level.
     *
     * Without the wait, one unlucky geometry problem on the first day
     * would drop that topic on its own and she would see the app react to
     * a single answer. Six is about two breaks' worth of one topic.
     */
    const val EVIDENCE_NEEDED = 6

    fun encode(ladders: Map<ProblemTopic, TopicLadder>): String =
        ladders.entries
            .sortedBy { it.key.ordinal }
            .joinToString(",") { (topic, l) ->
                "${topic.name}:${l.level.name}:${l.correctInARow}:${l.missesInARow}:${l.seen}"
            }

    fun decode(raw: String?): Map<ProblemTopic, TopicLadder> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 5) return@mapNotNull null
            val topic = ProblemTopic.entries.firstOrNull { it.name == parts[0] }
                ?: return@mapNotNull null
            val level = Difficulty.entries.firstOrNull { it.name == parts[1] }
                ?: return@mapNotNull null
            val correct = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val misses = parts[3].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val seen = parts[4].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            topic to TopicLadder(level, correct, misses, seen)
        }.toMap()
    }

    /**
     * The level [topic] is actually dealt at: its own once there is
     * enough evidence, the overall one until then, and never above
     * [maxLevel] (the Relaxed preset's ceiling) whatever the ladder says.
     */
    fun levelFor(
        ladders: Map<ProblemTopic, TopicLadder>,
        topic: ProblemTopic,
        overall: Difficulty,
        maxLevel: Difficulty = Difficulty.HARD,
    ): Difficulty {
        val ladder = ladders[topic] ?: return minOf(overall, maxLevel)
        if (ladder.seen < EVIDENCE_NEEDED) return minOf(overall, maxLevel)
        return minOf(ladder.level, maxLevel)
    }

    /**
     * Files one first-try answer against [topic]'s ladder. A ladder that
     * doesn't exist yet starts at [seedLevel] (the overall level), so a
     * topic begins where she already is rather than back at Easy.
     */
    fun record(
        raw: String?,
        topic: ProblemTopic,
        correct: Boolean,
        seedLevel: Difficulty,
        maxLevel: Difficulty = Difficulty.HARD,
    ): String {
        val ladders = decode(raw).toMutableMap()
        val ladder = ladders[topic] ?: TopicLadder(level = seedLevel)
        val tracker = DifficultyTracker(
            start = ladder.level,
            correctInARow = ladder.correctInARow,
            missesInARow = ladder.missesInARow,
            maxLevel = maxLevel,
        )
        if (correct) tracker.recordCorrect() else tracker.recordIncorrect()
        ladders[topic] = TopicLadder(
            level = tracker.current,
            correctInARow = tracker.correctInARow,
            missesInARow = tracker.missesInARow,
            seen = ladder.seen + 1,
        )
        return encode(ladders)
    }
}
