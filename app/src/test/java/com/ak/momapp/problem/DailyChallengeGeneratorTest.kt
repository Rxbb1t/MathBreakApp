package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daily chain is five problems that are really one, so the thing
 * worth testing is not any single step but that each answer is genuinely
 * the next step's input, and that no draw can produce a step with no
 * answer.
 *
 * Everything below re-derives the mathematics from the numbers actually
 * PRINTED in the text. Asking the generator what it thinks the answer is
 * would agree with any mistake in it.
 *
 * The file is in two halves. The first holds what is true of EVERY chain
 * whatever story it is telling, and those run across all four themes at
 * once. The second checks each theme's own arithmetic, on the days that
 * theme is actually dealt.
 */
class DailyChallengeGeneratorTest {

    private val generator = DailyChallengeGenerator()
    private val start = LocalDate.of(2026, 1, 1)

    private fun days(count: Int) = (0 until count).map { start.plusDays(it.toLong()) }

    private fun chain(date: LocalDate, language: AppLanguage = AppLanguage.ENGLISH) =
        generator.generate(date, language)

    /** The days in the window that are told through [theme]. */
    private fun daysOf(theme: ChallengeTheme, count: Int = 400) =
        days(count).filter { generator.themeFor(it) == theme }

    /** The numbers printed in a step's text, in the order they appear. */
    private fun numbersIn(text: String): List<Int> =
        Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()

    /** The same, minus the leading step number every step opens with. */
    private fun argsIn(text: String): List<Int> = numbersIn(text).drop(1)

    // ── True of every chain, whatever the story ──────────────────────────

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

    /**
     * The app promises a plain number keypad with no minus sign, so every
     * theme's last step is a place the chain could break that and must
     * not, however the draws land.
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

    /**
     * The text deliberately never restates an earlier answer, which is
     * what makes this a memory exercise. That only works if the first
     * hint hands the value back: forgetting a number is not a maths
     * failure, and the challenge never reveals, so without this a slip
     * ends the whole day.
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
                    "$date $language the finale recalls neither W nor the opening value",
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
    fun `the finale does not print the opening value it asks her to remember`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            assertTrue(
                "$date printed the opening value in '${stages[4].text}'",
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
     * ("it never contains the answer") turned out to be the wrong claim:
     * a sheet that lists the primes to thirty contains 2 and 3, and step
     * 4's answer is a small number, so the two collide constantly without
     * anything being given away. What actually matters is that no value
     * from this chain can reach the sheet, and a sheet that never varies
     * cannot carry one. Now compared WITHIN a theme, since a market stall
     * and a clock face teach different ideas.
     */
    @Test
    fun `the helper sheet carries nothing from today's chain`() {
        for (theme in ChallengeTheme.entries) {
            for (language in AppLanguage.entries) {
                val dates = daysOf(theme)
                val fixed = chain(dates.first(), language).stages.map { it.notes }
                for (date in dates) {
                    assertEquals(
                        "$theme $date $language",
                        fixed,
                        chain(date, language).stages.map { it.notes },
                    )
                }
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
    }

    // ── The themes themselves ────────────────────────────────────────────

    /**
     * Every theme has to actually turn up, and none of them may crowd the
     * others out. Four themes over sixty days is fifteen each on average;
     * anything under five is a rota that has gone wrong.
     */
    @Test
    fun `all four themes are dealt, and none of them dominates`() {
        val dealt = days(60).map(generator::themeFor)
        for (theme in ChallengeTheme.entries) {
            val count = dealt.count { it == theme }
            assertTrue("$theme came up only $count times in 60 days", count >= 5)
        }
    }

    /** The story is fixed by the date, like everything else about the day. */
    @Test
    fun `the theme is settled by the date alone`() {
        for (date in days(100)) {
            assertEquals("$date", generator.themeFor(date), DailyChallengeGenerator().themeFor(date))
        }
    }

    /**
     * Two chains told through different stories should not read as the
     * same five sentences with the nouns swapped.
     */
    @Test
    fun `each theme opens with its own words`() {
        val intros = ChallengeTheme.entries.map { theme ->
            chain(daysOf(theme).first()).intro
        }
        assertEquals("two themes share an intro", intros.size, intros.distinct().size)
    }

    // ── Theme: the anchor ────────────────────────────────────────────────

    /**
     * The hunt must have exactly ONE answer, or the step is unfair in a
     * way she cannot argue with: the app would reject a number that
     * satisfies everything it asked for.
     */
    @Test
    fun `the anchor is the only number in its window that fits`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stage = chain(date).stages[0]
            val (low, high, a, b) = argsIn(stage.text)
            val fitting = (low..high).filter { it % a == 0 && it % b == 0 }
            assertEquals("$date window $low..$high for $a and $b", listOf(stage.answer), fitting)
        }
    }

