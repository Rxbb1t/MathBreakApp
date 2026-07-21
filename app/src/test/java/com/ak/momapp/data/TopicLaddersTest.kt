package com.ak.momapp.data

import com.ak.momapp.problem.Difficulty
import com.ak.momapp.problem.Level
import com.ak.momapp.problem.LevelLadder
import com.ak.momapp.problem.Pace
import com.ak.momapp.problem.PaceEstimate
import com.ak.momapp.problem.ProblemTopic
import com.ak.momapp.problem.toLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-topic ladders decide what she is actually asked, so the rules
 * that keep them honest are worth nailing down: a topic follows the
 * overall level until it has earned its own, the Relaxed ceiling outranks
 * whatever it earns, and the name on the row can never disagree with the
 * level the generator deals at.
 */
class TopicLaddersTest {

    private val start = Difficulty.MEDIUM.toLevel()

    /** Files [count] answers against one topic and returns the raw string. */
    private fun record(
        raw: String?,
        topic: ProblemTopic,
        correct: Boolean,
        count: Int = 1,
        pace: Pace = Pace.STEADY,
        solveTimeMs: Long = 20_000,
        ceiling: Level = Level.CEILING,
    ): String {
        var current = raw
        repeat(count) {
            current = TopicLadders.record(
                raw = current,
                topic = topic,
                correct = correct,
                pace = pace,
                solveTimeMs = solveTimeMs,
                seedLevel = start,
                seedBand = Difficulty.MEDIUM,
                ceiling = ceiling,
            )
        }
        return current.orEmpty()
    }

    @Test
    fun `a ladder survives the round trip`() {
        val ladders = mapOf(
            ProblemTopic.CORE to TopicLadder(Level.of(54), Difficulty.MEDIUM, 9, PaceEstimate(23_000, 7)),
            ProblemTopic.LOGIC to TopicLadder(Level.of(20), Difficulty.EASY, 3),
        )
        assertEquals(ladders, TopicLadders.decode(TopicLadders.encode(ladders)))
    }

    @Test
    fun `garbage costs one topic, not all of them`() {
        val good = TopicLadders.encode(
            mapOf(ProblemTopic.MONEY to TopicLadder(Level.of(40), Difficulty.MEDIUM, 8)),
        )
        val decoded = TopicLadders.decode("NONSENSE:x:y,$good,TIME:999")
        assertEquals(setOf(ProblemTopic.MONEY), decoded.keys)
    }

    /**
     * Rows from the tier era carried streak counts that mean nothing on a
     * points ladder. A row that doesn't parse is dropped rather than
     * half-read: a level silently reappearing from a different scheme
     * would be worse than the ladder starting again.
     */
    @Test
    fun `rows from the tier-era encoding are discarded`() {
        assertTrue(TopicLadders.decode("CORE:MEDIUM:2:0:9").isEmpty())
        assertTrue(TopicLadders.decode("CORE:MEDIUM:2:0:9:0").isEmpty())
    }

    @Test
    fun `an empty or missing store decodes to nothing`() {
        assertTrue(TopicLadders.decode(null).isEmpty())
        assertTrue(TopicLadders.decode("").isEmpty())
    }

    @Test
    fun `a level outside the scale is rejected rather than clamped`() {
        assertNull(TopicLadders.decode("CORE:400:MEDIUM:9:0:0")[ProblemTopic.CORE])
    }

    // ── The evidence wait ────────────────────────────────────────────────

    @Test
    fun `a topic follows the overall level until it has evidence`() {
        val ladders = TopicLadders.decode(record(null, ProblemTopic.GEOMETRY, correct = false, count = 3))
        // Its own ladder has moved, but nobody is listening yet.
        assertTrue(ladders.getValue(ProblemTopic.GEOMETRY).level < start)
        assertEquals(start, TopicLadders.levelFor(ladders, ProblemTopic.GEOMETRY, start))
    }

    @Test
    fun `once there is evidence the topic speaks for itself`() {
        val raw = record(null, ProblemTopic.GEOMETRY, correct = false, count = TopicLadders.EVIDENCE_NEEDED)
        assertTrue(TopicLadders.levelFor(TopicLadders.decode(raw), ProblemTopic.GEOMETRY, start) < start)
    }

