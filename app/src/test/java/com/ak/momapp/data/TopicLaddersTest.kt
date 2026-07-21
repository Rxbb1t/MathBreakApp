package com.ak.momapp.data

import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.DifficultyTracker
import com.ak.momapp.problem.ProblemTopic
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The per-topic ladders decide what she is actually asked, so the rules
 * that keep them honest are worth nailing down: a topic follows the
 * overall level until it has earned its own, and the Relaxed ceiling
 * outranks whatever it earns.
 */
class TopicLaddersTest {

    private fun ladders(vararg pairs: Pair<ProblemTopic, TopicLadder>) = mapOf(*pairs)

    /** Answers one topic [times] over, starting from an empty store. */
    private fun drill(
        topic: ProblemTopic,
        times: Int,
        correct: Boolean,
        seedLevel: Difficulty = Difficulty.EASY,
        maxLevel: Difficulty = Difficulty.HARD,
    ): String {
        var raw: String? = null
        repeat(times) {
            raw = TopicLadders.record(raw, topic, correct, seedLevel, maxLevel)
        }
        return raw.orEmpty()
    }

    @Test
    fun `a round trip through the encoding keeps every field`() {
        val original = ladders(
            ProblemTopic.CORE to TopicLadder(Difficulty.MEDIUM, 2, 0, 9),
            ProblemTopic.LOGIC to TopicLadder(Difficulty.HARD, 0, 1, 21),
        )
        assertEquals(original, TopicLadders.decode(TopicLadders.encode(original)))
    }

    /** One bad entry must not cost the others their history. */
    @Test
    fun `garbage entries are dropped without taking the rest with them`() {
        val good = TopicLadders.encode(
            ladders(ProblemTopic.MONEY to TopicLadder(Difficulty.MEDIUM, 1, 0, 8)),
        )
        val decoded = TopicLadders.decode("NOPE:MEDIUM:0:0:1,$good,CORE:WHAT:0:0:1,broken")
        assertEquals(setOf(ProblemTopic.MONEY), decoded.keys)
        assertEquals(Difficulty.MEDIUM, decoded[ProblemTopic.MONEY]?.level)
    }

    /**
     * Rows written while topics could still be pinned carry a sixth
     * field. They must be dropped, not misread: a stale level silently
     * reappearing would be worse than starting the ladder again.
     */
    @Test
    fun `rows from the pinning version are discarded`() {
        assertEquals(emptyMap<ProblemTopic, TopicLadder>(), TopicLadders.decode("CORE:HARD:0:0:9:1"))
    }

    @Test
    fun `an untouched topic follows the overall level`() {
        assertEquals(
            Difficulty.MEDIUM,
            TopicLadders.levelFor(emptyMap(), ProblemTopic.GEOMETRY, Difficulty.MEDIUM),
        )
    }

    /**
     * The whole point of the wait: a topic that has climbed on its own
     * evidence must not be dragged back by the overall level, and one
     * that hasn't earned an opinion yet must not act on a lucky streak.
     */
    @Test
    fun `a topic follows the overall level until it has enough evidence`() {
        val young = ladders(
            ProblemTopic.MONEY to TopicLadder(
                level = Difficulty.HARD,
                seen = TopicLadders.EVIDENCE_NEEDED - 1,
            ),
        )
        assertEquals(
            "a ladder this young should not be trusted yet",
            Difficulty.EASY,
            TopicLadders.levelFor(young, ProblemTopic.MONEY, Difficulty.EASY),
        )

        val grown = ladders(
            ProblemTopic.MONEY to TopicLadder(
                level = Difficulty.HARD,
                seen = TopicLadders.EVIDENCE_NEEDED,
            ),
        )
        assertEquals(
            "a ladder with evidence should outrank the overall level",
            Difficulty.HARD,
            TopicLadders.levelFor(grown, ProblemTopic.MONEY, Difficulty.EASY),
        )
    }

    @Test
    fun `a run of correct answers lifts one topic and leaves the others alone`() {
        val raw = drill(
            ProblemTopic.MONEY,
            times = DifficultyTracker.CORRECT_STREAK_TO_LEVEL_UP * 2,
            correct = true,
        )
        val decoded = TopicLadders.decode(raw)
        assertEquals(setOf(ProblemTopic.MONEY), decoded.keys)
        assertEquals(Difficulty.HARD, decoded[ProblemTopic.MONEY]?.level)
        // And the untouched topic still follows the overall level.
        assertEquals(
            Difficulty.EASY,
            TopicLadders.levelFor(decoded, ProblemTopic.GEOMETRY, Difficulty.EASY),
        )
    }

    /** Relaxed caps the climb, per topic, exactly as it caps the overall one. */
    @Test
    fun `the Relaxed ceiling outranks a topic's own climb`() {
        val raw = drill(
            ProblemTopic.CORE,
            times = DifficultyTracker.CORRECT_STREAK_TO_LEVEL_UP * 3,
            correct = true,
            maxLevel = Difficulty.MEDIUM,
        )
        val decoded = TopicLadders.decode(raw)
        assertEquals(Difficulty.MEDIUM, decoded[ProblemTopic.CORE]?.level)
        assertEquals(
            Difficulty.MEDIUM,
            TopicLadders.levelFor(decoded, ProblemTopic.CORE, Difficulty.HARD, Difficulty.MEDIUM),
        )
    }

    /**
     * A ceiling that arrives AFTER the climb (she switches to Relaxed)
     * still has to bite, even though the stored level is already above it.
     */
    @Test
    fun `a ceiling applied later still holds a topic down`() {
        val grown = ladders(
            ProblemTopic.LOGIC to TopicLadder(Difficulty.HARD, seen = 30),
        )
        assertEquals(
            Difficulty.MEDIUM,
            TopicLadders.levelFor(grown, ProblemTopic.LOGIC, Difficulty.HARD, Difficulty.MEDIUM),
        )
    }

    /** A new topic starts where she already is, not back at the bottom. */
    @Test
    fun `a fresh ladder is seeded from the overall level`() {
        val raw = TopicLadders.record(null, ProblemTopic.NUMBERS, correct = true, Difficulty.HARD)
        assertEquals(Difficulty.HARD, TopicLadders.decode(raw).values.single().level)
    }

    @Test
    fun `misses walk a topic back down`() {
        val raw = drill(
            ProblemTopic.GEOMETRY,
            times = DifficultyTracker.MISS_STREAK_TO_LEVEL_DOWN,
            correct = false,
            seedLevel = Difficulty.HARD,
        )
        assertEquals(Difficulty.MEDIUM, TopicLadders.decode(raw)[ProblemTopic.GEOMETRY]?.level)
    }
}
