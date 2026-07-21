package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * "12 ? 3 = 4": which operation sign makes the line true? She taps one
 * of + − × ÷; [Problem.answer] is the tapped index into [SYMBOLS],
 * [Problem.revealText] the sign itself. Generation retries until
 * exactly one sign fits (2 + 2 and 2 × 2 both land on 4, so ambiguous
 * draws are thrown away). The text is pure numbers, identical in both
 * languages. Rides the CORE topic switch.
 */
class MissingOperatorGenerator(private val random: Random) {

    fun generate(level: Level, language: AppLanguage): Problem {
        while (true) {
            val index = random.nextInt(SYMBOLS.size)
            val (a, b) = operands(index, level)
            val result = apply(index, a, b) ?: continue
            // Every sign that lands on the same result makes it ambiguous.
            val fitting = SYMBOLS.indices.filter { apply(it, a, b) == result }
            if (fitting != listOf(index)) continue

            // No hints: four buttons, and trying them in your head IS the exercise.
            return Problem(
                text = "$a ? $b = $result",
                answer = index,
                level = level,
                kind = ProblemKind.MISSING_OP,
                revealText = SYMBOLS[index],
            )
        }
    }

    /** The sign applied to the pair; null when it wouldn't stay a whole non-negative number. */
    private fun apply(index: Int, a: Int, b: Int): Int? = when (SYMBOLS[index]) {
        "+" -> a + b
        "−" -> if (a >= b) a - b else null
        "×" -> a * b
        else -> if (b != 0 && a % b == 0) a / b else null
    }

    private fun operands(index: Int, level: Level): Pair<Int, Int> =
        when (SYMBOLS[index]) {
            "+" -> {
                val span = level.span(4..40, 15..199, 60..699)
                random.nextInt(span) to random.nextInt(span)
            }

            "−" -> {
                val a = random.nextInt(level.span(10..59, 40..299, 150..899))
                a to random.nextInt(2, a)
            }

            "×" -> random.nextInt(level.span(2..9, 4..12, 11..19)) to
                random.nextInt(level.span(2..9, 3..12, 6..15))

            else -> {
                val quotient = random.nextInt(level.span(2..9, 3..12, 8..25))
                val divisor = random.nextInt(level.span(2..9, 3..12, 6..19))
                quotient * divisor to divisor
            }
        }

    companion object {
        val SYMBOLS = listOf("+", "−", "×", "÷")
    }
}
