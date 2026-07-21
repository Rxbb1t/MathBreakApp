package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The point of the fine scale is that difficulty moves in small steps
 * instead of lurching a whole tier, and that what she is asked genuinely
 * differs between two levels that share a name.
 *
 * These are properties rather than fixed expectations: the exact operand
 * ranges are tuning and will keep moving, but "harder means bigger" and
 * "nothing appears out of nowhere" must hold at every point on the scale.
 */
class ContinuousDifficultyTest {

    private val samples = 400

    private fun problemsAt(points: Int, seed: Int = 7): List<Problem> {
        val generator = ProblemGenerator(Random(seed))
        return List(samples) { generator.generate(Level.of(points), AppLanguage.ENGLISH) }
    }

    private fun biggestNumber(problem: Problem): Int =
        Regex("\\d+").findAll(problem.text + " " + problem.cards.joinToString(" "))
            .mapNotNull { it.value.toIntOrNull() }
            .maxOrNull() ?: 0

    /** The typical size of the numbers on screen at one point of the scale. */
    private fun typicalSize(points: Int): Double =
        problemsAt(points).map { biggestNumber(it).toDouble() }.sorted()[samples / 2]

    // ── Every point on the scale is a working level ──────────────────────

    @Test
    fun `every level on the scale deals valid problems`() {
        for (points in Level.MIN..Level.MAX step 4) {
            val problems = problemsAt(points, seed = points + 1)
            for (problem in problems) {
                assertTrue("empty text at $points", problem.text.isNotEmpty())
                assertTrue("negative answer at $points: ${problem.text}", problem.answer >= 0)
            }
        }
    }

    @Test
    fun `a problem carries the level it was dealt at`() {
        for (points in listOf(0, 17, 33, 50, 66, 83, 100)) {
            val level = Level.of(points)
            assertTrue(problemsAt(points).all { it.level == level })
        }
    }

    // ── Harder means harder, all the way up ──────────────────────────────

    /**
     * Two levels inside the SAME band have to differ. This is the whole
     * argument for the change: "Normal" covering one fixed set of problems
     * is what made the app feel stuck.
     */
    @Test
    fun `two levels within one band are genuinely different`() {
        val low = typicalSize(Level.EASY_TOP + 2)
        val high = typicalSize(Level.MEDIUM_TOP - 2)
        assertEquals(Difficulty.MEDIUM, Level.of(Level.EASY_TOP + 2).band)
        assertEquals(Difficulty.MEDIUM, Level.of(Level.MEDIUM_TOP - 2).band)
        assertTrue("bottom of Normal $low vs top $high", high > low * 1.5)
    }

    @Test
    fun `the numbers grow as the level climbs`() {
        val sizes = listOf(5, 25, 45, 60).map(::typicalSize)
        for (i in 1 until sizes.size) {
            assertTrue("size dipped from ${sizes[i - 1]} to ${sizes[i]}", sizes[i] > sizes[i - 1])
        }
    }

    // ── Shapes arrive and leave gradually ────────────────────────────────

    private fun shareOf(points: Int, predicate: (Problem) -> Boolean): Double =
        problemsAt(points, seed = points + 31).count(predicate) / samples.toDouble()

    @Test
    fun `geometry fades in rather than switching on at a boundary`() {
        val isGeometry = { p: Problem -> p.kind == ProblemKind.GEOMETRY }
        val bottom = shareOf(8, isGeometry)
        val edge = shareOf(Level.EASY_TOP + 4, isGeometry)
        val settled = shareOf(Level.MEDIUM_ANCHOR, isGeometry)
        assertEquals("geometry should be absent at the bottom", 0.0, bottom, 0.0001)
        assertTrue("nothing at the edge of Normal", edge > 0.0)
        assertTrue("edge $edge should be thinner than settled $settled", edge < settled)
    }

    @Test
    fun `equations take over from plain chains gradually`() {
        val chainsLow = shareOf(10) { it.kind == ProblemKind.ARITHMETIC }
        val chainsMid = shareOf(Level.EASY_TOP + 6) { it.kind == ProblemKind.ARITHMETIC }
        val equationsMid = shareOf(Level.EASY_TOP + 6) { it.kind == ProblemKind.EQUATION }
        assertTrue("chains should dominate low down", chainsLow > 0.0)
        assertTrue("chains should be thinning by $chainsMid", chainsMid < chainsLow)
        assertTrue("equations should have started by then", equationsMid > 0.0)
    }

    @Test
    fun `the tap exercises thin out but never disappear`() {
        val isTap = { p: Problem -> p.tapAnswered }
        val low = shareOf(10, isTap)
        val high = shareOf(Level.HARD_ANCHOR, isTap)
        assertTrue("taps $high should thin from $low", high < low)
        assertTrue("taps vanished entirely at the top", high > 0.02)
    }

    /**
     * A drill she chose herself is the one place a topic can be asked for
     * at any level, including ones the mix would never offer it at.
     */
    @Test
    fun `any topic can be drilled at any level`() {
        for (topic in ProblemTopic.entries) {
            for (points in listOf(0, Level.MEDIUM_ANCHOR, Level.MAX)) {
                val generator = ProblemGenerator(Random(topic.ordinal + points))
                repeat(20) {
                    val problem = generator.generate(
                        level = Level.of(points),
                        language = AppLanguage.ENGLISH,
                        topics = setOf(topic),
                    )
                    assertTrue("empty $topic at $points", problem.text.isNotEmpty())
                    assertTrue("negative $topic at $points", problem.answer >= 0)
                }
            }
        }
    }

    // ── The review bias ──────────────────────────────────────────────────

    /**
     * A due shape should actually come back. It cannot be guaranteed on
     * any single roll (the generator is asked for a topic, not a
     * template), so what matters is that it lands often, and always
     * within the topic she stumbled on.
     */
    @Test
    fun `a due shape is brought back within its own topic`() {
        val generator = ProblemGenerator(Random(11))
        val target = generator.generate(Level.of(40), AppLanguage.ENGLISH, setOf(ProblemTopic.MONEY))
        val shape = ProblemShape.of(target)!!
        val pick = ReviewPick(ProblemTopic.MONEY, shape)

        val served = List(200) {
            generator.generate(
                level = Level.of(40),
                language = AppLanguage.ENGLISH,
                review = pick,
            )
        }
        assertTrue("review should stay inside its topic", served.all { it.kind == ProblemKind.MONEY })
        val hits = served.count { ProblemShape.of(it) == shape }
        assertTrue("the due shape never came back", hits > 0)
    }
}
