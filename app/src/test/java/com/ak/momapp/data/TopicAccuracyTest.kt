package com.ak.momapp.data

import com.ak.momapp.problem.ProblemTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicAccuracyTest {

    @Test
    fun `tallies round-trip through encoding`() {
        val tallies = mapOf(
            ProblemTopic.CORE to TopicTally(3, 4),
            ProblemTopic.LOGIC to TopicTally(0, 2),
        )
        assertEquals(tallies, TopicAccuracy.decode(TopicAccuracy.encode(tallies)))
    }

    @Test
    fun `null or empty raw means no tallies yet`() {
        assertTrue(TopicAccuracy.decode(null).isEmpty())
        assertTrue(TopicAccuracy.decode("").isEmpty())
    }

    @Test
    fun `garbage entries are dropped, valid ones kept`() {
        val decoded = TopicAccuracy.decode("bogus,CORE:notanumber:4,LOGIC:1:2,TIME:9:4")
        // TIME claims more correct than seen — dropped too.
        assertEquals(mapOf(ProblemTopic.LOGIC to TopicTally(1, 2)), decoded)
    }

    @Test
    fun `recording votes builds a tally from nothing`() {
        var raw: String? = null
        raw = TopicAccuracy.record(raw, ProblemTopic.MONEY, correct = true)
        raw = TopicAccuracy.record(raw, ProblemTopic.MONEY, correct = false)
        raw = TopicAccuracy.record(raw, ProblemTopic.GEOMETRY, correct = true)
        assertEquals(
            mapOf(
                ProblemTopic.GEOMETRY to TopicTally(1, 1),
                ProblemTopic.MONEY to TopicTally(1, 2),
            ),
            TopicAccuracy.decode(raw),
        )
    }

    @Test
    fun `percent rounds and is null before the first problem`() {
        assertNull(TopicTally(0, 0).percent)
        assertEquals(50, TopicTally(1, 2).percent)
        assertEquals(67, TopicTally(2, 3).percent)
        assertEquals(100, TopicTally(5, 5).percent)
    }
}
