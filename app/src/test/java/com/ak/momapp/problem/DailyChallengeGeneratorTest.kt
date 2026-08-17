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
 * whatever story it is telling. The second checks each theme's own
 * arithmetic on the days that theme is dealt, and since the middle three
 * steps are now drawn from pools, it first works out WHICH variant was
 * dealt by reading the English wording, then re-derives that variant.
 * A variant whose wording changes without its test changing will fail
 * loudly here rather than quietly stop being checked.
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

    /**
     * Fails rather than passes when a step's wording matches no known
     * variant, so a new variant cannot slip in untested.
     */
    private fun unknown(step: Problem): Nothing =
        throw AssertionError("no test knows this variant: '${step.text}'")

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
     * No step may hand the next one a value it cannot work with. A zero
     * or a one arriving mid-chain is how a pool runs out of applicable
     * variants, and it is also how a step stops being a question.
     */
    @Test
    fun `every middle step leaves something for the next one to work on`() {
        for (date in days(400)) {
            val stages = chain(date).stages
            for (step in 1..3) {
                assertTrue(
                    "$date step ${step + 1} answered ${stages[step].answer}",
                    stages[step].answer >= 1,
                )
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
     * Written as "the same sheet always comes with the same question"
     * after the obvious version ("it never contains the answer") turned
     * out to be the wrong claim: a sheet that lists the primes to thirty
     * contains 2 and 3, and a step's answer is a small number, so the two
     * collide constantly without anything being given away. What actually
     * matters is that no value from today's chain can reach the sheet,
     * and a sheet that never varies with the numbers cannot carry one.
     */
    @Test
    fun `the helper sheet carries nothing from today's chain`() {
        for (language in AppLanguage.entries) {
            // Keyed on the question's shape rather than the day: two days
            // dealing the same variant must bring the identical sheet,
            // whatever numbers landed in it.
            val sheets = mutableMapOf<String, List<String>>()
            for (date in days(400)) {
                for (stage in chain(date, language).stages) {
                    val shape = stage.text.replace(Regex("\\d+"), "#")
                    val seen = sheets.putIfAbsent(shape, stage.notes)
                    if (seen != null) {
                        assertEquals("$date $language '$shape'", seen, stage.notes)
                    }
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

    // ── The themes and their shapes ──────────────────────────────────────

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

    @Test
    fun `each theme opens with its own words`() {
        val intros = ChallengeTheme.entries.map { chain(daysOf(it).first()).intro }
        assertEquals("two themes share an intro", intros.size, intros.distinct().size)
    }

    /**
     * The whole point of the pools: a theme must not always ask the same
     * five questions. Eight shapes per theme are possible; over a year of
     * that theme's days most of them should show up, and certainly more
     * than one.
     */
    @Test
    fun `every theme asks more than one shape of question`() {
        for (theme in ChallengeTheme.entries) {
            val shapes = daysOf(theme)
                .map { date ->
                    chain(date).stages.joinToString("|") { it.text.replace(Regex("\\d+"), "#") }
                }
                .distinct()
            assertTrue("$theme only ever asks ${shapes.size} shape(s)", shapes.size >= 4)
        }
    }

    /**
     * And every individual variant has to be reachable. A variant that
     * never fires is dead code that still has to be read and maintained,
     * and it usually means its own guard rejects everything.
     */
    @Test
    fun `every written variant is dealt at least once`() {
        val seen = days(400).flatMap { date ->
            chain(date).stages.map { it.text.replace(Regex("\\d+"), "#") }
        }.toSet()
        // Two openings and two finales are fixed, plus three pools of two
        // in each of four themes: 4 * (1 + 2 + 2 + 2 + 1) = 32 shapes.
        assertEquals("some variant never came up:\n" + seen.sorted().joinToString("\n"), 32, seen.size)
    }

    // ── Theme: the anchor ────────────────────────────────────────────────

    @Test
    fun `the anchor chain adds up`() {
        for (date in daysOf(ChallengeTheme.ANCHOR)) {
            val stages = chain(date).stages
            val anchor = stages[0].answer

            // Step 1: the hunt must have exactly ONE answer, or the step
            // is unfair in a way she cannot argue with.
            val (low, high, a, b) = argsIn(stages[0].text)
            assertEquals(
                "$date window $low..$high for $a and $b",
                listOf(anchor),
                (low..high).filter { it % a == 0 && it % b == 0 },
            )
            assertTrue("$date anchored at $anchor", anchor in 10..99)

            val y = stages[1].answer
            when {
                "keep only the remainder" in stages[1].text -> {
                    val (multiplier, divisor) = argsIn(stages[1].text)
                    assertEquals("$date", anchor * multiplier % divisor, y)
                    // Never a clean division: a remainder of nothing would
                    // turn the step into a yes-or-no.
                    assertTrue("$date left no remainder", y in 1 until divisor)
                }
                "next whole ten" in stages[1].text ->
                    assertEquals("$date", 10 - anchor % 10, y)
                else -> unknown(stages[1])
            }

            val z = stages[2].answer
            when {
                "digit sum" in stages[2].text -> {
                    val shifted = y + argsIn(stages[2].text).first()
                    assertTrue("$date shifted to $shifted", shifted in 10..99)
                    assertEquals("$date", shifted / 10 + shifted % 10, z)
                }
                "Double Y" in stages[2].text ->
                    assertEquals("$date", 2 * y + argsIn(stages[2].text).first(), z)
                else -> unknown(stages[2])
            }

            val w = stages[3].answer
            when {
                "smallest prime" in stages[3].text -> {
                    val prime = generateSequence(z + 1) { it + 1 }.first(::isPrime)
                    assertEquals("$date reaching up from $z", prime - z, w)
                }
                "divide Z exactly" in stages[3].text ->
                    assertEquals("$date", (1..z).count { z % it == 0 }, w)
                else -> unknown(stages[3])
            }

            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", w * anchor - tail, stages[4].answer)
        }
    }

    // ── Theme: the clock ─────────────────────────────────────────────────

    @Test
    fun `the clock chain adds up`() {
        for (date in daysOf(ChallengeTheme.CLOCK)) {
            val stages = chain(date).stages
            val trip = stages[0].answer

            val (departHour, departMinute, arriveHour, arriveMinute) = argsIn(stages[0].text)
            val depart = departHour * 60 + departMinute
            val arrive = arriveHour * 60 + arriveMinute
            assertEquals("$date", arrive - depart, trip)
            assertTrue("$date arrived before it left", arrive > depart)

            val y = stages[1].answer
            when {
                "spare minutes" in stages[1].text -> assertEquals("$date", trip % 60, y)
                "fall short" in stages[1].text ->
                    assertEquals("$date", argsIn(stages[1].text).first() - trip, y)
                else -> unknown(stages[1])
            }
            // Whatever the variant, it has to leave a workable number of
            // minutes: under five and the block step finds nothing.
            assertTrue("$date only $y minutes", y in 5..59)

            val z = stages[2].answer
            when {
                "five-minute blocks" in stages[2].text -> assertEquals("$date", y / 5, z)
                "two figures" in stages[2].text -> {
                    assertTrue("$date asked for two figures of $y", y >= 10)
                    assertEquals("$date", y / 10 + y % 10, z)
                }
                else -> unknown(stages[2])
            }

            val w = stages[3].answer
            when {
                "one stop for every block" in stages[3].text ->
                    assertEquals("$date", z * argsIn(stages[3].text).first(), w)
                "return trip" in stages[3].text ->
                    assertEquals("$date", 2 * z + argsIn(stages[3].text).first(), w)
                else -> unknown(stages[3])
            }

            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", trip + w - tail, stages[4].answer)
        }
    }

    // ── Theme: the market ────────────────────────────────────────────────

    @Test
    fun `the market chain adds up`() {
        for (date in daysOf(ChallengeTheme.MARKET)) {
            val stages = chain(date).stages

            val (perCrate, crates, sold) = argsIn(stages[0].text)
            val stock = stages[0].answer
            assertEquals("$date", perCrate * crates - sold, stock)
            // The finale multiplies the stock and then subtracts, so a
            // tiny stock is the one way it could go negative.
            assertTrue("$date left only $stock", stock >= 20)

            val y = stages[1].answer
            val bag = argsIn(stages[1].text).first()
            when {
                "still loose" in stages[1].text -> {
                    assertEquals("$date", stock % bag, y)
                    assertTrue("$date bagged everything", y >= 1)
                }
                "full bags does that make" in stages[1].text ->
                    assertEquals("$date", stock / bag, y)
                else -> unknown(stages[1])
            }

            val z = stages[2].answer
            when {
                "carried home" in stages[2].text -> {
                    val (price, carriage) = argsIn(stages[2].text)
                    assertEquals("$date", y * price + carriage, z)
                }
                "sells for" in stages[2].text ->
                    assertEquals("$date", y * argsIn(stages[2].text).first(), z)
                else -> unknown(stages[2])
            }

            val w = stages[3].answer
            when {
                "pays with a note" in stages[3].text -> {
                    val note = argsIn(stages[3].text).first()
                    assertTrue("$date paid $note for a bill of $z", note > z)
                    assertEquals("$date", note - z, w)
                }
                "ten-euro notes" in stages[3].text -> {
                    assertTrue("$date counted tens out of $z", z >= 10)
                    assertEquals("$date", z / 10, w)
                }
                else -> unknown(stages[3])
            }

            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", w * stock - tail, stages[4].answer)
        }
    }

    // ── Theme: the workshop ──────────────────────────────────────────────

    @Test
    fun `the workshop chain adds up`() {
        for (date in daysOf(ChallengeTheme.WORKSHOP)) {
            val stages = chain(date).stages

            val (length, width) = argsIn(stages[0].text)
            val border = stages[0].answer
            assertEquals("$date", 2 * (length + width), border)
            assertNotEquals("$date the rug was square", length, width)

            val y = stages[1].answer
            when {
                "same distance round" in stages[1].text -> {
                    // The one place this chain could need a decimal point,
                    // and the keypad has none.
                    assertEquals("$date border $border is not a multiple of 4", 0, border % 4)
                    assertEquals("$date", border / 4, y)
                }
                "Half the Border" in stages[1].text -> assertEquals("$date", border / 2, y)
                else -> unknown(stages[1])
            }

            val z = stages[2].answer
            when {
                "A square is cut" in stages[2].text -> assertEquals("$date", y * y, z)
                "A runner is cut" in stages[2].text ->
                    assertEquals("$date", y * argsIn(stages[2].text).first(), z)
                else -> unknown(stages[2])
            }
            // Squaring grows fast; the bounds exist to keep the finale
            // something she can still multiply by hand.
            assertTrue("$date area of $z", z in 100..2600)

            val w = stages[3].answer
            when {
                "whole hundreds" in stages[3].text -> assertEquals("$date", z / 100, w)
                "Tiles of" in stages[3].text -> {
                    val tile = argsIn(stages[3].text).first()
                    assertTrue("$date tiled $z with $tile", tile <= z)
                    assertEquals("$date", z / tile, w)
                }
                else -> unknown(stages[3])
            }

            val tail = numbersIn(stages[4].text).last()
            assertEquals("$date", w * border - tail, stages[4].answer)
        }
    }

    private fun isPrime(n: Int): Boolean = n >= 2 && (2..n / 2).none { n % it == 0 }

    /** So a four-number header can be destructured in one line above. */
    private operator fun <T> List<T>.component4(): T = this[3]
}
