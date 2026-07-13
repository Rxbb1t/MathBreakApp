package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissingOperatorGeneratorTest {

    private val generator = MissingOperatorGenerator(Random(seed = 7))

    /** The sign at [index] applied to the pair, or null when it leaves whole numbers. */
    private fun apply(index: Int, a: Int, b: Int): Int? =
        when (MissingOperatorGenerator.SYMBOLS[index]) {
            "+" -> a + b
            "−" -> if (a >= b) a - b else null
            "×" -> a * b
            else -> if (b != 0 && a % b == 0) a / b else null
        }

    /** Re-derives "a ? b = c" and asserts the answer sign is the only fit. */
    private fun verify(problem: Problem) {
        val match = Regex("""^(\d+) \? (\d+) = (\d+)$""").find(problem.text)
            ?: throw AssertionError("unparseable line: '${problem.text}'")
        val (a, b, c) = match.destructured.toList().map(String::toInt)
        val fitting = MissingOperatorGenerator.SYMBOLS.indices.filter { apply(it, a, b) == c }
        assertEquals("ambiguous or wrong sign for '${problem.text}'", listOf(problem.answer), fitting)
        assertEquals(MissingOperatorGenerator.SYMBOLS[problem.answer], problem.revealText)
    }

    @Test
    fun `exactly one sign ever fits the line`() {
        for (difficulty in Difficulty.entries) {
            repeat(400) {
                verify(generator.generate(difficulty, AppLanguage.ENGLISH))
            }
        }
    }

    @Test
    fun `all four signs appear at every difficulty`() {
        for (difficulty in Difficulty.entries) {
            val answers = List(400) { generator.generate(difficulty, AppLanguage.ENGLISH) }
                .map { it.answer }
                .toSet()
            assertEquals("missing signs at $difficulty", setOf(0, 1, 2, 3), answers)
        }
    }

    @Test
    fun `taps carry no hints and identical text in both languages`() {
        for (difficulty in Difficulty.entries) {
            val enGenerator = MissingOperatorGenerator(Random(seed = 99))
            val roGenerator = MissingOperatorGenerator(Random(seed = 99))
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
}
