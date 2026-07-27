package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estimation is the one exercise where being close counts, which makes it
 * the one exercise that can be wrong in a new way: too generous and it
 * marks anything right, too strict and it punishes the very shortcut it
 * spends three helper-sheet notes teaching.
 */
class EstimationGeneratorTest {

    private val samples = 3_000
    private val levels = listOf(Level.of(8), Level.of(20), Level.of(35), Level.of(50), Level.of(75), Level.of(95))

    private fun problems(language: AppLanguage = AppLanguage.ENGLISH): List<Problem> {
        val generator = EstimationGenerator(Random(17))
        return levels.flatMap { level -> List(samples / levels.size) { generator.generate(level, language) } }
    }

    /** The estimate the worked solution says the shortcut arrives at. */
    private fun taughtEstimate(problem: Problem): Int {
        val step = problem.solution.first()
        val match = Regex("comes to (\\d+)").find(step) ?: Regex("adică (\\d+)").find(step)
        return requireNotNull(match) { "no estimate in: $step" }.groupValues[1].toInt()
    }

    @Test
    fun `every problem is an estimate on its own topic`() {
        for (problem in problems()) {
            assertEquals(ProblemKind.ESTIMATE, problem.kind)
            assertEquals(ProblemTopic.ESTIMATION, problem.kind.topic)
        }
    }

    @Test
    fun `answers are whole and positive`() {
        for (problem in problems()) {
            assertTrue("answer was ${problem.answer} in: ${problem.text}", problem.answer > 0)
        }
    }

    /**
     * THE ONE THAT MATTERS. The prompt says a close answer counts, the
     * hint says which number to round, and the worked solution rounds it.
     * If the number that method lands on could still be marked wrong, the
     * exercise is punishing its own advice.
     *
     * Measured before the tolerance was tied to the rounding error: the
     * shortcut failed about one Hard problem in five, because 1499 was
     * being rounded to 1000. Nothing failed at the time, because nothing
     * checked.
     */
    @Test
    fun `the shortcut the solution teaches is always accepted`() {
        for (language in AppLanguage.entries) {
            for (problem in problems(language)) {
                val estimate = taughtEstimate(problem)
                assertTrue(
                    "rounding gave $estimate, answer ${problem.answer}, " +
                        "tolerance ${problem.tolerance}: ${problem.text}",
                    problem.accepts(estimate),
                )
            }
        }
    }

    @Test
    fun `the exact answer is always accepted`() {
        for (problem in problems()) {
            assertTrue(problem.accepts(problem.answer))
            // And exact is not "close": she should get the plain praise.
            assertTrue(!problem.isApproximate(problem.answer))
        }
    }

    /**
     * Tolerance has to mean something. Doubling the answer or halving it
     * are the shapes a guess takes, and neither is estimating.
     */
    @Test
    fun `a wild guess is not close enough`() {
        for (problem in problems()) {
            assertTrue("half of ${problem.answer} passed", !problem.accepts(problem.answer / 2))
            assertTrue("double ${problem.answer} passed", !problem.accepts(problem.answer * 2))
        }
    }

    /** Being near but not on it is what earns the "close enough" wording. */
    @Test
    fun `a near miss counts and is reported as approximate`() {
        for (problem in problems()) {
            val near = problem.answer + problem.tolerance
            assertTrue(problem.accepts(near))
            assertTrue("$near should read as approximate", problem.isApproximate(near))
            assertTrue("one past the tolerance passed", !problem.accepts(near + 1))
        }
    }

    /**
     * The band narrows as she climbs. Compared as a SHARE of the answer,
     * since the answers themselves grow with the level and the raw amounts
     * would tell us nothing.
     */
    @Test
    fun `the tolerance tightens as the level rises`() {
        fun shareAt(points: Int): Double {
            val generator = EstimationGenerator(Random(5))
            return List(2_000) { generator.generate(Level.of(points), AppLanguage.ENGLISH) }
                .map { it.tolerance.toDouble() / it.answer }
                .average()
        }
        val easy = shareAt(10)
        val normal = shareAt(47)
        val hard = shareAt(90)
        assertTrue("easy $easy should be looser than normal $normal", easy > normal)
        assertTrue("normal $normal should be looser than hard $hard", normal > hard)
    }

    /**
     * There is always something left to round. "300 × 4" would have no
     * estimating in it at all, and the roll lands on round numbers often
     * enough that this needs enforcing rather than hoping.
     *
     * Stated as "rounding changes the question", which is the property
     * that actually matters. An earlier version of this test asked instead
     * that no operand end in a zero, and wrongly failed "3210 × 4" -- a
     * fine estimate, since 3210 is rounded to 3200 at its own scale. The
     * question is not whether a number looks round, it is whether rounding
     * it does anything.
     */
    @Test
    fun `rounding always changes the question`() {
        for (problem in problems()) {
            val question = problem.text.substringAfter("is ").removeSuffix("?")
            val rounded = problem.solution.first()
                .substringAfter("that is ")
                .substringBefore(", which comes to")
            assertNotEquals(
                "nothing to round in: ${problem.text}",
                question,
                rounded,
            )
        }
    }

