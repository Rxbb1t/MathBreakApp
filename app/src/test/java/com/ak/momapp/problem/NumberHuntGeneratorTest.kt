package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberHuntGeneratorTest {

    private val generator = NumberHuntGenerator(Random(seed = 7))

    private fun manyProblems(
        difficulty: Difficulty,
        language: AppLanguage = AppLanguage.ENGLISH,
    ): List<Problem> = List(SAMPLE_SIZE) { generator.generate(difficulty.toLevel(), language) }

    private fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        var d = 2
        while (d * d <= n) {
            if (n % d == 0) return false
            d++
        }
        return true
    }

    /** Rebuilds the rule from the English prompt; null means largest-prime. */
    private fun ruleFor(text: String): ((Int) -> Boolean)? = when {
        "even" in text -> { n -> n % 2 == 0 }
        "odd" in text -> { n -> n % 2 != 0 }
        "multiples of" in text -> {
            val k = Regex("""multiples of (\d+)""").find(text)!!.groupValues[1].toInt()
            val fits: (Int) -> Boolean = { n -> n % k == 0 }
            fits
        }
        "largest prime" in text -> null
        else -> throw AssertionError("unrecognized hunt prompt: '$text'")
    }

    @Test
    fun `correct cards are exactly what the prompt asks for`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                val rule = ruleFor(problem.text)
                if (rule != null) {
                    assertEquals(
                        "wrong correct set for '${problem.text}' over ${problem.cards}",
                        problem.cards.filter(rule).toSet(),
                        problem.correctCards,
                    )
                } else {
                    val primes = problem.cards.filter(::isPrime)
                    assertTrue("under two primes in ${problem.cards}", primes.size >= 2)
                    assertEquals(
                        "largest prime of ${problem.cards}",
                        setOf(primes.max()),
                        problem.correctCards,
                    )
                }
            }
        }
    }

    @Test
    fun `cards are distinct and leave something to find and something to skip`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                assertEquals(
                    "duplicate cards in ${problem.cards}",
                    problem.cards.size,
                    problem.cards.toSet().size,
                )
                assertTrue("nothing to tap in '${problem.text}'", problem.correctCards.isNotEmpty())
                assertTrue(
                    "no decoys in '${problem.text}' over ${problem.cards}",
                    problem.correctCards.size < problem.cards.size,
                )
            }
        }
    }

    @Test
    fun `hunts are tap-answered with notebook notes but no hints`() {
        for (difficulty in Difficulty.entries) {
            for (language in AppLanguage.entries) {
                for (problem in manyProblems(difficulty, language)) {
                    assertEquals(ProblemKind.SELECT, problem.kind)
                    assertTrue(problem.tapAnswered)
                    assertTrue("hints on a hunt '${problem.text}'", problem.hints.isEmpty())
                    assertTrue("no notes on '${problem.text}'", problem.notes.isNotEmpty())
                    assertTrue("blank reveal for '${problem.text}'", problem.revealText.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `the answer is the sum of the correct cards`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty)) {
                assertEquals(problem.correctCards.sum(), problem.answer)
            }
        }
    }

    @Test
    fun `easy hunts stay visual - no prime questions`() {
        for (problem in manyProblems(Difficulty.EASY)) {
            assertFalse("prime hunt at EASY", "prime" in problem.text)
        }
    }

    @Test
    fun `spread size grows with the level`() {
        assertEquals(setOf(6), manyProblems(Difficulty.EASY).map { it.cards.size }.toSet())
        assertEquals(setOf(8), manyProblems(Difficulty.MEDIUM).map { it.cards.size }.toSet())
        assertEquals(setOf(9), manyProblems(Difficulty.HARD).map { it.cards.size }.toSet())
    }

    @Test
    fun `romanian hunts speak romanian`() {
        for (difficulty in Difficulty.entries) {
            for (problem in manyProblems(difficulty, AppLanguage.ROMANIAN)) {
                assertTrue("not Romanian: '${problem.text}'", problem.text.startsWith("Atinge"))
            }
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 300
    }
}
