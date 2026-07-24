package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily chain is five problems that are really one, so the thing worth
 * testing is not any single step but that each answer is genuinely the
 * next step's input, and that no draw can produce a step with no answer.
 *
 * Everything below re-derives the mathematics from the numbers actually
 * PRINTED in the text. Asking the generator what it thinks the answer is
 * would agree with any mistake in it.
 */
class DailyChallengeGeneratorTest {

    private val generator = DailyChallengeGenerator()
    private val start = LocalDate.of(2026, 1, 1)

    private fun days(count: Int) = (0 until count).map { start.plusDays(it.toLong()) }

    private fun chain(date: LocalDate, language: AppLanguage = AppLanguage.ENGLISH) =
        generator.generate(date, language)

    /** The numbers printed in a step's text, in the order they appear. */
    private fun numbersIn(text: String): List<Int> =
        Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every day deals five steps and an intro`() {
        for (date in days(400)) {
            val challenge = chain(date)
            assertEquals("$date", 5, challenge.stages.size)
            assertTrue("$date has no intro", challenge.intro.isNotBlank())
        }
    }

    @Test
    fun `the same day always deals the same chain`() {
        assertEquals(chain(start).stages.map { it.answer }, chain(start).stages.map { it.answer })
    }

    @Test
    fun `both languages draw the same numbers`() {
        for (date in days(200)) {
            assertEquals(
                "$date",
                chain(date, AppLanguage.ENGLISH).stages.map { it.answer },
                chain(date, AppLanguage.ROMANIAN).stages.map { it.answer },
            )
        }
    }

    // ── Step 1: the anchor ───────────────────────────────────────────────

    /**
     * The hunt must have exactly ONE answer, or the step is unfair in a way
     * she cannot argue with: the app would reject a number that satisfies
     * everything it asked for.
     */
    @Test
    fun `the anchor is the only number in its window that fits`() {
        for (date in days(400)) {
            val stage = chain(date).stages[0]
            // "Step 1 ... between LOW and HIGH ... multiple of A ... of B"
            val printed = numbersIn(stage.text).drop(1)
            val (low, high, a, b) = printed
            val fitting = (low..high).filter { it % a == 0 && it % b == 0 }
            assertEquals("$date window $low..$high for $a and $b", listOf(stage.answer), fitting)
        }
    }

    @Test
    fun `the anchor is a moderate double-digit number`() {
        for (date in days(400)) {
            val anchor = chain(date).stages[0].answer
            assertTrue("$date anchored at $anchor", anchor in 10..99)
        }
    }

    // ── Step 2: the operator shift ───────────────────────────────────────

    @Test
    fun `the remainder is the anchor multiplied then divided`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val (multiplier, divisor) = numbersIn(stages[1].text).drop(1)
            assertEquals("$date", stages[0].answer * multiplier % divisor, stages[1].answer)
        }
    }

    /**
     * The draw's guarantee rather than arithmetic: never a zero divisor,
     * and never a clean division either. A remainder of nothing would leave
     * step 3 shifting a zero and turn this step into a yes-or-no.
     */
    @Test
    fun `the divisor never divides by zero and never divides exactly`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val divisor = numbersIn(stages[1].text)[2]
            assertTrue("$date divided by $divisor", divisor > 0)
            assertTrue("$date left no remainder", stages[1].answer > 0)
            assertTrue("$date remainder was $divisor or more", stages[1].answer < divisor)
        }
    }

    // ── Step 3: the working-memory twist ─────────────────────────────────

    @Test
    fun `the digit sum follows from the remainder and the shift`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val shifted = stages[1].answer + numbersIn(stages[2].text)[1]
            assertEquals("$date shifted to $shifted", shifted / 10 + shifted % 10, stages[2].answer)
        }
    }

    /** The step says "its two digits", so it had better always have two. */
    @Test
    fun `the shifted value always has exactly two digits`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val shifted = stages[1].answer + numbersIn(stages[2].text)[1]
            assertTrue("$date shifted to $shifted", shifted in 10..99)
        }
    }

    // ── Step 4: the filter ───────────────────────────────────────────────

    @Test
    fun `the gap reaches the first prime above the digit sum`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val digitSum = stages[2].answer
            val prime = generateSequence(digitSum + 1) { it + 1 }.first(::isPrime)
            assertEquals("$date reaching up from $digitSum", prime - digitSum, stages[3].answer)
        }
    }

    /**
     * There is always a prime STRICTLY above, so the reach can never be
     * zero and the step always has somewhere to go.
     */
    @Test
    fun `the gap is never zero`() {
        for (date in days(400)) {
            assertTrue("$date", chain(date).stages[3].answer >= 1)
        }
    }

    // ── Step 5: the finale ───────────────────────────────────────────────

    @Test
    fun `the finale multiplies the gap by the anchor from step one`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", stages[3].answer * stages[0].answer - tail, stages[4].answer)
        }
    }

    /**
     * The app promises a plain number keyboard and no minus sign, so the
     * last step is the one place the chain could break that and must not,
     * however the draws land.
     */
    @Test
    fun `every answer is a non-negative whole number that fits the field`() {
        for (date in days(400)) {
            for (stage in chain(date).stages) {
                assertTrue("$date '${stage.text}'", stage.answer >= 0)
                assertTrue("$date '${stage.text}'", stage.answer <= 99_999)
            }
        }
    }

    // ── What the steps carry with them ───────────────────────────────────

    /**
     * The text deliberately never restates an earlier answer, which is what
     * makes this a memory exercise. That only works if the first hint hands
     * the value back: forgetting a number is not a maths failure, and the
     * challenge never reveals, so without this a slip ends the whole day.
     */
    @Test
    fun `each step after the first recalls the value it needs`() {
        for (date in days(200)) {
            for (language in AppLanguage.entries) {
                val stages = chain(date, language).stages
                for (step in 1..3) {
                    assertTrue(
                        "$date $language step ${step + 1} does not recall step $step",
                        stages[step].hints.first().contains(stages[step - 1].answer.toString()),
                    )
                }
                val finale = stages[4].hints.first()
                assertTrue(
                    "$date $language the finale recalls neither W nor the anchor",
                    finale.contains(stages[3].answer.toString()) &&
                        finale.contains(stages[0].answer.toString()),
                )
            }
        }
    }

    /**
     * And the text must NOT hand it over, or nothing is being carried
     * forward and step 5's reach back to step 1 costs her nothing.
     */
    @Test
    fun `the finale does not print the anchor it asks her to remember`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            assertTrue(
                "$date printed the anchor in '${stages[4].text}'",
                stages[0].answer !in numbersIn(stages[4].text),
            )
        }
    }

    @Test
    fun `every step carries two hints and a helper sheet`() {
        for (date in days(200)) {
            for (language in AppLanguage.entries) {
                for (stage in chain(date, language).stages) {
                    assertEquals("$date $language '${stage.text}'", 2, stage.hints.size)
                    assertTrue("$date $language '${stage.text}'", stage.notes.isNotEmpty())
                }
            }
        }
    }

    /**
     * The sheet teaches the ideas, so it has to be generic reference text
     * rather than anything built out of today's numbers.
     *
     * Written as "it is the same every day" after the obvious version
     * ("it never contains the answer") turned out to be the wrong claim: a
     * sheet that lists the primes to thirty contains 2 and 3, and step 4's
     * answer is a small number, so the two collide constantly without
     * anything being given away. What actually matters is that no value
     * from this chain can reach the sheet, and a sheet that never varies
     * cannot carry one.
     */
    @Test
    fun `the helper sheet carries nothing from today's chain`() {
        for (language in AppLanguage.entries) {
            val fixed = chain(start, language).stages.map { it.notes }
            for (date in days(200)) {
                assertEquals("$date $language", fixed, chain(date, language).stages.map { it.notes })
            }
        }
    }

    @Test
    fun `both languages are written, and differently`() {
        for (date in days(100)) {
            val en = chain(date, AppLanguage.ENGLISH)
            val ro = chain(date, AppLanguage.ROMANIAN)
            assertNotEquals("$date intro", en.intro, ro.intro)
            for (i in en.stages.indices) {
                assertNotEquals("$date step $i", en.stages[i].text, ro.stages[i].text)
                assertNotEquals(
                    "$date step $i hint",
                    en.stages[i].hints.first(),
                    ro.stages[i].hints.first(),
                )
            }
        }
    }

    /**
     * A chain whose numbers never moved would be memorised in a week, and
     * seeding from the date means one wrong constant could freeze them
     * without anything else looking wrong.
     */
    @Test
    fun `the numbers move from day to day`() {
        val finales = days(60).map { chain(it).stages.last().answer }.distinct()
        assertTrue("only ${finales.size} distinct finales in 60 days", finales.size > 25)
        val anchors = days(60).map { chain(it).stages.first().answer }.distinct()
        assertTrue("only ${anchors.size} distinct anchors in 60 days", anchors.size > 8)
    }

    private fun isPrime(n: Int): Boolean = n >= 2 && (2..n / 2).none { n % it == 0 }

    /** So a four-number header can be destructured in one line above. */
    private operator fun <T> List<T>.component4(): T = this[3]
}
