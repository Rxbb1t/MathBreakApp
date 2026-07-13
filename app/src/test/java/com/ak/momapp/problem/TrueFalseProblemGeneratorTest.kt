package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrueFalseProblemGeneratorTest {

    private val generator = TrueFalseProblemGenerator(Random(seed = 7))

    /** Re-derives the truth of "a op b = claim" straight from the text. */
    private fun verify(problem: Problem) {
        val match = Regex("""^(\d+) ([+−×÷]) (\d+) = (\d+)$""").find(problem.text)
            ?: throw AssertionError("unparseable claim: '${problem.text}'")
        val (a, op, b, claim) = match.destructured
        val value = when (op) {
            "+" -> a.toInt() + b.toInt()
            "−" -> a.toInt() - b.toInt()
            "×" -> a.toInt() * b.toInt()
            else -> {
                assertEquals("non-exact division in '${problem.text}'", 0, a.toInt() % b.toInt())
                a.toInt() / b.toInt()
            }
        }
        val expected = if (value == claim.toInt()) 0 else 1
        assertEquals("wrong verdict for '${problem.text}'", expected, problem.answer)
        if (expected == 0) {
            assertEquals("✓", problem.revealText)
        } else {
            assertEquals("✗ (= $value)", problem.revealText)
            assertTrue("claim equals truth in '${problem.text}'", claim.toInt() != value)
        }
    }

    @Test
    fun `every claim's verdict matches the arithmetic`() {
        for (difficulty in Difficulty.entries) {
            repeat(400) {
                verify(generator.generate(difficulty, AppLanguage.ENGLISH))
            }
        }
    }

    @Test
    fun `both verdicts appear and claims never go negative`() {
        for (difficulty in Difficulty.entries) {
            val problems = List(400) { generator.generate(difficulty, AppLanguage.ENGLISH) }
            val answers = problems.map { it.answer }.toSet()
            assertEquals("one-sided claims at $difficulty", setOf(0, 1), answers)
            for (problem in problems) {
                assertTrue("negative number in '${problem.text}'", "−" !in problem.text.substringAfter("= "))
            }
        }
    }

    @Test
    fun `taps carry no hints and identical text in both languages`() {
        for (difficulty in Difficulty.entries) {
            val enGenerator = TrueFalseProblemGenerator(Random(seed = 99))
            val roGenerator = TrueFalseProblemGenerator(Random(seed = 99))
            repeat(100) {
                val en = enGenerator.generate(difficulty, AppLanguage.ENGLISH)
                val ro = roGenerator.generate(difficulty, AppLanguage.ROMANIAN)
                assertEquals("language leaked into '${en.text}'", en.text, ro.text)
                assertTrue("hints on tap kind", en.hints.isEmpty())
                assertTrue(en.tapAnswered)
                assertTrue(en.submitsOnTap)
            }
        }
    }

    @Test
    fun `division claims skip easy`() {
        val problems = List(400) { generator.generate(Difficulty.EASY, AppLanguage.ENGLISH) }
        assertTrue("÷ claim at EASY", problems.none { "÷" in it.text })
    }
}