    @Test
    fun `every problem carries two hints, notes and working`() {
        for (problem in problems()) {
            assertEquals("hints on: ${problem.text}", 2, problem.hints.size)
            assertTrue("no helper sheet on: ${problem.text}", problem.notes.isNotEmpty())
            assertEquals("working on: ${problem.text}", 2, problem.solution.size)
        }
    }

    /**
     * The last step ends on the exact answer, which is the rule every
     * worked solution in the app follows, and doubly the point here: the
     * whole reason to show working after an estimate is to say what the
     * estimate was near.
     */
    @Test
    fun `the working ends at the exact answer in both languages`() {
        for (language in AppLanguage.entries) {
            for (problem in problems(language)) {
                val last = problem.solution.last()
                val lastNumber = Regex("\\d+").findAll(last).last().value.toInt()
                assertEquals("working ended at $lastNumber: $last", problem.answer, lastNumber)
            }
        }
    }

    /** Both languages authored, and actually different from each other. */
    @Test
    fun `both languages are written and not the same text`() {
        val english = EstimationGenerator(Random(99)).generate(Level.of(50), AppLanguage.ENGLISH)
        val romanian = EstimationGenerator(Random(99)).generate(Level.of(50), AppLanguage.ROMANIAN)
        // Same seed, so the numbers match and only the wording differs.
        assertEquals(english.answer, romanian.answer)
        assertNotEquals(english.text, romanian.text)
        assertNotEquals(english.hints.first(), romanian.hints.first())
        assertNotEquals(english.notes.first(), romanian.notes.first())
    }

    /** All four shapes are reachable somewhere on the scale. */
    @Test
    fun `every shape is dealt`() {
        val symbols = problems().map { problem ->
            when {
                "+" in problem.text -> "+"
                "−" in problem.text -> "−"
                "×" in problem.text -> "×"
                "÷" in problem.text -> "÷"
                else -> "?"
            }
        }.toSet()
        assertEquals(setOf("+", "−", "×", "÷"), symbols)
    }

    /**
     * Division only becomes an estimate once the divisor has two digits.
     * "608 ÷ 8" is a times table, and dealing it here would be teaching
     * her to round something that never needed rounding.
     */
    @Test
    fun `division always has a two-digit divisor`() {
        val divisors = problems()
            .filter { "÷" in it.text }
            .map { it.text.substringAfter("÷").filter(Char::isDigit).toInt() }
        assertTrue("no division problems were dealt", divisors.isNotEmpty())
        assertTrue("a single-digit divisor was dealt: ${divisors.minOrNull()}", divisors.all { it >= 10 })
    }

    /**
     * Tolerance is the one field that could quietly loosen every other
     * exercise in the app, since one shared answer check reads it.
     */
    @Test
    fun `no other kind of problem accepts a near miss`() {
        val generator = ProblemGenerator(Random(31))
        for (difficulty in Difficulty.entries) {
            repeat(2_000) {
                val problem = generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH)
                if (problem.kind == ProblemKind.ESTIMATE) return@repeat
                assertEquals(
                    "${problem.kind} carries a tolerance: ${problem.text}",
                    0,
                    problem.tolerance,
                )
                assertTrue(
                    "${problem.kind} accepted a wrong answer",
                    !problem.accepts(problem.answer + 1),
                )
            }
        }
    }

    @Test
    fun `estimates are dealt as a modest share of a break`() {
        val generator = ProblemGenerator(Random(13))
        for (points in listOf(12, 47, 85)) {
            val dealt = List(4_000) { generator.generate(Level.of(points)) }
            val share = dealt.count { it.kind == ProblemKind.ESTIMATE } / 4_000.0
            assertTrue("estimation was $share of level $points", share in 0.03..0.12)
        }
    }

    /**
     * The core carries chains at the bottom and equations above, so it is
     * the slice that quietly pays for every topic added after it. It was
     * squeezed to one percent in the middle of Normal by adding estimation
     * before the other weights were trimmed to make room.
     */
    @Test
    fun `the core keeps a real share of every level`() {
        val generator = ProblemGenerator(Random(29))
        for (points in listOf(5, 25, 47, 60, 85)) {
            val dealt = List(4_000) { generator.generate(Level.of(points)) }
            val share = dealt.count { it.kind.topic == ProblemTopic.CORE } / 4_000.0
            assertTrue("core was only $share of level $points", share >= 0.07)
        }
    }
}