    @Test
    fun `the wait works upward too`() {
        val climbing = record(null, ProblemTopic.MONEY, correct = true, count = 3, pace = Pace.FAST)
        assertEquals(start, TopicLadders.levelFor(TopicLadders.decode(climbing), ProblemTopic.MONEY, start))

        val settled = record(
            null, ProblemTopic.MONEY, correct = true,
            count = TopicLadders.EVIDENCE_NEEDED, pace = Pace.FAST,
        )
        assertTrue(TopicLadders.levelFor(TopicLadders.decode(settled), ProblemTopic.MONEY, start) > start)
    }

    @Test
    fun `topics move independently`() {
        var raw = record(
            null, ProblemTopic.MONEY, correct = true,
            count = TopicLadders.EVIDENCE_NEEDED, pace = Pace.FAST,
        )
        raw = record(raw, ProblemTopic.GEOMETRY, correct = false, count = TopicLadders.EVIDENCE_NEEDED)
        val ladders = TopicLadders.decode(raw)
        val money = TopicLadders.levelFor(ladders, ProblemTopic.MONEY, start)
        val geometry = TopicLadders.levelFor(ladders, ProblemTopic.GEOMETRY, start)
        assertTrue("money $money should sit above geometry $geometry", money > geometry)
    }

    @Test
    fun `an unseen topic is dealt at the overall level`() {
        assertEquals(start, TopicLadders.levelFor(emptyMap(), ProblemTopic.WORD, start))
    }

    @Test
    fun `a fresh ladder is seeded from the overall level, not the floor`() {
        val ladder = TopicLadders.decode(record(null, ProblemTopic.TIME, correct = true))
            .getValue(ProblemTopic.TIME)
        assertTrue("seeded at ${ladder.level}", ladder.level > Level.of(Level.EASY_TOP))
    }

    // ── The Relaxed cap ──────────────────────────────────────────────────

    @Test
    fun `the cap outranks a topic that has climbed past it`() {
        val ceiling = Level.of(Level.MEDIUM_TOP)
        val ladders = TopicLadders.decode(
            record(
                null, ProblemTopic.CORE, correct = true,
                count = TopicLadders.EVIDENCE_NEEDED * 3, pace = Pace.FAST,
            ),
        )
        assertTrue(TopicLadders.levelFor(ladders, ProblemTopic.CORE, start) > ceiling)
        assertEquals(ceiling, TopicLadders.levelFor(ladders, ProblemTopic.CORE, start, ceiling))
    }

    /**
     * The cap has to bite when it is applied LATE as well: picking the
     * Relaxed preset after climbing into Hard should bring the problems
     * back down, not leave her where she was.
     */
    @Test
    fun `a cap applied after the climb still applies`() {
        val raw = record(
            null, ProblemTopic.CORE, correct = true,
            count = TopicLadders.EVIDENCE_NEEDED * 3, pace = Pace.FAST,
        )
        val capped = TopicLadders.levelFor(
            TopicLadders.decode(raw), ProblemTopic.CORE, start, Level.of(Level.MEDIUM_TOP),
        )
        assertEquals(Difficulty.MEDIUM, capped.band)
    }

    // ── The name on the Exercises row ────────────────────────────────────

    @Test
    fun `the row names the level the topic is actually dealt at`() {
        val ladders = TopicLadders.decode(
            record(
                null, ProblemTopic.CORE, correct = true,
                count = TopicLadders.EVIDENCE_NEEDED * 4, pace = Pace.FAST,
            ),
        )
        val level = TopicLadders.levelFor(ladders, ProblemTopic.CORE, start)
        val band = TopicLadders.bandFor(ladders, ProblemTopic.CORE, start, Difficulty.MEDIUM)
        assertEquals(level.band, band)
    }

    @Test
    fun `a topic without evidence shows the overall name`() {
        val ladders = TopicLadders.decode(record(null, ProblemTopic.WORD, correct = false))
        assertEquals(
            Difficulty.MEDIUM,
            TopicLadders.bandFor(ladders, ProblemTopic.WORD, start, Difficulty.MEDIUM),
        )
    }

