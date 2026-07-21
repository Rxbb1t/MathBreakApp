package com.ak.momapp.data

import com.ak.momapp.problem.ProblemTopic
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueueTest {

    private val today = 20_000L

    /** Asks repeatedly so the review chance can't hide a due item. */
    private fun anythingDue(raw: String?, day: Long = today, topics: Set<ProblemTopic> = ProblemTopic.ALL) =
        (1..200).firstNotNullOfOrNull {
            ReviewQueue.due(raw, day, topics, Random(it))
        }

    @Test
    fun `a queue survives the round trip`() {
        val items = listOf(
            ReviewItem(ProblemTopic.MONEY, "{name} had # euros#", 20_001, 0),
            ReviewItem(ProblemTopic.CORE, "# + # = ?", 20_004, 2),
        )
        assertEquals(items, ReviewQueue.decode(ReviewQueue.encode(items)))
    }

    /**
     * A shape is arbitrary problem text, and set problems print real
     * braces, colons and commas. Splitting the shape off by position
     * rather than by delimiter is what stops one of them corrupting the
     * whole queue.
     */
    @Test
    fun `a shape full of punctuation survives`() {
        val nasty = "A = {#, #, #}\nB = {#, #}\nHow many elements are in A : B?"
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.NUMBERS, nasty, today)
        assertEquals(listOf(nasty), ReviewQueue.decode(raw).map { it.shape })
    }

    @Test
    fun `garbage decodes to nothing rather than throwing`() {
        assertTrue(ReviewQueue.decode("nonsense").isEmpty())
        assertTrue(ReviewQueue.decode(null).isEmpty())
        assertTrue(ReviewQueue.decode("").isEmpty())
        assertTrue(ReviewQueue.decode("NOPE:1:0:shape").isEmpty())
    }

    @Test
    fun `a missed shape comes back tomorrow, not today`() {
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.WORD, "# apples#", today)
        assertNull("should not be due the same day", anythingDue(raw))
        assertNotNull(anythingDue(raw, today + 1))
    }

    @Test
    fun `missing the same shape twice does not make it more urgent`() {
        val once = ReviewQueue.recordMiss(null, ProblemTopic.WORD, "# apples#", today)
        val twice = ReviewQueue.recordMiss(once, ProblemTopic.WORD, "# apples#", today + 5)
        assertEquals(ReviewQueue.decode(once), ReviewQueue.decode(twice))
    }

    @Test
    fun `getting it right pushes it further out each time`() {
        var raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "# + #", today)
        var previousGap = 0L
        for (stage in ReviewQueue.INTERVALS.indices.drop(1)) {
            val due = ReviewQueue.decode(raw).single().dueDay
            raw = ReviewQueue.recordCorrect(raw, "# + #", due)
            val item = ReviewQueue.decode(raw).singleOrNull() ?: break
            val gap = item.dueDay - due
            assertTrue("gap $gap did not grow past $previousGap at stage $stage", gap > previousGap)
            previousGap = gap
        }
    }

    /**
     * Right across two weeks means it is simply known. Keeping it forever
     * would slowly fill every break with things she has already learned.
     */
    @Test
    fun `a shape answered right at the last stage is retired`() {
        var raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "# + #", today)
        repeat(ReviewQueue.INTERVALS.size) {
            val due = ReviewQueue.decode(raw).singleOrNull()?.dueDay ?: return@repeat
            raw = ReviewQueue.recordCorrect(raw, "# + #", due)
        }
        assertTrue("should have retired: $raw", ReviewQueue.decode(raw).isEmpty())
    }

    @Test
    fun `a correct answer on something not queued changes nothing`() {
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "# + #", today)
        assertEquals(raw, ReviewQueue.recordCorrect(raw, "# × #", today))
    }

    @Test
    fun `a shapeless problem is never queued`() {
        assertEquals("", ReviewQueue.recordMiss(null, ProblemTopic.TARGET, null, today))
    }

    /**
     * Bringing back a shape from a topic she has switched off would be
     * putting that topic back on for her.
     */
    @Test
    fun `a switched-off topic is never brought back`() {
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.GEOMETRY, "a ladder #", today)
        assertNull(anythingDue(raw, today + 5, ProblemTopic.ALL - ProblemTopic.GEOMETRY))
        assertNotNull(anythingDue(raw, today + 5, ProblemTopic.ALL))
    }

    @Test
    fun `the one waiting longest goes first`() {
        var raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "old", today)
        raw = ReviewQueue.recordMiss(raw, ProblemTopic.WORD, "new", today + 3)
        assertEquals("old", anythingDue(raw, today + 6)?.shape)
    }

    @Test
    fun `nothing due means the roll is left alone`() {
        assertNull(anythingDue(null))
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "later", today + 40)
        assertNull(anythingDue(raw))
    }

    /**
     * Review has to stay a seasoning rather than the meal. A break that
     * was mostly problems she once got wrong would be a bad afternoon,
     * whatever it was called.
     */
    @Test
    fun `review stays a minority of rolls`() {
        val raw = ReviewQueue.recordMiss(null, ProblemTopic.CORE, "# + #", today)
        val hits = (1..4_000).count {
            ReviewQueue.due(raw, today + 1, ProblemTopic.ALL, Random(it)) != null
        }
        val share = hits / 4_000.0
        assertTrue("review fired on $share of rolls", share < 0.35)
        assertTrue("review never fired", share > 0.05)
    }

    @Test
    fun `the queue does not grow without bound`() {
        var raw: String? = null
        repeat(ReviewQueue.MAX_ITEMS * 3) {
            raw = ReviewQueue.recordMiss(raw, ProblemTopic.CORE, "shape $it", today)
        }
        assertTrue(ReviewQueue.decode(raw).size <= ReviewQueue.MAX_ITEMS)
    }
}
