package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * "12 ? 3 = 4": which operation sign makes the line true? She taps one
 * of + − × ÷; [Problem.answer] is the tapped index into [SYMBOLS],
 * [Problem.revealText] the sign itself. Generation retries until
 * exactly one sign fits (2 + 2 and 2 × 2 both land on 4, so ambiguous
 * draws are thrown away). The text is pure numbers, identical in both
 * languages. Rides the CORE topic switch.
 */
class MissingOperatorGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        while (true) {
            val index = random.nextInt(SYMBOLS.size)
            val (a, b) = operands(index, difficulty)
            val result = apply(index, a, b) ?: continue
            // Every sign that lands on the same result makes it ambiguous.
            val fitting = SYMBOLS.indices.filter { apply(it, a, b) == result }
            if (fitting != listOf(index)) continue

            // No hints: four buttons, and trying them in your head IS the exercise.
            return Problem(
                text = "$a ? $b = $result",
                answer = index,
                difficulty = difficulty,
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

    private fun operands(index: Int, difficulty: Difficulty): Pair<Int, Int> =
        when (SYMBOLS[index]) {
            "+" -> when (difficulty) {
                Difficulty.EASY -> random.nextInt(4, 41) to random.nextInt(4, 41)
                Difficulty.MEDIUM -> random.nextInt(15, 200) to random.nextInt(15, 200)
                Difficulty.HARD -> random.nextInt(60, 700) to random.nextInt(60, 700)
            }

            "−" -> {
                val a = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(10, 60)
                    Difficulty.MEDIUM -> random.nextInt(40, 300)
                    Difficulty.HARD -> random.nextInt(150, 900)
                }
                a to random.nextInt(2, a)
            }

            "×" -> when (difficulty) {
                Difficulty.EASY -> random.nextInt(2, 10) to random.nextInt(2, 10)
                Difficulty.MEDIUM -> random.nextInt(4, 13) to random.nextInt(3, 13)
                Difficulty.HARD -> random.nextInt(11, 20) to random.nextInt(6, 16)
            }

            else -> {
                val (quotient, divisor) = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(2, 10) to random.nextInt(2, 10)
                    Difficulty.MEDIUM -> random.nextInt(3, 13) to random.nextInt(3, 13)
                    Difficulty.HARD -> random.nextInt(8, 26) to random.nextInt(6, 20)
                }
                quotient * divisor to divisor
            }
        }

    companion object {
        val SYMBOLS = listOf("+", "−", "×", "÷")
    }
}