    /** The stored name has to travel with the level, or stickiness cannot work. */
    @Test
    fun `the shown name is remembered alongside the level`() {
        val ladder = TopicLadders.decode(
            record(
                null, ProblemTopic.CORE, correct = true,
                count = TopicLadders.EVIDENCE_NEEDED * 4, pace = Pace.FAST,
            ),
        ).getValue(ProblemTopic.CORE)
        assertEquals(LevelLadder.shownBand(ladder.level, ladder.shownBand), ladder.shownBand)
    }

    // ── Pace ─────────────────────────────────────────────────────────────

    @Test
    fun `only correct answers teach the app her pace`() {
        val raw = record(null, ProblemTopic.CORE, correct = false, count = 4, solveTimeMs = 30_000)
        assertEquals(0, TopicLadders.decode(raw).getValue(ProblemTopic.CORE).pace.samples)
    }

    @Test
    fun `her usual pace is learned per topic`() {
        var raw = record(null, ProblemTopic.CORE, correct = true, count = 6, solveTimeMs = 8_000)
        raw = record(raw, ProblemTopic.GEOMETRY, correct = true, count = 6, solveTimeMs = 90_000)
        val ladders = TopicLadders.decode(raw)
        // The same 30 seconds is quick for geometry and slow for the core.
        assertEquals(Pace.SLOW, TopicLadders.paceOf(ladders, ProblemTopic.CORE, 30_000))
        assertEquals(Pace.FAST, TopicLadders.paceOf(ladders, ProblemTopic.GEOMETRY, 30_000))
    }

    @Test
    fun `a topic with no history has no opinion about pace`() {
        assertEquals(Pace.STEADY, TopicLadders.paceOf(emptyMap(), ProblemTopic.MONEY, 1_000))
    }

    /**
     * The gap that made speed effectively worthless: a break draws from ten
     * topics, so waiting for each to gather its own five timed answers meant
     * fifty problems before quickness counted anywhere. Someone racing
     * through a first sitting was scored as merely steady the whole way.
     */
    @Test
    fun `her overall pace stands in until a topic has its own`() {
        val overall = PaceEstimate(30_000, 12)
        assertEquals(
            Pace.FAST,
            TopicLadders.paceOf(emptyMap(), ProblemTopic.MONEY, 8_000, overall),
        )
    }

    @Test
    fun `a topic's own pace outranks the overall one once it has enough`() {
        // Slow topic, quick person: 30 s is fast for THIS topic even though
        // it is slow for her in general.
        val raw = record(
            null, ProblemTopic.GEOMETRY, correct = true,
            count = PaceEstimate.EVIDENCE_NEEDED, solveTimeMs = 90_000,
        )
        val overall = PaceEstimate(10_000, 40)
        assertEquals(
            Pace.FAST,
            TopicLadders.paceOf(TopicLadders.decode(raw), ProblemTopic.GEOMETRY, 30_000, overall),
        )
    }

    @Test
    fun `a half-learned topic still defers to the overall pace`() {
        val raw = record(
            null, ProblemTopic.CORE, correct = true,
            count = PaceEstimate.EVIDENCE_NEEDED - 2, solveTimeMs = 90_000,
        )
        val overall = PaceEstimate(20_000, 30)
        // Judged against her general pace, not against two stray samples.
        assertEquals(
            Pace.FAST,
            TopicLadders.paceOf(TopicLadders.decode(raw), ProblemTopic.CORE, 9_000, overall),
        )
    }

    @Test
    fun `a fast answer climbs a topic quicker than a slow one`() {
        val fast = record(null, ProblemTopic.CORE, correct = true, count = 6, pace = Pace.FAST)
        val slow = record(null, ProblemTopic.CORE, correct = true, count = 6, pace = Pace.SLOW)
        assertTrue(
            TopicLadders.decode(fast).getValue(ProblemTopic.CORE).level >
                TopicLadders.decode(slow).getValue(ProblemTopic.CORE).level,
        )
    }

    @Test
    fun `misses walk a topic back down`() {
        val raw = record(null, ProblemTopic.PUZZLE, correct = false, count = 8)
        assertTrue(TopicLadders.decode(raw).getValue(ProblemTopic.PUZZLE).level < start)
    }
}
