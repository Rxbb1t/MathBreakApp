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