    @Test
    fun `the anchor is a moderate double-digit number`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val anchor = chain(date).stages[0].answer
            assertTrue("$date anchored at $anchor", anchor in 10..99)
        }
    }

    @Test
    fun `the remainder is the anchor multiplied then divided`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val (multiplier, divisor) = argsIn(stages[1].text)
            assertEquals("$date", stages[0].answer * multiplier % divisor, stages[1].answer)
        }
    }

    /**
     * The draw's guarantee rather than arithmetic: never a zero divisor,
     * and never a clean division either. A remainder of nothing would
     * leave step 3 shifting a zero and turn this step into a yes-or-no.
     */
    @Test
    fun `the divisor never divides by zero and never divides exactly`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val divisor = argsIn(stages[1].text)[1]
            assertTrue("$date divided by $divisor", divisor > 0)
            assertTrue("$date left no remainder", stages[1].answer > 0)
            assertTrue("$date remainder was $divisor or more", stages[1].answer < divisor)
        }
    }

    @Test
    fun `the digit sum follows from the remainder and the shift`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val shifted = stages[1].answer + argsIn(stages[2].text)[0]
            assertEquals("$date shifted to $shifted", shifted / 10 + shifted % 10, stages[2].answer)
        }
    }

    /** The step says "its two digits", so it had better always have two. */
    @Test
    fun `the shifted value always has exactly two digits`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val shifted = stages[1].answer + argsIn(stages[2].text)[0]
            assertTrue("$date shifted to $shifted", shifted in 10..99)
        }
    }

    @Test
    fun `the gap reaches the first prime above the digit sum`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val digitSum = stages[2].answer
            val prime = generateSequence(digitSum + 1) { it + 1 }.first(::isPrime)
            assertEquals("$date reaching up from $digitSum", prime - digitSum, stages[3].answer)
            assertTrue("$date the gap was zero", stages[3].answer >= 1)
        }
    }

    @Test
    fun `the anchor finale multiplies the gap by the anchor from step one`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", stages[3].answer * stages[0].answer - tail, stages[4].answer)
        }
    }

    // ── Theme: the clock ─────────────────────────────────────────────────

    @Test
    fun `the journey is the minutes between the two printed times`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stage = chain(date).stages[0]
            val (departHour, departMinute, arriveHour, arriveMinute) = argsIn(stage.text)
            val depart = departHour * 60 + departMinute
            val arrive = arriveHour * 60 + arriveMinute
            assertEquals("$date", arrive - depart, stage.answer)
            assertTrue("$date arrived before it left", arrive > depart)
        }
    }

    @Test
    fun `the spare minutes are what the whole hours leave behind`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stages = chain(date).stages
            assertEquals("$date", stages[0].answer % 60, stages[1].answer)
            // Five or more, or step 3 has no whole block to find.
            assertTrue("$date only ${stages[1].answer} spare", stages[1].answer >= 5)
        }
    }

    @Test
    fun `the blocks are whole fives inside the spare minutes`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stages = chain(date).stages
            assertEquals("$date", stages[1].answer / 5, stages[2].answer)
            assertTrue("$date found no blocks", stages[2].answer >= 1)
        }
    }

    @Test
    fun `the stops cost one lot per block`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stages = chain(date).stages
            val perStop = argsIn(stages[3].text).first()
            assertEquals("$date", stages[2].answer * perStop, stages[3].answer)
        }
    }

    @Test
    fun `the clock finale adds the stops to the journey from step one`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stages = chain(date).stages
            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", stages[0].answer + stages[3].answer - tail, stages[4].answer)
        }
    }

    // ── Theme: the market ────────────────────────────────────────────────

    @Test
    fun `the stock is what the crates brought in less what sold`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stage = chain(date).stages[0]
            val (perCrate, crates, sold) = argsIn(stage.text)
            assertEquals("$date", perCrate * crates - sold, stage.answer)
            assertTrue("$date left only ${stage.answer}", stage.answer >= 20)
        }
    }

    @Test
    fun `the loose ones are what the full bags leave behind`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stages = chain(date).stages
            val bag = argsIn(stages[1].text).first()
            assertEquals("$date", stages[0].answer % bag, stages[1].answer)
            assertTrue("$date bagged everything", stages[1].answer >= 1)
        }
    }

    @Test
    fun `the loose ones are worth their count times the price`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stages = chain(date).stages
            val price = argsIn(stages[2].text).first()
            assertEquals("$date", stages[1].answer * price, stages[2].answer)
        }
    }

    /** Change from a note, so the note has to cover the bill. */
    @Test
    fun `the change is the note less the bill and never negative`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stages = chain(date).stages
            val note = argsIn(stages[3].text).first()
            assertTrue("$date paid $note for a bill of ${stages[2].answer}", note > stages[2].answer)
            assertEquals("$date", note - stages[2].answer, stages[3].answer)
        }
    }

    @Test
    fun `the market finale multiplies the change by the stock from step one`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stages = chain(date).stages
            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", stages[3].answer * stages[0].answer - tail, stages[4].answer)
        }
    }

    // ── Theme: the workshop ──────────────────────────────────────────────

    @Test
    fun `the border goes twice round both sides`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stage = chain(date).stages[0]
            val (length, width) = argsIn(stage.text)
            assertEquals("$date", 2 * (length + width), stage.answer)
            assertNotEquals("$date the rug was square", length, width)
        }
    }

    /**
     * The square's side has to come out whole. It is the one place this
     * chain could need a decimal point, and the keypad has none.
     */
    @Test
    fun `the square side divides the border exactly by four`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stages = chain(date).stages
            assertEquals("$date border ${stages[0].answer} is not a multiple of 4",
                0, stages[0].answer % 4)
            assertEquals("$date", stages[0].answer / 4, stages[1].answer)
        }
    }

    @Test
    fun `the area is the side times itself`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stages = chain(date).stages
            assertEquals("$date", stages[1].answer * stages[1].answer, stages[2].answer)
        }
    }

    @Test
    fun `the hundreds are whole hundreds inside the area`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stages = chain(date).stages
            assertEquals("$date", stages[2].answer / 100, stages[3].answer)
            assertTrue("$date found no whole hundreds", stages[3].answer >= 1)
        }
    }

    @Test
    fun `the workshop finale multiplies the hundreds by the border from step one`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stages = chain(date).stages
            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", stages[3].answer * stages[0].answer - tail, stages[4].answer)
        }
    }

    private fun isPrime(n: Int): Boolean = n >= 2 && (2..n / 2).none { n % it == 0 }

    /** So a four-number header can be destructured in one line above. */
    private operator fun <T> List<T>.component4(): T = this[3]
}
