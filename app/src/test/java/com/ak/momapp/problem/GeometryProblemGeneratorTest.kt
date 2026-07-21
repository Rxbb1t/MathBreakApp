package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Re-derives each geometry problem's answer from the numbers in its
 * English text, so a broken template or a wrong answer fails loudly.
 */
class GeometryProblemGeneratorTest {

    private val generator = GeometryProblemGenerator(Random(seed = 7))

    private fun many(difficulty: Difficulty, language: AppLanguage = AppLanguage.ENGLISH): List<Problem> =
        List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), language) }

    private fun numbersIn(text: String): List<Int> =
        Regex("""\d+""").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every geometry type computes correctly and all appear per difficulty`() {
        val expectedPerDifficulty = mapOf(
            Difficulty.MEDIUM to setOf("perimeter", "area", "thirdAngle", "squareSide", "ladder"),
            Difficulty.HARD to setOf("shortcut", "border", "volume", "isosceles", "laps"),
        )
        for ((difficulty, expected) in expectedPerDifficulty) {
            val seen = mutableSetOf<String>()
            for (problem in many(difficulty)) {
                assertEquals(ProblemKind.GEOMETRY, problem.kind)
                assertEquals("difficulty of '${problem.text}'", difficulty, problem.difficulty)
                val type = verify(problem)
                seen += type
                // Lengths answer in the template's unit, angles in degrees,
                // and the counting types (tiles, cubes) in nothing at all.
                when (type) {
                    "thirdAngle", "isosceles" ->
                        assertEquals("unit of '${problem.text}'", "°", problem.answerUnit)
                    "area", "volume" ->
                        assertEquals("unit of '${problem.text}'", "", problem.answerUnit)
                    else -> assertTrue(
                        "unit '${problem.answerUnit}' of '${problem.text}'",
                        problem.answerUnit in listOf("m", "cm", "dm"),
                    )
                }
            }
            assertEquals("at $difficulty", expected, seen)
        }
    }

    /**
     * Returns the problem type after checking its math against the text
     * and its diagram against the type. Each type's English templates all
     * carry a marker phrase, and their numbers appear in a fixed order —
     * both are authoring rules in GeometryProblemGenerator.
     */
    private fun verify(problem: Problem): String {
        val text = problem.text
        val answer = problem.answer
        val numbers = numbersIn(text)
        val diagram = problem.diagram
        return when {
            "all the way around" in text -> {
                val (a, b) = numbers
                assertEquals("'$text'", 2 * (a + b), answer)
                assertTrue("diagram of '$text'", diagram is Diagram.Rectangle && !diagram.grid)
                "perimeter"
            }

            "one square meter" in text -> {
                val (a, b) = numbers
                assertEquals("'$text'", a * b, answer)
                assertTrue("diagram of '$text'", diagram is Diagram.Rectangle && diagram.grid)
                "area"
            }

            "third angle" in text -> {
                val (x, y) = numbers
                assertEquals("'$text'", 180 - x - y, answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.AngleTriangle && diagram.topLabel == "?" &&
                        diagram.leftDegrees == x && diagram.rightDegrees == y,
                )
                "thirdAngle"
            }

            "ladder" in text -> {
                val (c, a) = numbers
                assertEquals("not a whole triple in '$text'", c * c - a * a, answer * answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.RightTriangle && diagram.rightLabel == "?",
                )
                "ladder"
            }

            "right angle" in text -> {
                val (a, b) = numbers
                assertEquals("not a whole triple in '$text'", a * a + b * b, answer * answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.RightTriangle && diagram.slantLabel == "?",
                )
                "shortcut"
            }

            "border" in text -> {
                val (area, a) = numbers
                assertEquals("length doesn't divide the area in '$text'", 0, area % a)
                assertEquals("'$text'", 2 * (a + area / a), answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.Rectangle && diagram.heightLabel == "?" &&
                        diagram.innerLabel != null,
                )
                "border"
            }

            "cubes" in text -> {
                val (a, b, c) = numbers
                assertEquals("'$text'", a * b * c, answer)
                assertTrue("diagram of '$text'", diagram is Diagram.Box)
                "volume"
            }

            "laps" in text -> {
                val (a, b, n) = numbers
                assertEquals("'$text'", n * 2 * (a + b), answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.Rectangle && diagram.lapsLabel != null,
                )
                "laps"
            }

            "one side" in text -> {
                val p = numbers.single()
                assertEquals("perimeter not in quarters in '$text'", 0, p % 4)
                assertEquals("'$text'", p / 4, answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.Square && diagram.sideLabel == "?",
                )
                "squareSide"
            }

            "equal" in text -> {
                val t = numbers.single()
                assertEquals("odd base angles in '$text'", 0, (180 - t) % 2)
                assertEquals("'$text'", (180 - t) / 2, answer)
                assertTrue(
                    "diagram of '$text'",
                    diagram is Diagram.AngleTriangle && diagram.leftLabel == "?" &&
                        diagram.rightLabel == "?" && diagram.leftDegrees == answer,
                )
                "isosceles"
            }

            else -> throw AssertionError("unrecognised geometry problem: '$text'")
        }
    }

    @Test
    fun `every geometry problem brings helper notes, two hints and a diagram`() {
        for (difficulty in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
            for (language in AppLanguage.entries) {
                for (problem in many(difficulty, language)) {
                    assertEquals("hints in '${problem.text}'", 2, problem.hints.size)
                    assertTrue("no notes in '${problem.text}'", problem.notes.isNotEmpty())
                    assertTrue(
                        "blank note in '${problem.text}'",
                        problem.notes.all(String::isNotBlank),
                    )
                    assertTrue("no diagram in '${problem.text}'", problem.diagram != null)
                }
            }
        }
    }

    @Test
    fun `medium geometry never shows or asks for a number past 500`() {
        for (problem in many(Difficulty.MEDIUM)) {
            assertTrue("answer over 500 in '${problem.text}'", problem.answer <= 500)
            for (number in numbersIn(problem.text) + problem.hints.flatMap(::numbersIn)) {
                assertTrue("number $number over 500 in '${problem.text}'", number <= 500)
            }
        }
    }

    @Test
    fun `romanian geometry avoids english wording`() {
        val markers = listOf("What", "How", " of ", "meters", "ladder", "angle", "square", "laps")
        for (difficulty in listOf(Difficulty.MEDIUM, Difficulty.HARD)) {
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
