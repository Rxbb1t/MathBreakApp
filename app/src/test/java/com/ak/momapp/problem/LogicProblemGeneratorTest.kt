package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-derives each riddle's answer from the numbers in its English text,
 * so a broken template or a wrong answer fails loudly.
 */
class LogicProblemGeneratorTest {

    private val generator = LogicProblemGenerator(Random(seed = 3))
    private val names = listOf("Toni", "Vera")

    private fun many(difficulty: Difficulty, language: AppLanguage = AppLanguage.ENGLISH): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), names, language) }

    private fun numbersIn(text: String): List<Int> =
        Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every riddle type computes correctly and all appear per difficulty`() {
        val expectedPerDifficulty = mapOf(
            Difficulty.EASY to setOf("sequence", "addBackwards", "leftover"),
            Difficulty.MEDIUM to setOf("sequence", "doubleBackwards", "ages"),
            Difficulty.HARD to setOf("headsLegs", "threeStepBackwards", "twiceAsOld"),
        )
        for (difficulty in Difficulty.entries) {
            val seen = mutableSetOf<String>()
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.LOGIC, problem.kind)
                seen += verify(problem)
            }
            assertEquals("at $difficulty", expectedPerDifficulty.getValue(difficulty), seen)
        }
    }

    /**
     * Returns the riddle type after checking its math against the text.
     * Each type's English templates all carry a marker phrase, and their
     * numbers appear in a fixed order — both are authoring rules in
     * LogicProblemGenerator.
     */
    private fun verify(problem: Problem): String {
        val text = problem.text
        val answer = problem.answer
        val numbers = numbersIn(text)
        return when {
            "comes next" in text -> {
                // The numbers live in the drawn row of boxes, not the text.
                val row = problem.diagram
                assertTrue("no sequence boxes for '$text'", row is Diagram.SequenceRow)
                val values = (row as Diagram.SequenceRow).values
                assertEquals("last box of '$text'", "?", values.last())
                val terms = values.dropLast(1).map(String::toInt) + answer
                val step = terms[1] - terms[0]
                val byStep = terms.zipWithNext().all { (a, b) -> b - a == step }
                val ratio = terms[1] / terms[0]
                val byRatio = terms.zipWithNext().all { (a, b) -> b == a * ratio }
                assertTrue("no pattern in $terms (answer $answer)", byStep || byRatio)
                "sequence"
            }

            "left?" in text -> {
                val (total, people, each) = numbers
                assertEquals("'$text'", total - people * each, answer)
                "leftover"
            }

            "legs" in text -> {
                val (heads, legs) = numbers
                assertEquals("odd legs in '$text'", 0, (legs - 2 * heads) % 2)
                assertEquals("'$text'", (legs - 2 * heads) / 2, answer)
                "headsLegs"
            }

            "halv" in text -> {
                val (m, c, r) = numbers
                assertEquals("uneven undo in '$text'", 0, (2 * r - c) % m)
                assertEquals("'$text'", (2 * r - c) / m, answer)
                "threeStepBackwards"
            }

            "doubl" in text -> {
                val (c, r) = numbers
                assertEquals("uneven halving in '$text'", 0, (r - c) % 2)
                assertEquals("'$text'", (r - c) / 2, answer)
                "doubleBackwards"
            }

            "twice as old" in text -> {
                val total = numbers.single()
                assertEquals("total not in thirds in '$text'", 0, total % 3)
                assertEquals("'$text'", total / 3 * 2, answer)
                "twiceAsOld"
            }

            "years older" in text -> {
                val (gap, total) = numbers
                assertEquals("uneven ages in '$text'", 0, (total - gap) % 2)
                assertEquals("'$text'", (total - gap) / 2 + gap, answer)
                "ages"
            }

            "now" in text -> {
                val (c, r) = numbers
                assertEquals("'$text'", r - c, answer)
                "addBackwards"
            }

            else -> throw AssertionError("unrecognised riddle: '$text'")
        }
    }

    @Test
    fun `people riddles use the family names when there are enough`() {
        val peopleRiddles = many(Difficulty.MEDIUM).filter { "years older" in it.text } +
            many(Difficulty.HARD).filter { "twice as old" in it.text }
        assertTrue("no people riddles sampled", peopleRiddles.isNotEmpty())
        for (problem in peopleRiddles) {
            assertTrue(
                "family names missing in '${problem.text}'",
                names.all { problem.text.contains(it) },
            )
        }
    }

    @Test
    fun `romanian riddles avoid english wording`() {
        val markers = listOf("What", "How", "I add", "I double", "I think", "years", "slices", "legs", "heads", "cake")
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
