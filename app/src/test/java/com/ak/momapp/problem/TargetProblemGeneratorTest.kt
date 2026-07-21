package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetProblemGeneratorTest {

    private val generator = TargetProblemGenerator(Random(seed = 3))

    private fun many(difficulty: Difficulty, language: AppLanguage = AppLanguage.ENGLISH): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), language) }

    @Test
    fun `a valid pick always exists among the cards`() {
        for (difficulty in Difficulty.entries) {
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.TARGET, problem.kind)
                val solution = problem.revealText.split(" + ").map(String::toInt)
                assertEquals("pick count in '${problem.text}'", problem.pickCount, solution.size)
                assertEquals("solution sum in '${problem.text}'", problem.answer, solution.sum())
                // The planted solution must be takeable from the spread —
                // duplicates and all.
                val pool = problem.cards.toMutableList()
                for (card in solution) {
                    assertTrue("card $card missing from ${problem.cards}", pool.remove(card))
                }
            }
        }
    }

    @Test
    fun `text quotes the pick count and target, in both languages`() {
        for (language in AppLanguage.entries) {
            for (difficulty in Difficulty.entries) {
                for (problem in many(difficulty, language)) {
                    val numbers = Regex("""\d+""").findAll(problem.text).map { it.value.toInt() }.toList()
                    assertEquals("numbers in '${problem.text}'", 2, numbers.size)
                    assertEquals("pick count in '${problem.text}'", problem.pickCount, numbers[0])
                    assertEquals("target in '${problem.text}'", problem.answer, numbers[1])
                }
            }
        }
    }

    @Test
    fun `spread and pick sizes grow with difficulty`() {
        assertTrue(many(Difficulty.EASY).all { it.cards.size == 6 && it.pickCount == 3 })
        assertTrue(many(Difficulty.MEDIUM).all { it.cards.size == 7 && it.pickCount in 3..4 })
        assertTrue(many(Difficulty.HARD).all { it.cards.size == 8 && it.pickCount == 4 })
    }

    @Test
    fun `card problems carry no hints`() {
        for (difficulty in Difficulty.entries) {
            for (problem in many(difficulty)) {
                // Tap exercises are hint-free — the Hint button stays hidden.
                assertTrue("unexpected hints in '${problem.text}'", problem.hints.isEmpty())
            }
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 300
    }
}
