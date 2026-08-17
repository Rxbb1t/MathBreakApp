package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * Which story today's chain is told through.
 *
 * The five steps were the same five roles every single day before this:
 * hunt a multiple, take a remainder, sum the digits, reach for a prime,
 * multiply back. Only the numbers moved, so a week of it and she has
 * learned the shape rather than the arithmetic.
 *
 * A theme changes what the chain is ABOUT. The rules it has to obey do
 * not change at all, and they are the reason this is an interface rather
 * than four loose functions:
 *
 *  - exactly five steps, each one's answer feeding the next;
 *  - every answer a whole number that is never negative, because the
 *    keypad has no minus sign and no decimal point;
 *  - the text never restates an earlier answer, and the first hint of
 *    every step always hands it back, because forgetting a number is not
 *    a maths failure and the challenge never reveals;
 *  - the last step reaches back to the first, so the opening value has to
 *    be carried the whole way.
 */
enum class ChallengeTheme(internal val chain: ChallengeChain) {
    /** The original: hunt the number everything else hangs off. */
    ANCHOR(AnchorChain),

    /** Journeys, and what is left over when hours are taken out. */
    CLOCK(ClockChain),

    /** Stock, bags, prices and change. */
    MARKET(MarketChain),

    /** Rugs and mats: distance round, then area. */
    WORKSHOP(WorkshopChain),
    ;

    companion object {
        /**
         * Today's theme.
         *
         * Drawn from the same stream as everything else, so a date still
         * decides the whole day and both languages still agree.
         */
        fun of(random: Random): ChallengeTheme = entries.random(random)
    }
}

/** Builds one day's five-step chain in one theme. */
internal interface ChallengeChain {
    fun build(random: Random, language: AppLanguage): DailyChallenge
}

/**
 * One step of a chain, already drawn but not yet worded.
 *
 * Held as functions of the language rather than as finished strings so
 * that the same drawn step can be asked in either language without being
 * drawn twice, which is what keeps both languages on identical numbers.
 */
internal class ChainStep(
    val answer: Int,
    private val text: (AppLanguage) -> String,
    private val hint: (AppLanguage) -> String,
    private val notes: (AppLanguage) -> List<String>,
) {
    fun problem(language: AppLanguage): Problem =
        stage(text(language), answer, hint(language), notes(language), language)
}

/**
 * A step that might or might not suit the value handed to it.
 *
 * Returns null when it cannot work on that input: "how many whole
 * hundreds fit inside this" has nothing to say about 40, and "add the
 * two figures on the display" has nothing to say about a single digit.
 * Refusing is how each variant states its own domain, instead of the
 * caller having to know all of them.
 */
internal typealias StepSpec = (input: Int, random: Random) -> ChainStep?

/**
 * Picks one of [pool] that will work on [input].
 *
 * Every pool must hold at least one variant that accepts anything its
 * slot can be handed, so this can always find one. That is a rule about
 * how the pools are written, and [DailyChallengeGeneratorTest] sweeps
 * enough days to hold it: if a pool ever fails to produce a step, the
 * chain cannot be built at all and the test says so loudly.
 *
 * Shuffled rather than indexed so that adding a variant reshuffles which
 * day gets which shape, instead of shifting every later day by one.
 */
internal fun pick(pool: List<StepSpec>, input: Int, random: Random): ChainStep =
    pool.shuffled(random).firstNotNullOf { it(input, random) }

/**
 * One step, with the two hints and the helper sheet every step carries.
 *
 * The second hint is always the digit count, which narrows without
 * telling: knowing the answer has two digits is a real nudge and gives
 * nothing away about which two.
 */
internal fun stage(
    text: String,
    answer: Int,
    hint: String,
    notes: List<String>,
    language: AppLanguage,
): Problem = Problem(
    text = text,
    answer = answer,
    level = DailyChallengeGenerator.STAGE_LEVEL,
    kind = ProblemKind.LOGIC,
    hints = listOf(hint, HintText.digits(answer, language)),
    notes = notes,
)

internal fun en(language: AppLanguage): Boolean = language == AppLanguage.ENGLISH

/**
 * A Romanian numeral with "de" in front of the noun where the language
 * wants one: "12 mere" but "45 de mere".
 *
 * Same helper the other generators carry. Every chain that prints a
 * counted noun in Romanian has to draw its numbers so this is actually
 * right, because the rule really follows the last two digits: 105 is
 * "105 minute" with no "de" at all. The draws below stay out of that
 * band rather than the helper trying to be clever about it.
 */
internal fun de(n: Int): String = if (n < 20) "$n" else "$n de"

/** [n] divided by [by], rounded up, without leaving the integers. */
internal fun ceilingDivide(n: Int, by: Int): Int = (n + by - 1) / by

