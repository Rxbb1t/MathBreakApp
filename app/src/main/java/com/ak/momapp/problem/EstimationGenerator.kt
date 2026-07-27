package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * "Roughly how much is 297 × 4?"
 *
 * The one exercise in the app where being exactly right is not the point.
 * Every other kind wants the number; this one wants the ability to round
 * something awkward into something you can do in your head and know roughly
 * where the answer lands. That is the arithmetic people actually use in a
 * shop, and it is a different skill from long multiplication.
 *
 * The numbers are chosen to be ROUNDABLE BUT NOT ROUND: 297 and 41 are the
 * point, 300 and 40 would be the answer already. Everything sits just off a
 * power of ten so there is a decision to make about which way to go.
 */
class EstimationGenerator(private val random: Random) {

    private enum class Shape { SUM, PRODUCT, DIFFERENCE, QUOTIENT }

    fun generate(level: Level, language: AppLanguage): Problem {
        // Adding is the friendliest thing to approximate and never stops
        // being worth asking. Taking away arrives soon after. Division is
        // last on purpose: with a single-digit divisor "608 ÷ 8" is a times
        // table rather than an estimate, so it only becomes a real exercise
        // once the divisor has two digits of its own.
        val shape = random.pickWeighted(
            listOf(
                Shape.SUM to 1.0,
                Shape.PRODUCT to 1.0,
                Shape.DIFFERENCE to level.ramp(Level.EASY_TOP - 10, Level.MEDIUM_ANCHOR),
                Shape.QUOTIENT to level.ramp(Level.MEDIUM_ANCHOR, Level.HARD_ANCHOR),
            ),
        )
        return when (shape) {
            Shape.SUM -> sum(level, language)
            Shape.PRODUCT -> product(level, language)
            Shape.DIFFERENCE -> difference(level, language)
            Shape.QUOTIENT -> quotient(level, language)
        }
    }

    private fun sum(level: Level, language: AppLanguage): Problem {
        val terms = List(3) { offRound(random.nextInt(level.span(21..99, 101..999, 1001..9999))) }
        val answer = terms.sum()
        val unit = roundingUnit(terms.max())
        val rounded = terms.map { roundTo(it, unit) }
        return build(
            level = level,
            language = language,
            question = terms.joinToString(" + "),
            answer = answer,
            roundedText = rounded.joinToString(" + "),
            estimate = rounded.sum(),
            nudge = when (language) {
                AppLanguage.ENGLISH ->
                    "Take each number to the nearest ${unitWord(unit, language)} first, then add those."
                AppLanguage.ROMANIAN ->
                    "Du fiecare număr la ${unitWord(unit, language)} cea mai apropiată, apoi adună-le."
            },
        )
    }

    private fun difference(level: Level, language: AppLanguage): Problem {
        val big = offRound(random.nextInt(level.span(51..99, 401..999, 4001..9999)))
        val small = offRound(random.nextInt(level.span(11..40, 101..390, 1001..3900)))
        val answer = big - small
        val unit = roundingUnit(big)
        return build(
            level = level,
            language = language,
            question = "$big − $small",
            answer = answer,
            roundedText = "${roundTo(big, unit)} − ${roundTo(small, unit)}",
            estimate = roundTo(big, unit) - roundTo(small, unit),
            nudge = when (language) {
                AppLanguage.ENGLISH ->
                    "Round both to the nearest ${unitWord(unit, language)}. The gap between them barely moves."
                AppLanguage.ROMANIAN ->
                    "Rotunjește-le pe amândouă la ${unitWord(unit, language)} cea mai apropiată. Diferența dintre ele abia se schimbă."
            },
        )
    }

    private fun product(level: Level, language: AppLanguage): Problem {
        // Kept deliberately smaller than the other shapes. A product grows
        // by multiplying, so the same operands that make a harmless sum run
        // straight past the app's answer caps: Normal answers stop at 3500
        // and the input field stops at five digits.
        val big = offRound(random.nextInt(level.span(21..99, 101..380, 1001..4999)))
        val small = random.nextInt(level.span(3..9, 3..9, 4..9))
        val answer = big * small
        val unit = roundingUnit(big)
        val roundedBig = roundTo(big, unit)
        return build(
            level = level,
            language = language,
            question = "$big × $small",
            answer = answer,
            roundedText = "$roundedBig × $small",
            estimate = roundedBig * small,
            nudge = when (language) {
                AppLanguage.ENGLISH ->
                    "$big is nearly $roundedBig. Multiply that instead and you are close."
                AppLanguage.ROMANIAN ->
                    "$big e aproape $roundedBig. Înmulțește-l pe acela și ești aproape."
            },
        )
    }

    private fun quotient(level: Level, language: AppLanguage): Problem {
        // Built backwards from a whole answer so the exact result is clean,
        // while the numbers on screen give nothing away: 608 ÷ 19 does not
        // look divisible, which is what makes rounding worth doing.
        val divisor = offRound(random.nextInt(level.span(11..19, 11..29, 21..49)))
        val answer = random.nextInt(level.span(3..12, 11..49, 21..99))
        val total = divisor * answer
        val unit = roundingUnit(divisor)
        val roundedDivisor = roundTo(divisor, unit)
        return build(
            level = level,
            language = language,
            question = "$total ÷ $divisor",
            answer = answer,
            roundedText = "$total ÷ $roundedDivisor",
            estimate = total / roundedDivisor,
            nudge = when (language) {
                AppLanguage.ENGLISH ->
                    "$divisor is nearly $roundedDivisor. How many of those fit into $total?"
                AppLanguage.ROMANIAN ->
                    "$divisor e aproape $roundedDivisor. Câte de acestea încap în $total?"
            },
        )
    }

