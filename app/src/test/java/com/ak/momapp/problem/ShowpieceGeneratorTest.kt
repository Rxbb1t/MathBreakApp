package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The showpieces are hand-authored, one function each, so an arithmetic
 * slip in any one of them would only ever surface in front of her. These
 * tests re-derive what can be re-derived and pin down the rest.
 */
class ShowpieceGeneratorTest {

    private fun sample(count: Int = 4_000, language: AppLanguage = AppLanguage.ENGLISH) =
        ShowpieceGenerator(Random(11)).let { g -> List(count) { g.generate(Difficulty.HARD.toLevel(), language) } }

    private fun numbersIn(text: String) =
        Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()

    /** All thirty must actually be reachable. */
    @Test
    fun `every showpiece shape is dealt`() {
        val shapes = sample(20_000).map { it.text.replace(Regex("\\d+"), "#") }.toSet()
        assertEquals("not all thirty showpieces come up", 30, shapes.size)
    }

    @Test
    fun `answers are whole and never negative`() {
        sample().forEach {
            assertTrue("negative answer: ${it.text}", it.answer >= 0)
        }
    }

    /**
     * The rule the whole worked-solution feature rests on: the last
     * number of the last step IS the answer. Checked in both languages,
     * because the steps are authored twice.
     */
    @Test
    fun `every solution ends at the answer it explains`() {
        for (language in AppLanguage.entries) {
            sample(language = language).forEach { problem ->
                assertTrue("no solution for: ${problem.text}", problem.solution.isNotEmpty())
                assertEquals(
                    "solution disagrees with the answer for '${problem.text}': ${problem.solution}",
                    problem.answer,
                    numbersIn(problem.solution.last()).lastOrNull(),
                )
            }
        }
    }

    /** Two hints and a helper-sheet note, like every other hard problem. */
    @Test
    fun `every showpiece carries hints and a note`() {
        sample().forEach {
            assertEquals("wrong hint count for: ${it.text}", 2, it.hints.size)
            assertTrue("no helper-sheet note for: ${it.text}", it.notes.isNotEmpty())
            assertTrue("empty hint for: ${it.text}", it.hints.none(String::isBlank))
        }
    }

    /**
     * Both halves must be written, and they must be different from each
     * other -- a forgotten translation shows up as the English text
     * appearing under the Romanian setting.
     */
    @Test
    fun `both languages are authored`() {
        val english = ShowpieceGenerator(Random(5))
        val romanian = ShowpieceGenerator(Random(5))
        repeat(2_000) {
            val en = english.generate(Difficulty.HARD.toLevel(), AppLanguage.ENGLISH)
            val ro = romanian.generate(Difficulty.HARD.toLevel(), AppLanguage.ROMANIAN)
            // Same seed, same rolls, so these are the same puzzle twice.
            assertEquals("the two runs drifted apart", en.answer, ro.answer)
            assertTrue("untranslated text: ${en.text}", en.text != ro.text)
            assertTrue("untranslated note: ${en.notes}", en.notes != ro.notes)
        }
    }

    /**
     * Spot-checks on the shapes whose whole point is a counting trap, so
     * a "fix" that quietly turns them into ordinary arithmetic fails
     * here rather than passing unnoticed.
     */
    @Test
    fun `the counting traps are still traps`() {
        val problems = sample(20_000)

        // A log in p pieces takes p-1 cuts, and the answer is cuts x minutes.
        problems.filter { it.text.startsWith("A log is sawn") }.forEach {
            val (pieces, minutes) = numbersIn(it.text)
            assertEquals(it.text, (pieces - 1) * minutes, it.answer)
        }
        // Posts along a straight fence outnumber the gaps by one.
        problems.filter { it.text.startsWith("A straight fence") }.forEach {
            val (length, spacing) = numbersIn(it.text)
            assertEquals(it.text, length / spacing + 1, it.answer)
        }
        // Handshakes are halved; postcards are not.
        problems.filter { it.text.startsWith("There are") && it.text.contains("shakes hands") }
            .forEach {
                val people = numbersIn(it.text).first()
                assertEquals(it.text, people * (people - 1) / 2, it.answer)
            }
        problems.filter { it.text.contains("sends a postcard") }.forEach {
            val cousins = numbersIn(it.text).first()
            assertEquals(it.text, cousins * (cousins - 1), it.answer)
        }
        // The lily was half the pond the day before, not halfway through.
        problems.filter { it.text.contains("water lily") }.forEach {
            val day = numbersIn(it.text).first()
            assertEquals(it.text, day - 1, it.answer)
        }
        // The snail escapes on its last climb, before it can slip back.
        problems.filter { it.text.contains("snail") }.forEach {
            val (depth, climb, slip) = numbersIn(it.text)
            assertEquals(it.text, (depth - climb) / (climb - slip) + 1, it.answer)
        }
    }

    /**
     * The snail must be able to climb out at all: a slip at least as big
     * as the climb would be an unsolvable puzzle, not a hard one.
     */
    @Test
    fun `the snail always makes progress`() {
        sample(20_000).filter { it.text.contains("snail") }.forEach {
            val (_, climb, slip) = numbersIn(it.text)
            assertTrue("the snail can never get out: ${it.text}", climb > slip)
        }
    }
}
