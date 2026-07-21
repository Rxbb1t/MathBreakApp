package com.ak.momapp.data

import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.LevelLadder
import com.ak.momapp.problem.Outcome
import com.ak.momapp.problem.Pace
import com.ak.momapp.problem.PaceEstimate
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
    /** Where this topic sits on the fine 0–100 scale. */
    val level: Level,
    /**
     * The name last shown for this topic. Kept because the band is sticky
     * near a boundary ([LevelLadder.shownBand]) and stickiness needs to
     * remember what it was being sticky about.
     */
    val shownBand: Difficulty,
    /** First-try answers on this topic since the last fresh round. */
    val seen: Int = 0,
    /** How long she usually takes on this topic. */
    val pace: PaceEstimate = PaceEstimate(),
)

/**
 * DataStore encoding for the per-topic ladders, stored as one string:
 * "CORE:54:MEDIUM:9:23000:7,LOGIC:20:EASY:3:0:0"
 * (topic:points:shownBand:seen:typicalMs:paceSamples).
 *
 * Anything malformed is dropped on read, so a renamed topic or a
 * half-written value costs one topic's history rather than all of it.
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

    private const val FIELDS = 6

    fun encode(ladders: Map<ProblemTopic, TopicLadder>): String =
        ladders.entries
            .sortedBy { it.key.ordinal }
            .joinToString(",") { (topic, l) ->
                listOf(
                    topic.name,
                    l.level.points,
                    l.shownBand.name,
                    l.seen,
                    l.pace.typicalMs,
                    l.pace.samples,
                ).joinToString(":")
            }

    fun decode(raw: String?): Map<ProblemTopic, TopicLadder> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != FIELDS) return@mapNotNull null
            val topic = ProblemTopic.entries.firstOrNull { it.name == parts[0] }
                ?: return@mapNotNull null
            val points = parts[1].toIntOrNull()?.takeIf { it in Level.MIN..Level.MAX }
                ?: return@mapNotNull null
            val band = Difficulty.entries.firstOrNull { it.name == parts[2] }
                ?: return@mapNotNull null
            val seen = parts[3].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val typical = parts[4].toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val samples = parts[5].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            topic to TopicLadder(Level.of(points), band, seen, PaceEstimate(typical, samples))
        }.toMap()
    }

    /**
     * The level [topic] is actually dealt at: its own once there is
     * enough evidence, the overall one until then, and never above
     * [ceiling] (the Relaxed preset's cap) whatever the ladder says.
     */
    fun levelFor(
        ladders: Map<ProblemTopic, TopicLadder>,
        topic: ProblemTopic,
        overall: Level,
        ceiling: Level = Level.CEILING,
    ): Level {
        val ladder = ladders[topic] ?: return minOf(overall, ceiling)
        if (ladder.seen < EVIDENCE_NEEDED) return minOf(overall, ceiling)
        return minOf(ladder.level, ceiling)
    }

    /**
     * The NAME to show for [topic] on the Exercises screen. Follows the
     * same evidence rule as [levelFor] so the row and the problems she
     * gets can never disagree.
     */
    fun bandFor(
        ladders: Map<ProblemTopic, TopicLadder>,
        topic: ProblemTopic,
        overall: Level,
        overallBand: Difficulty,
        ceiling: Level = Level.CEILING,
    ): Difficulty {
        val ladder = ladders[topic]
        if (ladder == null || ladder.seen < EVIDENCE_NEEDED) {
            return LevelLadder.shownBand(minOf(overall, ceiling), overallBand)
        }
        return LevelLadder.shownBand(minOf(ladder.level, ceiling), ladder.shownBand)
    }

    /**
     * How quickly this answer arrived, judged against her usual pace on
     * this topic. Handed back rather than used here because the overall
     * ladder wants the same verdict, and classifying twice off two
     * different averages would let the two ladders disagree about whether
     * the very same answer was quick.
     */
    fun paceOf(
        ladders: Map<ProblemTopic, TopicLadder>,
        topic: ProblemTopic,
        solveTimeMs: Long,
        /**
         * Her pace across all topics, used until this one has an opinion of
         * its own.
         *
         * Without it, speed effectively never counted. A break draws from
         * ten topics, so waiting for five timed answers on EACH meant fifty
         * problems before quickness registered anywhere. Someone racing
         * through their first sitting would be scored as merely steady the
         * whole way, and the level would crawl for no reason they could
         * see. The topic estimate is still preferred the moment it exists,
         * because how long a geometry problem should take says very little
         * about a tap.
         */
        overall: PaceEstimate = PaceEstimate(),
    ): Pace {
        val own = ladders[topic]?.pace
        val estimate = if (own != null && own.samples >= PaceEstimate.EVIDENCE_NEEDED) own else overall
        return estimate.classify(solveTimeMs)
    }

    /**
     * Files one first-try answer against [topic]'s ladder. A ladder that
     * doesn't exist yet starts at [seedLevel] (the overall level), so a
     * topic begins where she already is rather than back at the floor.
     */
    fun record(
        raw: String?,
        topic: ProblemTopic,
        outcome: Outcome,
        pace: Pace,
        solveTimeMs: Long,
        seedLevel: Level,
        seedBand: Difficulty,
        effort: Double = 1.0,
        /**
         * Whether this counts as one of the topic's answers for the
         * evidence wait. False when the ladder is being moved a second time
         * for the same problem (a stumble votes, then losing it pays), so
         * one problem is never counted as two.
         */
        countsAsSeen: Boolean = true,
        ceiling: Level = Level.CEILING,
    ): String {
        val correct = outcome == Outcome.CORRECT
        val ladders = decode(raw).toMutableMap()
        val ladder = ladders[topic] ?: TopicLadder(level = seedLevel, shownBand = seedBand)
        val moved = LevelLadder.next(ladder.level, outcome, pace, effort, ceiling)
        ladders[topic] = TopicLadder(
            level = moved,
            shownBand = LevelLadder.shownBand(minOf(moved, ceiling), ladder.shownBand),
            seen = if (countsAsSeen) ladder.seen + 1 else ladder.seen,
            // Only correct answers teach the app her pace. A wrong answer's
            // time measures how long she was willing to stare at it, which
            // is a different thing entirely.
            pace = if (correct) ladder.pace.record(solveTimeMs) else ladder.pace,
        )
        return encode(ladders)
    }
}
