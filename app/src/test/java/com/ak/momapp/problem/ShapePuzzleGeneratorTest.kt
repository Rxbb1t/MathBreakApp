package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Solves every generated puzzle the way a person would — top line first,
 * one new shape per line — and checks the final line gives the stated
 * answer. A puzzle this solver can't crack is a bug.
 */
class ShapePuzzleGeneratorTest {

    private val generator = ShapePuzzleGenerator(Random(seed = 5))

    private fun many(difficulty: Difficulty): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH) }

    @Test
    fun `every puzzle is solvable line by line and computes correctly`() {
        for (difficulty in Difficulty.entries) {
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.PUZZLE, problem.kind)
                val values = solveGivenLines(problem.text)

                val expectedShapes = if (difficulty == Difficulty.EASY) 2 else 3
                assertEquals("shape count in '${problem.text}'", expectedShapes, values.size)
                assertTrue(
                    "shape values not distinct in '${problem.text}'",
                    values.values.toSet().size == values.size,
                )
                assertTrue(
                    "non-positive shape value in '${problem.text}'",
                    values.values.all { it > 0 },
                )

                val finalLine = problem.text.lines().last()
                assertTrue("no question in '${problem.text}'", finalLine.endsWith("= ?"))
                assertEquals(
                    "final line of '${problem.text}'",
                    evaluate(finalLine.removeSuffix(" = ?"), values),
                    problem.answer,
                )
            }
        }
    }

    /**
     * Walks the given (non-question) lines; each must be solvable with at
     * most one unknown shape, mirroring how the puzzle is meant to be read.
     */
    private fun solveGivenLines(text: String): Map<String, Int> {
        val values = mutableMapOf<String, Int>()
        for (line in text.lines().dropLast(1)) {
            val (lhs, rhs) = line.split(" = ")
            val total = rhs.toInt()
            val tokens = lhs.split(" ")
            val operators = tokens.filterIndexed { i, _ -> i % 2 == 1 }.toSet()
            assertEquals("mixed operators in given line '$line'", 1, operators.size)
            val shapes = tokens.filterIndexed { i, _ -> i % 2 == 0 }
            val unknown = shapes.filter { it !in values }.toSet()
            assertTrue("more than one new shape in '$line'", unknown.size <= 1)

            if (operators.first() == "+") {
                val knownSum = shapes.filter { it in values }.sumOf { values.getValue(it) }
                unknown.firstOrNull()?.let { shape ->
                    val count = shapes.count { it == shape }
                    assertEquals("uneven split in '$line'", 0, (total - knownSum) % count)
                    values[shape] = (total - knownSum) / count
                }
            } else {
                assertEquals("unexpected operator in '$line'", setOf("×"), operators)
                val knownProduct = shapes.filter { it in values }
                    .fold(1) { acc, shape -> acc * values.getValue(shape) }
                unknown.firstOrNull()?.let { shape ->
                    assertEquals("uneven division in '$line'", 0, total % knownProduct)
                    values[shape] = total / knownProduct
                }
            }
        }
        return values
    }

    /** Evaluates "🍎 × 🍐 + ⭐" with × before + and −. */
    private fun evaluate(expression: String, values: Map<String, Int>): Int {
        val tokens = expression.split(" ")
        var result = 0
        var sign = 1
        var term = values.getValue(tokens[0])
        for (i in 1 until tokens.size step 2) {
            val value = values.getValue(tokens[i + 1])
            when (tokens[i]) {
                "×" -> term *= value
                "+", "−" -> {
                    result += sign * term
                    sign = if (tokens[i] == "+") 1 else -1
                    term = value
                }
                else -> throw AssertionError("unexpected operator '${tokens[i]}' in '$expression'")
            }
        }
        return result + sign * term
    }

    companion object {
        private const val SAMPLE_SIZE = 300
    }
}
