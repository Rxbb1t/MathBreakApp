package com.ak.momapp.problem

/**
 * The one-a-day multi-stage puzzle: a short scene-setting line and five
 * [Problem]s worked in order, each one feeding the next.
 */
data class DailyChallenge(
    val intro: String,
    val stages: List<Problem>,
)

/**
 * One day's chain, from the anchor to the finale.
 *
 * Only the DRAWN values are stored. Every answer below is derived from
 * them, which is the whole reason this is one object rather than five
 * variables passed along: a chain where step four's input is a field
 * somebody could set is a chain that can disagree with itself. Here it
 * cannot. Change [anchor] and the finale moves with it, because there is
 * nowhere else for the value to have come from.
 *
 * All of it is whole-number arithmetic. No division that isn't exact, no
 * rounding, nothing that needs a decimal point explained to anybody.
 */
data class DailyChallengeState(
    /** Step 1's answer: the number the whole day hangs off. */
    val anchor: Int,
    /** The window step 1 hunts in. Exactly one multiple of both lives here. */
    val low: Int,
    val high: Int,
    /** The two numbers the anchor is a multiple of. */
    val factorA: Int,
    val factorB: Int,
    /** Step 2 multiplies by this and takes the remainder against [divisor]. */
    val multiplier: Int,
    val divisor: Int,
    /** Step 3 adds this before the digits are summed. */
    val addend: Int,
    /** Step 5 takes this off at the end. */
    val tail: Int,
) {
    /** Step 2: what is left over. Never zero; see the divisor's draw. */
    val remainder: Int get() = anchor * multiplier % divisor

    /** Step 3's working value, always two digits. */
    val shifted: Int get() = remainder + addend

    /** Step 3: its digits added together. */
    val digitSum: Int get() = digitsOf(shifted)

    /** Step 4: the first prime past the digit sum. */
    val prime: Int get() = PRIMES.first { it > digitSum }

    /** Step 4's answer: how far that prime sits above the digit sum. */
    val gap: Int get() = prime - digitSum

    /** Step 5: back to the anchor, one subtraction from done. */
    val finale: Int get() = gap * anchor - tail

    /** The five answers in order, which is also the order they are asked. */
    val answers: List<Int> get() = listOf(anchor, remainder, digitSum, gap, finale)

    private companion object {
        /**
         * Far enough to cover any digit sum this chain can reach. The
         * largest [shifted] is 48, whose digits sum to 12, so 13 is always
         * in reach and the lookup can never run off the end.
         */
        val PRIMES = listOf(2, 3, 5, 7, 11, 13, 17, 19, 23)

        fun digitsOf(n: Int): Int {
            var left = n
            var total = 0
            while (left > 0) {
                total += left % 10
                left /= 10
            }
            return total
        }
    }
}
