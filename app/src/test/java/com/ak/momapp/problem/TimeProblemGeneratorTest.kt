package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-derives each clock problem's answer from the h:mm readings in its
 * English text, so a broken template or a wrong answer fails loudly.
 */
class TimeProblemGeneratorTest {

    private val generator = TimeProblemGenerator(Random(seed = 5))

    private fun many(difficulty: Difficulty, language: AppLanguage = AppLanguage.ENGLISH): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty, language) }

    private fun numbersIn(text: String): List<Int> =
        Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every clock type computes correctly and all appear per difficulty`() {
        val expectedPerDifficulty = mapOf(
            Difficulty.EASY to setOf("duration", "wholeHours"),
            Difficulty.MEDIUM to setOf("duration", "conversion"),
            Difficulty.HARD to setOf("duration", "journey"),
        )
        for ((difficulty, expected) in expectedPerDifficulty) {
            val seen = mutableSetOf<String>()
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.TIME, problem.kind)
                assertEquals("difficulty of '${problem.text}'", difficulty, problem.difficulty)
                assertEquals("unit of '${problem.text}'", "min", problem.answerUnit)
                assertTrue("non-positive answer in '${problem.text}'", problem.answer > 0)
                seen += verify(problem)
            }
            assertEquals("at $difficulty", expected, seen)
        }
    }

    /**
     * Returns the problem type after checking its math against the text.
     * Clock times are always printed h:mm, so the digit runs come out in
     * pairs; marker phrases and number order are authoring rules in
     * TimeProblemGenerator.
     */
    private fun verify(problem: Problem): String {
        val text = problem.text
        val answer = problem.answer
        val numbers = numbersIn(text)
        return when {
            "away" in text -> {
                assertEquals("expected four clock readings in '$text'", 8, numbers.size)
                val out = numbers[0] * 60 + numbers[1]
                val back = numbers[6] * 60 + numbers[7]
                assertEquals("'$text'", back - out, answer)
                // The distractor readings must sit between the two ends.
                val stops = listOf(numbers[2] * 60 + numbers[3], numbers[4] * 60 + numbers[5])
                assertTrue("stops outside the trip in '$text'", stops.all { it in out..back })
                "journey"
            }

            "hours" in text && numbers.size == 1 -> {
                assertEquals("'$text'", 60 * numbers.single(), answer)
                "wholeHours"
            }

            "hours" in text -> {
                val (h, m) = numbers
                assertEquals("'$text'", 60 * h + m, answer)
                "conversion"
            }

            else -> {
                assertTrue("expected two clock readings in '$text'", numbers.size == 4)
                val start = numbers[0] * 60 + numbers[1]
                val end = numbers[2] * 60 + numbers[3]
                assertEquals("'$text'", end - start, answer)
                "duration"
            }
        }
    }

    @Test
    fun `every clock problem brings two hints, and notes above easy`() {
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
    fun `medium clock problems never show or ask for a number past 500`() {
        for (problem in many(Difficulty.MEDIUM)) {
            assertTrue("answer over 500 in '${problem.text}'", problem.answer <= 500)
            for (number in numbersIn(problem.text) + problem.hints.flatMap(::numbersIn)) {
                assertTrue("number $number over 500 in '${problem.text}'", number <= 500)
            }
        }
    }

    @Test
    fun `romanian clock problems avoid english wording`() {
        val markers = listOf("How", "What", "minutes", "hours", "leave", "home", "oven", "arrive")
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
