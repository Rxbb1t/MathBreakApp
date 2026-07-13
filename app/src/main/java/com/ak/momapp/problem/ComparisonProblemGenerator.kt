package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

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

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        val (leftText, leftValue) = freeExpression(difficulty)
        val delta = if (random.nextInt(100) < EQUAL_CHANCE_PERCENT) {
            0
        } else {
            val size = random.nextInt(1, deltaMax(difficulty) + 1)
            if (random.nextBoolean()) size else -size
        }
        val rightValue = leftValue + delta
        var rightText = valueExpression(rightValue, difficulty)
        // Two identical-looking sides would answer themselves.
        while (rightText == leftText) {
            rightText = valueExpression(rightValue, difficulty)
        }

        val answer = when {
            leftValue < rightValue -> 0
            leftValue == rightValue -> 1
            else -> 2
        }
        // No hints: a tapped answer is a third of a reveal already, and
        // the exercise is meant to be eyeball-and-check quick.
        return Problem(
            text = "$leftText\n?\n$rightText",
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.COMPARE,
            revealText = SYMBOLS[answer],
        )
    }

    /** A left side built freely: sum, difference, product, and its value. */
    private fun freeExpression(difficulty: Difficulty): Pair<String, Int> {
        if (random.nextInt(3) == 0) {
            val (a, b) = when (difficulty) {
                Difficulty.EASY -> random.nextInt(3, 10) to random.nextInt(3, 10)
                Difficulty.MEDIUM -> random.nextInt(6, 16) to random.nextInt(4, 13)
                Difficulty.HARD -> random.nextInt(12, 31) to random.nextInt(11, 26)
            }
            return "$a × $b" to a * b
        }
        val value = randomValue(difficulty)
        return valueExpression(value, difficulty) to value
    }

    /** A sum, difference, or plain number that lands exactly on [value]. */
    private fun valueExpression(value: Int, difficulty: Difficulty): String =
        when (random.nextInt(5)) {
            0 -> "$value"

            1, 2 -> {
                val a = random.nextInt(1, value)
                "$a + ${value - a}"
            }

            else -> {
                val b = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(2, 21)
                    Difficulty.MEDIUM -> random.nextInt(5, 61)
                    Difficulty.HARD -> random.nextInt(20, 201)
                }
                "${value + b} − $b"
            }
        }

    /** Values big enough that any minus-delta neighbour stays positive. */
    private fun randomValue(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> random.nextInt(12, 61)
        Difficulty.MEDIUM -> random.nextInt(40, 301)
        Difficulty.HARD -> random.nextInt(200, 901)
    }

    private fun deltaMax(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 4
        Difficulty.MEDIUM -> 9
        Difficulty.HARD -> 15
    }

    companion object {
        val SYMBOLS = listOf("<", "=", ">")
        private const val EQUAL_CHANCE_PERCENT = 32
    }
}
