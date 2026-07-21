package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two cross-cutting quality rules in [ProblemGenerator]:
 * nothing degenerate (× 1, + 0 and friends) reaches her, and the same
 * question doesn't come round again while it's still fresh.
 *
 * These are easy rules to write and never notice failing, so the tests
 * check both directions: that real output is clean, and that the filter
 * would actually catch a bad problem if one turned up.
 */
class ProblemQualityTest {

    private fun problems(
        count: Int,
        difficulty: Difficulty,
        seed: Int = 7,
    ): List<Problem> {
        val generator = ProblemGenerator(Random(seed))
        return List(count) { generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH) }
    }

    /**
     * Not a filter, an invariant: every generator draws its operands
     * from ranges starting at 2, so × 1 and + 0 are unreachable by
     * construction. Measured zero across 14,400 samples. This test
     * exists so a generator added later can't quietly start emitting
     * them -- there is no runtime guard to fall back on.
     */
    @Test
    fun `no degenerate operations reach the player`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1..6) {
                problems(SAMPLE, difficulty, seed).forEach { problem ->
                    assertTrue(
                        "degenerate operation in: ${problem.text}",
                        !DEGENERATE_PATTERNS.any { it.containsMatchIn(problem.text) },
                    )
                }
            }
        }
    }

    /**
     * The test above is only worth having if its patterns recognise the
     * thing they look for: an ASCII * where the app prints × would leave
     * it quietly matching nothing and still passing.
     */
    @Test
    fun `the degenerate patterns match the operators the app actually prints`() {
        val bad = listOf("7 × 1 = ?", "8 ÷ 1 = ?", "12 + 0 = ?", "30 − 0 = ?", "1 × 9 = ?")
        bad.forEach { text ->
            assertTrue(
                "should have been caught: $text",
                DEGENERATE_PATTERNS.any { it.containsMatchIn(text) },
            )
        }
        // And it must not be so greedy that it eats honest problems.
        val good = listOf("7 × 10 = ?", "40 ÷ 10 = ?", "12 + 30 = ?", "100 − 20 = ?", "21 × 3 = ?")
        good.forEach { text ->
            assertTrue(
                "false positive on: $text",
                !DEGENERATE_PATTERNS.any { it.containsMatchIn(text) },
            )
        }
    }

    @Test
    fun `the same question does not repeat while it is still fresh`() {
        for (difficulty in Difficulty.entries) {
            // A tap exercise repeats its instruction on purpose, so the
            // question's identity is the instruction plus its cards --
            // the same thing the filter keys on.
            val keys = problems(SAMPLE, difficulty).map { problem ->
                problem.text + "|" + problem.cards.sorted().joinToString(",")
            }
            // The ring holds 25, so any two identical questions must be
            // further apart than that.
            keys.forEachIndexed { index, key ->
                val window = keys.subList(index + 1, minOf(index + 1 + 25, keys.size))
                assertTrue("repeat inside the ring: $key", key !in window)
            }
        }
    }

    /**
     * The reroll is bounded, so with a single topic switched on it is
     * allowed to give up and repeat. What it must never do is fail to
     * produce a problem, or hang.
     */
    @Test
    fun `a single enabled topic still yields problems`() {
        for (topic in ProblemTopic.entries) {
            val generator = ProblemGenerator(Random(3))
            repeat(30) {
                val problem = generator.generate(
                    Difficulty.MEDIUM.toLevel(),
                    AppLanguage.ENGLISH,
                    setOf(topic),
                )
                assertTrue("empty problem text for $topic", problem.text.isNotEmpty())
            }
        }
    }

    /**
     * A tap exercise's text is only its instruction, so a repeat filter
     * keyed on text alone would reject nearly every number hunt and
     * quietly strangle the topic. The cards are what vary, so what comes
     * back must vary too.
     */
    @Test
    fun `card exercises keep their variety through the repeat filter`() {
        val generator = ProblemGenerator(Random(9))
        val hunts = List(200) {
            generator.generate(Difficulty.MEDIUM.toLevel(), AppLanguage.ENGLISH, setOf(ProblemTopic.NUMBERS))
        }.filter { it.cards.isNotEmpty() }

        assertTrue("expected card exercises, got none", hunts.size > 50)
        val spreads = hunts.map { it.cards.sorted() }.toSet()
        // If the filter were keyed on the prompt alone this collapses to
        // a handful of repeatedly-served spreads.
        assertTrue(
            "card spreads barely varied: ${spreads.size} distinct from ${hunts.size}",
            spreads.size > hunts.size * 3 / 4,
        )
    }

    /**
     * Word problems come in three shapes now. Two-step stories must
     * actually take two operations, and backwards ones must ask for a
     * number bigger than any in the story -- if a "how many at the
     * start" answer were smaller than what's left, the story would be
     * nonsense however tidy the arithmetic.
     */
    @Test
    fun `word problems come in more than one shape and each adds up`() {
        val generator = ProblemGenerator(Random(13))
        val words = List(1200) {
            generator.generate(Difficulty.HARD.toLevel(), AppLanguage.ENGLISH, setOf(ProblemTopic.WORD))
        }.filter { it.kind == ProblemKind.WORD }

        val numbersOf = { text: String ->
            Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        }
        // Two-step stories carry three numbers; one-step ones carry two.
        val twoStep = words.filter { numbersOf(it.text).size == 3 }
        assertTrue("no two-step stories at HARD", twoStep.size > words.size / 5)

        twoStep.forEach { problem ->
            val (a, b, c) = numbersOf(problem.text)
            assertTrue(
                "two-step answer doesn't reconstruct: '${problem.text}' -> ${problem.answer}",
                problem.answer == a * b + c || problem.answer == a * b - c,
            )
        }

        // Backwards stories: the start must exceed what is left over.
        val reverse = words.filter { problem ->
            val n = numbersOf(problem.text)
            n.size == 2 && problem.answer == n[0] + n[1]
        }
        assertTrue("no backwards stories", reverse.isNotEmpty())
        reverse.forEach { problem ->
            val n = numbersOf(problem.text)
            assertTrue(
                "start not bigger than the remainder: '${problem.text}'",
                problem.answer > n.max(),
            )
        }
    }

    /**
     * A worked solution that contradicts the answer is worse than none:
     * she would be told, in the app's own voice, something the app knows
     * is wrong. So the last number in the last step has to BE the answer.
     *
     * Tap-answered problems are excluded, and only they: their [answer]
     * is the index of a button (0 = "<", 1 = "="), not a value, so the
     * rule is meaningless there. The test below covers those instead.
     *
     * Checked in both languages, because the steps are authored twice
     * and a mistake in the Romanian half would otherwise never be seen.
     */
    @Test
    fun `every worked solution ends at the answer it explains`() {
        for (language in AppLanguage.entries) {
            for (difficulty in Difficulty.entries) {
                val generator = ProblemGenerator(Random(31))
                repeat(SAMPLE) {
                    val problem = generator.generate(difficulty.toLevel(), language)
                    val steps = problem.solution
                    if (steps.isEmpty() || problem.tapAnswered) return@repeat
                    val lastNumber = Regex("\\d+").findAll(steps.last())
                        .map { it.value.toInt() }
                        .lastOrNull()
                    assertEquals(
                        "solution disagrees with the answer for '${problem.text}': $steps",
                        problem.answer,
                        lastNumber,
                    )
                }
            }
        }
    }

    /**
     * The tap kinds that do carry working must land on the same symbol
     * the reveal shows. A solution ending in "so >" under a reveal that
     * says "<" is the same contradiction as a wrong number, just harder
     * to notice.
     */
    @Test
    fun `tap-answered solutions end on the symbol the reveal shows`() {
        for (language in AppLanguage.entries) {
            for (difficulty in Difficulty.entries) {
                val generator = ProblemGenerator(Random(23))
                repeat(SAMPLE) {
                    val problem = generator.generate(difficulty.toLevel(), language)
                    if (!problem.tapAnswered || problem.solution.isEmpty()) return@repeat
                    // TRUE_FALSE reveals "✗ (= 56)"; the symbol is the head of it.
                    val symbol = problem.revealText.substringBefore(" ")
                    assertTrue(
                        "solution ends on the wrong symbol for '${problem.text}': ${problem.solution}",
                        problem.solution.last().trimEnd().endsWith(symbol),
                    )
                }
            }
        }
    }

    /**
     * Which kinds explain themselves, pinned down so a generator added
     * later can't quietly ship without working. The five listed as bare
     * are deliberate, not forgotten: SELECT reveals the matching numbers,
     * TARGET reveals a valid pick, and MISSING_OP's working is the
     * question with the sign filled in. Those reveals ARE the explanation.
     */
    @Test
    fun `every kind that should explain itself does`() {
        val withSolution = mutableSetOf<ProblemKind>()
        val seen = mutableSetOf<ProblemKind>()
        for (language in AppLanguage.entries) {
            for (difficulty in Difficulty.entries) {
                val generator = ProblemGenerator(Random(41))
                repeat(SAMPLE * 4) {
                    val problem = generator.generate(difficulty.toLevel(), language)
                    seen.add(problem.kind)
                    if (problem.solution.isNotEmpty()) withSolution.add(problem.kind)
                }
            }
        }
        assertEquals("not every kind was sampled", ProblemKind.entries.toSet(), seen)
        val expectedBare = setOf(
            ProblemKind.SELECT,
            ProblemKind.TARGET,
            ProblemKind.MISSING_OP,
        )
        assertEquals(
            "wrong set of kinds carries working",
            ProblemKind.entries.toSet() - expectedBare,
            withSolution,
        )
    }

    /** The kinds that carry working, and the ones that rightly don't. */
    @Test
    fun `story problems explain themselves`() {
        val generator = ProblemGenerator(Random(17))
        val withSolution = mutableSetOf<ProblemKind>()
        for (difficulty in Difficulty.entries) {
            repeat(SAMPLE * 2) {
                val problem = generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH)
                if (problem.solution.isNotEmpty()) withSolution.add(problem.kind)
            }
        }
        assertTrue("word problems carry no working", ProblemKind.WORD in withSolution)
        assertTrue("money problems carry no working", ProblemKind.MONEY in withSolution)
        assertTrue("time problems carry no working", ProblemKind.TIME in withSolution)
    }

    @Test
    fun `money answers are denominated in euro`() {
        val generator = MoneyProblemGenerator(Random(5))
        val units = Difficulty.entries.flatMap { difficulty ->
            List(60) { generator.generate(difficulty.toLevel(), AppLanguage.ENGLISH).answerUnit }
        }.toSet()
        // Counting answers (jars, weeks) carry no unit; the rest pay in euro.
        assertEquals(setOf("", "€"), units)
    }

    /**
     * Per-topic levels have to reach the problem itself, not just the
     * Exercises row. A topic held at HARD while the sitting sits at EASY
     * must actually deal HARD problems, and its neighbours must not be
     * dragged up with it.
     */
    @Test
    fun `each topic is dealt at its own level`() {
        val generator = ProblemGenerator(Random(19))
        val problems = List(SAMPLE * 4) {
            generator.generate(
                level = Difficulty.EASY.toLevel(),
                language = AppLanguage.ENGLISH,
                topics = setOf(ProblemTopic.MONEY, ProblemTopic.TIME),
                levelFor = { topic ->
                    if (topic == ProblemTopic.MONEY) Difficulty.HARD.toLevel() else Difficulty.EASY.toLevel()
                },
            )
        }
        val money = problems.filter { it.kind == ProblemKind.MONEY }
        val time = problems.filter { it.kind == ProblemKind.TIME }
        assertTrue("no money problems sampled", money.size > 50)
        assertTrue("no time problems sampled", time.size > 50)
        assertEquals(
            "money should have been dealt at its own level",
            setOf(Difficulty.HARD),
            money.map { it.difficulty }.toSet(),
        )
        assertEquals(
            "time should have stayed where the sitting is",
            setOf(Difficulty.EASY),
            time.map { it.difficulty }.toSet(),
        )
    }

    /** Left alone, the generator still deals one level for everything. */
    @Test
    fun `without per-topic levels every problem uses the one passed in`() {
        val generator = ProblemGenerator(Random(27))
        val levels = List(SAMPLE * 2) {
            generator.generate(Difficulty.MEDIUM.toLevel(), AppLanguage.ENGLISH).difficulty
        }.toSet()
        assertEquals(setOf(Difficulty.MEDIUM), levels)
    }

    private companion object {
        const val SAMPLE = 300

        /** The operators the app prints: × ÷ − are typographic, not ASCII. */
        val DEGENERATE_PATTERNS = listOf(
            Regex("[×÷]\\s*[01]\\b"),
            Regex("\\b[01]\\s*×"),
            Regex("[+−]\\s*0\\b"),
        )
    }
}
