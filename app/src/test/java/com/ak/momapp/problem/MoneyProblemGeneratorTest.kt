package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-derives each money problem's answer from the numbers in its
 * English text, so a broken template or a wrong answer fails loudly.
 */
class MoneyProblemGeneratorTest {

    private val generator = MoneyProblemGenerator(Random(seed = 11))

    private fun many(difficulty: Difficulty, language: AppLanguage = AppLanguage.ENGLISH): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty, language) }

    private fun numbersIn(text: String): List<Int> =
        Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every money type computes correctly and all appear per difficulty`() {
        val expectedPerDifficulty = mapOf(
            Difficulty.EASY to setOf("twoItems", "payNote", "perPiece"),
            Difficulty.MEDIUM to setOf("basket", "payTwo", "exactBudget"),
            Difficulty.HARD to setOf("discount", "marketHaul", "saveUp"),
        )
        for ((difficulty, expected) in expectedPerDifficulty) {
            val seen = mutableSetOf<String>()
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.MONEY, problem.kind)
                assertEquals("difficulty of '${problem.text}'", difficulty, problem.difficulty)
                assertTrue("negative answer in '${problem.text}'", problem.answer > 0)
                val type = verify(problem)
                seen += type
                // Counting answers (jars, weeks) have no unit; the rest pay in lei.
                val expectedUnit = if (type == "exactBudget" || type == "saveUp") "" else "lei"
                assertEquals("unit of '${problem.text}'", expectedUnit, problem.answerUnit)
            }
            assertEquals("at $difficulty", expected, seen)
        }
    }

    /**
     * Returns the problem type after checking its math against the text.
     * Each type's English templates all carry a marker phrase, and their
     * numbers appear in a fixed order — both are authoring rules in
     * MoneyProblemGenerator. The branch order resolves marker overlaps
     * ("altogether" before "together", "banknote" before "note").
     */
    private fun verify(problem: Problem): String {
        val text = problem.text
        val answer = problem.answer
        val numbers = numbersIn(text)
        return when {
            "altogether" in text -> {
                val (q, p, c) = numbers
                assertEquals("'$text'", q * p + c, answer)
                "basket"
            }

            "banknote" in text -> {
                val (q1, p1, q2, p2, n) = numbers
                assertEquals("'$text'", n - q1 * p1 - q2 * p2, answer)
                "marketHaul"
            }

            "together" in text -> {
                val (a, b) = numbers
                assertEquals("'$text'", a + b, answer)
                "twoItems"
            }

            "exactly" in text -> {
                val (p, t) = numbers
                assertEquals("budget not exact in '$text'", 0, t % p)
                assertEquals("'$text'", t / p, answer)
                "exactBudget"
            }

            "off" in text -> {
                val (p, d) = numbers
                assertEquals("uneven discount in '$text'", 0, p * d % 100)
                assertEquals("'$text'", p - p * d / 100, answer)
                "discount"
            }

            "week" in text -> {
                val (p, s, m) = numbers
                assertEquals("uneven saving in '$text'", 0, (p - s) % m)
                assertEquals("'$text'", (p - s) / m, answer)
                "saveUp"
            }

            "each" in text -> {
                val (k, p) = numbers
                assertEquals("'$text'", k * p, answer)
                "perPiece"
            }

            "note" in text && numbers.size == 3 -> {
                val (a, b, n) = numbers
                assertEquals("'$text'", n - a - b, answer)
                "payTwo"
            }

            "note" in text -> {
                val (c, n) = numbers
                assertEquals("'$text'", n - c, answer)
                "payNote"
            }

            else -> throw AssertionError("unrecognised money problem: '$text'")
        }
    }

    @Test
    fun `every money problem brings two hints, and notes above easy`() {
        for (difficulty in Difficulty.entries) {
            for (language in AppLanguage.entries) {
                for (problem in many(difficulty, language)) {
                    assertEquals("hints in '${problem.text}'", 2, problem.hints.size)
                    assertTrue(
                        "placeholder left in '${problem.text}'",
                        !problem.text.contains("{"),
                    )
                    if (difficulty == Difficulty.EASY) {
                        assertTrue("notes at EASY in '${problem.text}'", problem.notes.isEmpty())
                    } else {
                        assertTrue("no notes in '${problem.text}'", problem.notes.isNotEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun `medium money never shows or asks for a number past 500`() {
        for (problem in many(Difficulty.MEDIUM)) {
            assertTrue("answer over 500 in '${problem.text}'", problem.answer <= 500)
            for (number in numbersIn(problem.text) + problem.hints.flatMap(::numbersIn)) {
                assertTrue("number $number over 500 in '${problem.text}'", number <= 500)
            }
        }
    }

    @Test
    fun `romanian money problems avoid english wording`() {
        val markers = listOf("How", "What", "You", "pay", "costs", "change", "week", "buy")
        for (difficulty in Difficulty.entries) {
            for (problem in many(difficulty, AppLanguage.ROMANIAN)) {
                for (marker in markers) {
                    assertTrue(
                        "english '$marker' in '${problem.text}'",
                        !problem.text.contains(marker),
                    )
                }
            }
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 400
    }
}
