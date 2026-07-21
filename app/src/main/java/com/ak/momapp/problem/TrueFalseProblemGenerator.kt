package com.ak.momapp.problem

import com.ak.momapp.i18n.AppLanguage
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * A single claim like "7 × 8 = 54"; she taps ✓ or ✗. [Problem.answer]
 * is the tapped index (0 = "✓", 1 = "✗"). A false claim is always close
 * to the truth, so eyeballing isn't enough. The text is pure numbers,
 * identical in both languages. Rides the COMPARE topic switch.
 */
class TrueFalseProblemGenerator(private val random: Random) {

    fun generate(level: Level, language: AppLanguage): Problem {
        val statement = statement(level)
        val (a, op, b, value) = statement
        val isTrue = random.nextInt(100) < TRUE_CHANCE_PERCENT
        val claim = if (isTrue) value else falseClaim(statement, level)

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
            level = level,
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
    private fun falseClaim(statement: Statement, level: Level): Int {
        val value = statement.value
        val nearMiss = {
            val size = random.nextInt(1, deltaMax(level) + 1)
            if (random.nextBoolean() && value - size >= 0) value - size else value + size
        }
        // Division answers are small quotients: knocking ten off one
        // isn't a slip anyone makes, it's just a different number. Those
        // stay near misses, and a quotient can't be checked by its last
        // digit without doing the division anyway.
        if (statement.op == "÷") return nearMiss()

        if (random.nextBoolean()) return nearMiss()

        // A dropped hundred only makes sense once the numbers are long
        // enough to have one, which happens gradually up the scale.
        val bigCarry = random.nextDouble() < level.ramp(Level.MEDIUM_TOP, Level.HARD_ANCHOR)
        val slip = when (statement.op) {
            // Carrying is what gets dropped, so the claim comes out ten
            // (or a hundred, once the numbers are long enough) short.
            "+" -> if (bigCarry) 100 else 10
            // A forgotten borrow leaves the answer too big by the same.
            "−" -> -(if (bigCarry) 100 else 10)
            // Two ways to get a product wrong. One group too few (7 × 8
            // offered as 48, which is 6 × 8), or, once a multiplicand
            // has two digits, a dropped ten from the carry. The second
            // only makes sense when there's a ten to drop.
            else -> when {
                statement.a >= 10 || statement.b >= 10 -> if (random.nextBoolean()) 10 else statement.a
                random.nextBoolean() -> statement.a
                else -> statement.b
            }
        }
        val claimed = value - slip
        // Never below zero, and never accidentally correct.
        return if (claimed >= 0 && claimed != value) claimed else nearMiss()
    }

    private fun statement(level: Level): Statement {
        // Division claims arrive after the other three: a whole-number
        // quotient is the one of the four that has to be reasoned about
        // rather than counted out.
        val division = random.nextDouble() < level.ramp(Level.EASY_TOP - 8, Level.MEDIUM_ANCHOR)
        return when (if (division) random.nextInt(4) else random.nextInt(3)) {
            0 -> {
                val span = level.span(7..59, 35..299, 150..899)
                val a = random.nextInt(span)
                val b = random.nextInt(span)
                Statement(a, "+", b, a + b)
            }

            1 -> {
                val a = random.nextInt(level.span(20..89, 80..499, 300..999))
                val b = random.nextInt(a / 3, a)
                Statement(a, "−", b, a - b)
            }

            2 -> {
                val a = random.nextInt(level.span(3..9, 6..15, 12..25))
                val b = random.nextInt(level.span(3..9, 4..12, 11..19))
                Statement(a, "×", b, a * b)
            }

            else -> {
                val quotient = random.nextInt(level.span(4..9, 4..15, 8..25))
                val divisor = random.nextInt(level.span(2..9, 3..9, 6..19))
                Statement(quotient * divisor, "÷", divisor, quotient)
            }
        }
    }

    /** How close a false claim sits to the truth. Closer is harder. */
    private fun deltaMax(level: Level): Int = level.between(4, 9, 15)

    companion object {
        val CHOICES = listOf("✓", "✗")
        private const val TRUE_CHANCE_PERCENT = 50
    }
}
