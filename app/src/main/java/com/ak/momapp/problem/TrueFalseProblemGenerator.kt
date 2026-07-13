package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random

/**
 * A single claim like "7 × 8 = 54"; she taps ✓ or ✗. [Problem.answer]
 * is the tapped index (0 = "✓", 1 = "✗"). A false claim is always close
 * to the truth, so eyeballing isn't enough. The text is pure numbers,
 * identical in both languages. Rides the COMPARE topic switch.
 */
class TrueFalseProblemGenerator(private val random: Random) {

    fun generate(difficulty: Difficulty, language: AppLanguage): Problem {
        val (a, op, b, value) = statement(difficulty)
        val isTrue = random.nextInt(100) < TRUE_CHANCE_PERCENT
        val claim = if (isTrue) {
            value
        } else {
            val size = random.nextInt(1, deltaMax(difficulty) + 1)
            // A miss below zero would give the game away; push it up instead.
            if (random.nextBoolean() && value - size >= 0) value - size else value + size
        }

        val answer = if (isTrue) 0 else 1
        // No hints: with two buttons a tapped answer is half a reveal already.
        return Problem(
            text = "$a $op $b = $claim",
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.TRUE_FALSE,
            revealText = if (isTrue) CHOICES[0] else "${CHOICES[1]} (= $value)",
        )
    }

    private data class Statement(val a: Int, val op: String, val b: Int, val value: Int)

    private fun statement(difficulty: Difficulty): Statement =
        when (random.nextInt(if (difficulty == Difficulty.EASY) 3 else 4)) {
            0 -> {
                val (a, b) = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(7, 60) to random.nextInt(7, 60)
                    Difficulty.MEDIUM -> random.nextInt(35, 300) to random.nextInt(35, 300)
                    Difficulty.HARD -> random.nextInt(150, 900) to random.nextInt(150, 900)
                }
                Statement(a, "+", b, a + b)
            }

            1 -> {
                val a = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(20, 90)
                    Difficulty.MEDIUM -> random.nextInt(80, 500)
                    Difficulty.HARD -> random.nextInt(300, 1000)
                }
                val b = random.nextInt(a / 3, a)
                Statement(a, "−", b, a - b)
            }

            2 -> {
                val (a, b) = when (difficulty) {
                    Difficulty.EASY -> random.nextInt(3, 10) to random.nextInt(3, 10)
                    Difficulty.MEDIUM -> random.nextInt(6, 16) to random.nextInt(4, 13)
                    Difficulty.HARD -> random.nextInt(12, 26) to random.nextInt(11, 20)
                }
                Statement(a, "×", b, a * b)
            }

            // Division claims skip EASY; the quotient is always whole.
            else -> {
                val (quotient, divisor) = when (difficulty) {
                    Difficulty.EASY -> 0 to 0 // unreachable
                    Difficulty.MEDIUM -> random.nextInt(4, 16) to random.nextInt(3, 10)
                    Difficulty.HARD -> random.nextInt(8, 26) to random.nextInt(6, 20)
                }
                Statement(quotient * divisor, "÷", divisor, quotient)
            }
        }

    private fun deltaMax(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 4
        Difficulty.MEDIUM -> 9
        Difficulty.HARD -> 15
    }

    companion object {
        val CHOICES = listOf("✓", "✗")
        private const val TRUE_CHANCE_PERCENT = 50
    }
}