    /**
     * Assembles the problem once a shape has picked its numbers.
     *
     * [estimate] is what the rounding shortcut actually lands on, and it is
     * computed rather than described, so the worked solution cannot claim a
     * shortcut that does not arrive where it says it does.
     */
    private fun build(
        level: Level,
        language: AppLanguage,
        question: String,
        answer: Int,
        roundedText: String,
        estimate: Int,
        nudge: String,
    ): Problem {
        val tolerance = toleranceFor(answer, estimate, level)
        val text = when (language) {
            AppLanguage.ENGLISH -> "Roughly how much is $question?"
            AppLanguage.ROMANIAN -> "Cam cât face $question?"
        }
        return Problem(
            text = text,
            answer = answer,
            level = level,
            kind = ProblemKind.ESTIMATE,
            tolerance = tolerance,
            hints = listOf(nudge, HintText.digits(answer, language)),
            notes = notesFor(language),
            solution = solutionFor(question, roundedText, estimate, answer, language),
        )
    }

    /**
     * How far off still counts.
     *
     * Two things decide it, and the second is the important one:
     *
     * A SHARE OF THE ANSWER, tightening as she climbs. A fixed amount would
     * be meaningless across this range, since the same ten points is the
     * whole answer at Easy and a rounding error at Hard. The floor of one
     * keeps a tiny answer from demanding the exact number under the guise
     * of an estimate.
     *
     * AT LEAST THE ROUNDING ERROR ITSELF. The app tells her to round, and
     * the worked solution shows her rounding; if the number that method
     * arrives at could still be marked wrong, the exercise is teaching a
     * technique it then punishes. Measured before this was added, the
     * shortcut failed about one Hard problem in five. Taking the maximum
     * makes it impossible BY CONSTRUCTION rather than by choosing a
     * percentage generous enough to usually cover it.
     */
    private fun toleranceFor(answer: Int, estimate: Int, level: Level): Int {
        val percent = level.between(20, 15, 10)
        return maxOf(1, abs(answer) * percent / 100, abs(answer - estimate))
    }

    /**
     * Nudges a number off a round one.
     *
     * Without this the roll occasionally hands her "300 × 4", where there is
     * nothing to estimate because the rounding has already happened. Shifting
     * by a few keeps every question one that has to be approximated.
     */
    private fun offRound(n: Int): Int {
        val unit = roundingUnit(n)
        if (n % unit == 0) {
            val nudge = random.nextInt(2, 1 + unit / 4).let { if (random.nextBoolean()) it else -it }
            return max(2, n + nudge)
        }
        return n
    }

    /**
     * The place worth rounding to, which is TWO SIGNIFICANT FIGURES rather
     * than the leading power of ten.
     *
     * Rounding to the leading power looks tidier and is badly wrong: it
     * turns 1499 into 1000, a third of the way off, and an exercise whose
     * own advice is that far out is not teaching estimation. Two figures
     * keeps every number within about five percent of itself, which is what
     * makes the shortcut worth using.
     */
    private fun roundingUnit(n: Int): Int = when {
        n >= 10_000 -> 1_000
        n >= 1_000 -> 100
        else -> 10
    }

    private fun roundTo(n: Int, unit: Int): Int = ((n + unit / 2) / unit) * unit

    private fun unitWord(unit: Int, language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> when (unit) {
            1_000 -> "thousand"
            100 -> "hundred"
            else -> "ten"
        }
        AppLanguage.ROMANIAN -> when (unit) {
            1_000 -> "mie"
            100 -> "sută"
            else -> "zece"
        }
    }

    /**
     * The shortcut first, then the true number.
     *
     * Both halves matter. The shortcut is the skill being taught, and the
     * exact value is what tells her how good the shortcut was, which is the
     * only way the estimate stops feeling like a guess. The last step ends
     * on the answer, as every worked solution in the app must.
     */
    private fun solutionFor(
        question: String,
        roundedText: String,
        estimate: Int,
        answer: Int,
        language: AppLanguage,
    ): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Rounded, that is $roundedText, which comes to $estimate.",
            "Worked out exactly, $question = $answer.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Rotunjit, asta înseamnă $roundedText, adică $estimate.",
            "Calculat exact, $question = $answer.",
        )
    }

    private fun notesFor(language: AppLanguage): List<String> = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "Rounding: look at the digit after the place you are keeping. 5 or more rounds up, less rounds down. 297 becomes 300, 341 becomes 300.",
            "Round to something you can work with in your head, then do the easy sum. 300 × 4 = 1200 takes a moment; 297 × 4 does not.",
            "Rounding one number up makes the estimate a little high, rounding down a little low. Doing one of each keeps it honest.",
        )
        AppLanguage.ROMANIAN -> listOf(
            "Rotunjirea: te uiți la cifra de după poziția pe care o păstrezi. 5 sau mai mult urcă, mai puțin coboară. 297 devine 300, 341 devine 300.",
            "Rotunjește la ceva cu care poți lucra în minte, apoi fă calculul ușor. 300 × 4 = 1200 iese pe loc; 297 × 4 nu.",
            "Dacă rotunjești un număr în sus, estimarea iese puțin mai mare, iar în jos puțin mai mică. Câte una din fiecare o ține dreaptă.",
        )
    }
}
