package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Which side is bigger? Two expressions with a "?" between them; she
 * taps <, = or > instead of typing. [Problem.answer] is the tapped
 * index (0 = "<", 1 = "=", 2 = ">"), [Problem.revealText] the symbol.
 *
 * The two sides are always close in value (equal about a third of the
 * time), so eyeballing isn't enough. Both sides must be worked out.
 * The text is pure numbers, identical in both languages.
 */
class ComparisonProblemGenerator(private val random: Random) {

    fun generate(level: Level, language: AppLanguage): Problem {
        val (leftText, leftValue) = freeExpression(level)
        val delta = if (random.nextInt(100) < EQUAL_CHANCE_PERCENT) {
            0
        } else {
            val size = random.nextInt(1, deltaMax(level) + 1)
            if (random.nextBoolean()) size else -size
        }
        val rightValue = leftValue + delta
        var rightText = valueExpression(rightValue, level)
        // Two identical-looking sides would answer themselves.
        while (rightText == leftText) {
            rightText = valueExpression(rightValue, level)
        }

        val answer = when {
            leftValue < rightValue -> 0
            leftValue == rightValue -> 1
            else -> 2
        }
        // No hints: a tapped answer is a third of a reveal already, and
        // the exercise is meant to be eyeball-and-check quick.
        //
        // A solution still earns its place: the reveal shows only the
        // symbol, so without it she never finds out what the two sides
        // were actually worth.
        val ro = language == AppLanguage.ROMANIAN
        val verdict = when (answer) {
            0 -> if (ro) "$leftValue e mai mic decât $rightValue, deci <"
            else "$leftValue is smaller than $rightValue, so <"
            1 -> if (ro) "$leftValue și $rightValue sunt egale, deci ="
            else "$leftValue and $rightValue are the same, so ="
            else -> if (ro) "$leftValue e mai mare decât $rightValue, deci >"
            else "$leftValue is bigger than $rightValue, so >"
        }
        return Problem(
            text = "$leftText\n?\n$rightText",
            answer = answer,
            level = level,
            kind = ProblemKind.COMPARE,
            revealText = SYMBOLS[answer],
            solution = listOf(
                side(if (ro) "Stânga:" else "Left side:", leftText, leftValue),
                side(if (ro) "Dreapta:" else "Right side:", rightText, rightValue),
                verdict,
            ),
        )
    }

    /** "Left side: 24 + 7 = 31", or just "Left side: 31" when it is bare. */
    private fun side(label: String, text: String, value: Int): String =
        if (text == "$value") "$label $value" else "$label $text = $value"

    /** A left side built freely: sum, difference, product, and its value. */
    private fun freeExpression(level: Level): Pair<String, Int> {
        if (random.nextInt(3) == 0) {
            val a = random.nextInt(level.span(3..9, 6..15, 12..30))
            val b = random.nextInt(level.span(3..9, 4..12, 11..25))
            return "$a × $b" to a * b
        }
        val value = randomValue(level)
        return valueExpression(value, level) to value
    }

    /** A sum, difference, or plain number that lands exactly on [value]. */
    private fun valueExpression(value: Int, level: Level): String =
        when (random.nextInt(5)) {
            0 -> "$value"

            1, 2 -> {
                val a = random.nextInt(1, value)
                "$a + ${value - a}"
            }

            else -> {
                val b = random.nextInt(level.span(2..20, 5..60, 20..200))
                "${value + b} − $b"
            }
        }

    /** Values big enough that any minus-delta neighbour stays positive. */
    private fun randomValue(level: Level): Int =
        random.nextInt(level.span(12..60, 40..300, 200..900))

    /** How close the two sides sit: closer is harder to eyeball. */
    private fun deltaMax(level: Level): Int = level.between(4, 9, 15)

    companion object {
        val SYMBOLS = listOf("<", "=", ">")
        private const val EQUAL_CHANCE_PERCENT = 32
    }
}
