package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetProblemGeneratorTest {

    private val generator = SetProblemGenerator(Random(seed = 11))

    private fun manyProblems(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
    ): List<Problem> = List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), language) }

    private fun parseElements(line: String): List<Int> =
        line.substringAfter("{").substringBefore("}").split(",").map { it.trim().toInt() }

    @Test
    fun `the answer counts the asked-for set operation`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                val lines = problem.text.lines()
                assertEquals("unexpected shape: '${problem.text}'", 3, lines.size)
                val a = parseElements(lines[0]).toSet()
                val b = parseElements(lines[1]).toSet()
                val expected = when {
                    "∩" in lines[2] -> (a intersect b).size
                    "∪" in lines[2] -> (a union b).size
                    "\\" in lines[2] -> (a - b).size
                    else -> throw AssertionError("no operation in '${lines[2]}'")
                }
                assertEquals("'${problem.text}'", expected, problem.answer)
            }
        }
    }

    @Test
    fun `both sets list distinct sorted elements that truly overlap`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                val lines = problem.text.lines()
                for (line in lines.take(2)) {
                    val elements = parseElements(line)
                    assertEquals("duplicates in '$line'", elements.size, elements.toSet().size)
                    assertEquals("unsorted '$line'", elements.sorted(), elements)
                }
                val shared = parseElements(lines[0]) intersect parseElements(lines[1]).toSet()
                assertTrue("disjoint sets in '${problem.text}'", shared.isNotEmpty())
            }
        }
    }

    @Test
    fun `set problems are typed with two hints and notebook notes`() {
        for (difficulty in Difficulty.entries) {
            for (language in AppLanguage.entries) {
                for (problem in manyProblems(difficulty, language)) {
                    assertEquals(ProblemKind.SETS, problem.kind)
                    assertFalse(problem.tapAnswered)
                    assertEquals("hints in '${problem.text}'", 2, problem.hints.size)
                    assertTrue(problem.hints.all(String::isNotBlank))
                    assertTrue("no notes for '${problem.text}'", problem.notes.isNotEmpty())
                }
            }
        }
    }

    @Test
    fun `easy sticks to intersections`() {
        for (problem in manyProblems(Difficulty.EASY)) {
            assertTrue("non-intersection at EASY: '${problem.text}'", "∩" in problem.text)
        }
    }

    @Test
    fun `union and difference arrive at the higher levels`() {
        val mediumTexts = manyProblems(Difficulty.MEDIUM).map { it.text }
        assertTrue(mediumTexts.any { "∪" in it })
        val hardTexts = manyProblems(Difficulty.HARD).map { it.text }
        assertTrue(hardTexts.any { "∪" in it })
        assertTrue(hardTexts.any { "\\" in it })
    }

    @Test
    fun `romanian set problems ask in romanian`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty, AppLanguage.ROMANIAN)) {
                assertTrue("not Romanian: '${problem.text}'", "Câte elemente" in problem.text)
            }
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 300
    }
}
