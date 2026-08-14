package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageReplayTest {

    @Test
    fun `every generated problem carries a spec that reproduces it`() {
        val generator = ProblemGenerator(Random(20260814))
        repeat(200) {
            val problem = generator.generate(Level.of(50), AppLanguage.ENGLISH)
            val spec = problem.spec
            assertNotNull("problem had no spec: ${problem.text}", spec)
            val again = ProblemGenerator.replay(spec!!, AppLanguage.ENGLISH)
            assertEquals(problem.text, again.text)
            assertEquals(problem.answer, again.answer)
            assertEquals(problem.kind, again.kind)
            assertEquals(problem.hints, again.hints)
            assertEquals(problem.solution, again.solution)
        }
    }

    @Test
    fun `replaying in the other language keeps the maths and changes the words`() {
        val generator = ProblemGenerator(Random(4242))
        var differed = 0
        repeat(200) {
            val english = generator.generate(Level.of(50), AppLanguage.ENGLISH)
            val romanian = ProblemGenerator.replay(english.spec!!, AppLanguage.ROMANIAN)
            assertEquals("answer moved for: ${english.text}", english.answer, romanian.answer)
            assertEquals(english.kind, romanian.kind)
            assertEquals(english.cards, romanian.cards)
            assertEquals(english.correctCards, romanian.correctCards)
            assertEquals(english.tolerance, romanian.tolerance)
            assertEquals(english.answerUnit, romanian.answerUnit)
            assertEquals(english.diagram, romanian.diagram)
            if (english.text != romanian.text) differed++
        }
        // Language-blind kinds (COMPARE, TARGET) share their text, so this is
        // a floor rather than a total. Most problems must actually translate.
        assertTrue("only $differed of 200 problems changed wording", differed > 120)
    }
}