/** Smallest number both [a] and [b] divide, by way of their gcd. */
internal fun leastCommonMultiple(a: Int, b: Int): Int =
    a / greatestCommonDivisor(a, b) * b

internal tailrec fun greatestCommonDivisor(a: Int, b: Int): Int =
    if (b == 0) a else greatestCommonDivisor(b, a % b)

/** The digits of [n] added together. */
internal fun digitsOf(n: Int): Int {
    var left = n
    var total = 0
    while (left > 0) {
        total += left % 10
        left /= 10
    }
    return total
}

/**
 * The first prime strictly above [n].
 *
 * Computed rather than looked up in a table, so a step that hands it a
 * larger number than its author expected still gets the right answer
 * instead of running off the end of a list.
 */
internal fun nextPrimeAbove(n: Int): Int =
    generateSequence(n + 1) { it + 1 }.first(::isPrime)

/** How many whole numbers divide [n] exactly, counting 1 and [n]. */
internal fun divisorCount(n: Int): Int = (1..n).count { n % it == 0 }

private fun isPrime(n: Int): Boolean = n >= 2 && (2..n / 2).none { n % it == 0 }

/** A time of day as HH:MM, from minutes since midnight. */
internal fun clockFace(minutesOfDay: Int): String {
    val hour = minutesOfDay / 60
    val minute = minutesOfDay % 60
    return "%d:%02d".format(hour, minute)
}

// ── Helper sheets shared between themes ──────────────────────────────────
//
// Worked on numbers no chain uses, so a sheet teaches the idea without
// quietly handing over today's answer. Shared where the idea is shared:
// a remainder is a remainder whether it came out of a clock or a crate.

internal fun remainderNotes(language: AppLanguage): List<String> =
    if (en(language)) {
        listOf(
            "The remainder is what will not fit into a whole group. " +
                "Share 23 between 5 and each gets 4, with 3 sitting there spare: the remainder is 3.",
            "It is always smaller than what you divided by. Dividing by 7 can only " +
                "ever leave 0 through 6.",
            "A quick way in: take away the biggest multiple you can, and read what is left.",
        )
    } else {
        listOf(
            "Restul e ce nu încape într-o grupă întreagă. " +
                "Împarți 23 la 5 și fiecare ia 4, iar 3 rămân pe dinafară: restul e 3.",
            "E întotdeauna mai mic decât numărul la care ai împărțit. La împărțirea la 7 " +
                "pot rămâne doar 0 până la 6.",
            "O cale scurtă: scoate cel mai mare multiplu care încape și citește ce rămâne.",
        )
    }

internal fun groupingNotes(language: AppLanguage): List<String> =
    if (en(language)) {
        listOf(
            "Whole groups only: 38 shared into groups of 8 makes 4 groups, " +
                "and the 6 left over are not a group.",
            "Counting the groups and counting the leftovers are two different questions. " +
                "Read which one is being asked.",
            "The leftover is always smaller than the group size, or another whole group would fit.",
        )
    } else {
        listOf(
            "Doar grupe întregi: dacă împarți 38 în grupe de câte 8, ies 4 grupe, " +
                "iar cele 6 rămase nu fac o grupă.",
            "Câte grupe ies și cât rămâne pe dinafară sunt două întrebări diferite. " +
                "Citește care dintre ele se cere.",
            "Ce rămâne e mereu mai mic decât grupa, altfel ar mai încăpea o grupă întreagă.",
        )
    }

internal fun digitNotes(language: AppLanguage): List<String> =
    if (en(language)) {
        listOf(
            "A digit sum ignores what the number is worth and just adds the figures: " +
                "52 becomes 5 + 2 = 7.",
            "Because it throws the place value away, a digit sum is always small. " +
                "Two digits can never add up past 18.",
        )
    } else {
        listOf(
            "Suma cifrelor nu ține cont de cât valorează numărul, ci doar adună figurile. " +
                "Suma cifrelor lui 52: 5 + 2 = 7.",
            "Fiindcă lasă deoparte valoarea de poziție, suma cifrelor e mereu mică. " +
                "Două cifre nu pot depăși 18.",
        )
    }

internal fun chainNotes(language: AppLanguage): List<String> =
    if (en(language)) {
        listOf(
            "This last one reaches all the way back to the number you started with, " +
                "not to the step just before it.",
            "Multiply first, take away second. The order is the whole difference.",
        )
    } else {
        listOf(
            "Ultimul pas se întoarce tocmai la numărul de la început, " +
                "nu la pasul dinaintea lui.",
            "Întâi înmulțești, apoi scazi. Ordinea schimbă tot.",
        )
    }
