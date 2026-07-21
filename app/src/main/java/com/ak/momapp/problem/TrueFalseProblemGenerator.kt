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
        val statement = statement(difficulty)
        val (a, op, b, value) = statement
        val isTrue = random.nextInt(100) < TRUE_CHANCE_PERCENT
        val claim = if (isTrue) value else falseClaim(statement, difficulty)

        val answer = if (isTrue) 0 else 1
        // No hints: with two buttons a tapped answer is half a reveal already.
        //
        // The solution is the part that teaches: a reveal names the true
        // value only when the claim was false, so a claim she got right
        // -- or a true one -- would otherwise never show its working.
        val ro = language == AppLanguage.ROMANIAN
        val verdict = if (isTrue) {
            if (ro) "Afirmația spune tot $claim, deci e adevărată: ${CHOICES[0]}"
            else "The claim says $claim too, so it is true: ${CHOICES[0]}"
        } else {
            if (ro) "Afirmația spune $claim, nu $value, deci e falsă: ${CHOICES[1]}"
            else "The claim says $claim, not $value, so it is false: ${CHOICES[1]}"
        }
        return Problem(
            text = "$a $op $b = $claim",
            answer = answer,
            difficulty = difficulty,
            kind = ProblemKind.TRUE_FALSE,
            revealText = if (isTrue) CHOICES[0] else "${CHOICES[1]} (= $value)",
            solution = listOf(
                if (ro) "Calculează singură: $a $op $b = $value"
                else "Work it out yourself: $a $op $b = $value",
                verdict,
            ),
        )
    }

    private data class Statement(val a: Int, val op: String, val b: Int, val value: Int)

    /**
     * A wrong number to put on the right of the "=".
     *
     * Half of these are *slips*: the carry forgotten in an addition, the
     * borrow forgotten in a subtraction, a whole group missed in a
     * multiplication. They're the mistakes people actually make, and
     * being multiples of ten they leave the last digit alone.
     *
     * That last part matters more than it sounds. When every wrong claim
     * was a near miss of 1..15, the true and claimed last digits always
     * differed, so "7 × 8 = 54" could be settled by looking at one digit
     * — 8 × 7 ends in 6, this ends in 4, false — without ever working
     * out the product. Measured: the last digit alone decided 100% of
     * false claims at EASY and MEDIUM, 93% at HARD. Slips close that
     * shortcut on about half of them.
     */
    private fun falseClaim(statement: Statement, difficulty: Difficulty): Int {
        val value = statement.value
        val nearMiss = {
            val size = random.nextInt(1, deltaMax(difficulty) + 1)
            if (random.nextBoolean() && value - size >= 0) value - size else value + size
        }
        // Division answers are small quotients: knocking ten off one
        // isn't a slip anyone makes, it's just a different number. Those
        // stay near misses, and a quotient can't be checked by its last
        // digit without doing the division anyway.
        if (statement.op == "÷") return nearMiss()

        if (random.nextBoolean()) return nearMiss()

        val slip = when (statement.op) {
            // Carrying is what gets dropped, so the claim comes out ten
            // (or a hundred, once the numbers are long enough) short.
            "+" -> if (difficulty == Difficulty.HARD && random.nextBoolean()) 100 else 10
            // A forgotten borrow leaves the answer too big by the same.
            "−" -> -(if (difficulty == Difficulty.HARD && random.nextBoolean()) 100 else 10)
            // Two ways to get a product wrong. One group too few (7 × 8
            // offered as 48, which is 6 × 8), or, once a multiplicand
            // has two digits, a dropped ten from the carry. The second
            // only makes sense when there's a ten to drop.
            else -> when {
                difficulty != Difficulty.EASY && random.nextBoolean() -> 10
                random.nextBoolean() -> statement.a
                else -> statement.b
            }
        }
        val claimed = value - slip
        // Never below zero, and never accidentally correct.
        return if (claimed >= 0 && claimed != value) claimed else nearMiss()
    }

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
