package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Both sides are re-evaluated from the text, so the tapped-symbol answer
 * is verified against the real comparison every time.
 */
class ComparisonProblemGeneratorTest {

    private val generator = ComparisonProblemGenerator(Random(seed = 7))

    private fun many(difficulty: Difficulty): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH) }

    /** Evaluates "a + b", "a − b", "a × b", or a plain number. */
    private fun evalSide(side: String): Int {
        val tokens = side.split(" ")
        if (tokens.size == 1) return tokens[0].toInt()
        val (a, op, b) = tokens
        return when (op) {
            "+" -> a.toInt() + b.toInt()
            "−" -> a.toInt() - b.toInt()
            "×" -> a.toInt() * b.toInt()
            else -> throw AssertionError("unknown operator in '$side'")
        }
    }

    @Test
    fun `the tapped symbol always matches the real comparison`() {
        for (difficulty in Difficulty.entries) {
            for (problem in many(difficulty)) {
                val sides = problem.text.split("\n?\n")
                assertEquals("shape of '${problem.text}'", 2, sides.size)
                val left = evalSide(sides[0])
                val right = evalSide(sides[1])
                val expected = when {
                    left < right -> 0
                    left == right -> 1
                    else -> 2
                }
                assertEquals("'${problem.text}'", expected, problem.answer)
                assertEquals(
                    "'${problem.text}'",
                    ComparisonProblemGenerator.SYMBOLS[expected],
                    problem.revealText,
                )
                assertEquals(ProblemKind.COMPARE, problem.kind)
                assertTrue("negative value in '${problem.text}'", left >= 0 && right >= 0)
            }
        }
    }

    @Test
    fun `all three outcomes appear and equality is common`() {
        for (difficulty in Difficulty.entries) {
            val answers = many(difficulty).map(Problem::answer)
            assertEquals(setOf(0, 1, 2), answers.toSet())
            val equalShare = answers.count { it == 1 } / SAMPLE_SIZE.toDouble()
            assertTrue("equal share was $equalShare", equalShare in 0.2..0.45)
        }
    }

    @Test
    fun `medium comparisons stay small and carry no hints`() {
        for (problem in many(Difficulty.MEDIUM)) {
            val numbers = Regex("""\d+""").findAll(problem.text)
            for (match in numbers) {
                assertTrue("${match.value} too big in '${problem.text}'", match.value.toInt() <= 3500)
            }
            // Tap exercises are hint-free — the Hint button stays hidden.
            assertTrue("unexpected hints in '${problem.text}'", problem.hints.isEmpty())
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 400
    }
}
